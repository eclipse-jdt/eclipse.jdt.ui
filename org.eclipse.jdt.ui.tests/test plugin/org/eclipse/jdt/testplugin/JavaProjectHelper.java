/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.testplugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.ITypeNameRequestor;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.CoreUtility;

/**
 * Helper methods to set up a IJavaProject.
 */
public class JavaProjectHelper {
	
	public static final IPath RT_STUBS= new Path("testresources/rtstubs.jar");
	public static final IPath JUNIT_SRC= new Path("testresources/junit37-noUI-src.zip");
	public static final IPath JUNIT_SRC_381= new Path("testresources/junit381-noUI-src.zip");
	
	public static final IPath MYLIB= new Path("testresources/mylib.jar");
	
	private static final int MAX_RETRY= 5;

	/**
	 * Creates a IJavaProject.
	 */	
	public static IJavaProject createJavaProject(String projectName, String binFolderName) throws CoreException {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		IProject project= root.getProject(projectName);
		if (!project.exists()) {
			project.create(null);
		} else {
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		}
		
		if (!project.isOpen()) {
			project.open(null);
		}
		
		IPath outputLocation;
		if (binFolderName != null && binFolderName.length() > 0) {
			IFolder binFolder= project.getFolder(binFolderName);
			if (!binFolder.exists()) {
				CoreUtility.createFolder(binFolder, false, true, null);
			}
			outputLocation= binFolder.getFullPath();
		} else {
			outputLocation= project.getFullPath();
		}
		
		if (!project.hasNature(JavaCore.NATURE_ID)) {
			addNatureToProject(project, JavaCore.NATURE_ID, null);
		}
		
		IJavaProject jproject= JavaCore.create(project);
		
		jproject.setOutputLocation(outputLocation, null);
		jproject.setRawClasspath(new IClasspathEntry[0], null);
		
		return jproject;	
	}
	
	/**
	 * Removes a IJavaProject.
	 */		
	public static void delete(final IJavaProject jproject) throws CoreException {
		IWorkspaceRunnable runnable= new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				performDummySearch();
				jproject.setRawClasspath(new IClasspathEntry[0], jproject.getProject().getFullPath(), null);
				for (int i= 0; i < MAX_RETRY; i++) {
					try {
						jproject.getProject().delete(true, true, null);
						i= MAX_RETRY;
					} catch (CoreException e) {
						if (i == MAX_RETRY - 1) {
							JavaPlugin.log(e);
							throw e;
						}
						try {
							Thread.sleep(1000); // sleep a second
						} catch (InterruptedException e1) {
						} 
					}
				}
			}
		};
		ResourcesPlugin.getWorkspace().run(runnable, null);	
		
	}

	/**
	 * Removes all files in the project and sets the given classpath
	 */			
	public static void clear(final IJavaProject jproject, final IClasspathEntry[] entries) throws CoreException {
		performDummySearch();
		
		IWorkspaceRunnable runnable= new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				jproject.setRawClasspath(entries, null);
		
				IResource[] resources= jproject.getProject().members();
				for (int i= 0; i < resources.length; i++) {
					if (!resources[i].getName().startsWith(".")) {
						resources[i].delete(true, null);
					}
				}
			}
		};
		ResourcesPlugin.getWorkspace().run(runnable, null);
	}
	

	public static void performDummySearch() throws JavaModelException {
		new SearchEngine().searchAllTypeNames(
		 	ResourcesPlugin.getWorkspace(),
			null,
			null,
			SearchPattern.R_EXACT_MATCH,
			true,
			IJavaSearchConstants.CLASS,
			SearchEngine.createJavaSearchScope(new IJavaElement[0]),
			new Requestor(),
			IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
			null);
	}


	/**
	 * Adds a source container to a IJavaProject.
	 */		
	public static IPackageFragmentRoot addSourceContainer(IJavaProject jproject, String containerName) throws CoreException {
		return addSourceContainer(jproject, containerName, new Path[0]);
	}

	/**
	 * Adds a source container to a IJavaProject.
	 */		
	public static IPackageFragmentRoot addSourceContainer(IJavaProject jproject, String containerName, IPath[] exclusionFilters) throws CoreException {
		IProject project= jproject.getProject();
		IContainer container= null;
		if (containerName == null || containerName.length() == 0) {
			container= project;
		} else {
			IFolder folder= project.getFolder(containerName);
			if (!folder.exists()) {
				CoreUtility.createFolder(folder, false, true, null);
			}
			container= folder;
		}
		IPackageFragmentRoot root= jproject.getPackageFragmentRoot(container);
		
		IClasspathEntry cpe= JavaCore.newSourceEntry(root.getPath(), exclusionFilters);
		addToClasspath(jproject, cpe);		
		return root;
	}

	/**
	 * @deprecated use addSourceContainerWithImport(IJavaProject jproject, String containerName, File zipFile) to make sure that the zip file is correctly closed
	 */	
	public static IPackageFragmentRoot addSourceContainerWithImport(IJavaProject jproject, String containerName, ZipFile zipFile) throws InvocationTargetException, CoreException {
		return addSourceContainerWithImport(jproject, containerName, zipFile, new Path[0]);
	}
	
	/**
	 * Adds a source container to a IJavaProject and imports all files contained
	 * in the given Zip file.
	 */		
	public static IPackageFragmentRoot addSourceContainerWithImport(IJavaProject jproject, String containerName, File zipFile) throws InvocationTargetException, CoreException, IOException {
		return addSourceContainerWithImport(jproject, containerName, zipFile, new Path[0]);
	}

	/**
	 * @deprecated use addSourceContainerWithImport(IJavaProject jproject, String containerName, File zipFile, IPath[]) to make sure that the zip file is correctly closed
	 */
	public static IPackageFragmentRoot addSourceContainerWithImport(IJavaProject jproject, String containerName, ZipFile zipFile, IPath[] exclusionFilters) throws InvocationTargetException, CoreException {
		IPackageFragmentRoot root= addSourceContainer(jproject, containerName, exclusionFilters);
		importFilesFromZip(zipFile, root.getPath(), null);
		return root;
	}

	/**
	 * Adds a source container to a IJavaProject and imports all files contained
	 * in the given Zip file.
	 */		
	public static IPackageFragmentRoot addSourceContainerWithImport(IJavaProject jproject, String containerName, File zipFile, IPath[] exclusionFilters) throws InvocationTargetException, CoreException, IOException {
		ZipFile file= new ZipFile(zipFile);
		try {
			IPackageFragmentRoot root= addSourceContainer(jproject, containerName, exclusionFilters);
			importFilesFromZip(file, root.getPath(), null);
			return root;
		} finally {
			if (file != null) {
				file.close();
			}
		}
	}	

	/**
	 * Removes a source folder from a IJavaProject.
	 */		
	public static void removeSourceContainer(IJavaProject jproject, String containerName) throws CoreException {
		IFolder folder= jproject.getProject().getFolder(containerName);
		removeFromClasspath(jproject, folder.getFullPath());
		folder.delete(true, null);
	}

	/**
	 * Adds a library entry to a IJavaProject.
	 */	
	public static IPackageFragmentRoot addLibrary(IJavaProject jproject, IPath path) throws JavaModelException {
		return addLibrary(jproject, path, null, null);
	}

	/**
	 * Adds a library entry with source attchment to a IJavaProject.
	 */			
	public static IPackageFragmentRoot addLibrary(IJavaProject jproject, IPath path, IPath sourceAttachPath, IPath sourceAttachRoot) throws JavaModelException {
		IClasspathEntry cpe= JavaCore.newLibraryEntry(path, sourceAttachPath, sourceAttachRoot);
		addToClasspath(jproject, cpe);
		return jproject.getPackageFragmentRoot(path.toString());
	}


	/**
	 * Copies the library into the project and adds it as library entry.
	 */			
	public static IPackageFragmentRoot addLibraryWithImport(IJavaProject jproject, IPath jarPath, IPath sourceAttachPath, IPath sourceAttachRoot) throws IOException, CoreException {
		IProject project= jproject.getProject();
		IFile newFile= project.getFile(jarPath.lastSegment());
		InputStream inputStream= null;
		try {
			inputStream= new FileInputStream(jarPath.toFile()); 
			newFile.create(inputStream, true, null);
		} finally {
			if (inputStream != null) {
				try { inputStream.close(); } catch (IOException e) { }
			}
		}				
		return addLibrary(jproject, newFile.getFullPath(), sourceAttachPath, sourceAttachRoot);
	}	

	/**
	 * Creates and adds a class folder to the class path.
	 */			
	public static IPackageFragmentRoot addClassFolder(IJavaProject jproject, String containerName, IPath sourceAttachPath, IPath sourceAttachRoot) throws CoreException {
		IProject project= jproject.getProject();
		IContainer container= null;
		if (containerName == null || containerName.length() == 0) {
			container= project;
		} else {
			IFolder folder= project.getFolder(containerName);
			if (!folder.exists()) {
				CoreUtility.createFolder(folder, false, true, null);
			}
			container= folder;
		}
		return addLibrary(jproject, container.getFullPath(), sourceAttachPath, sourceAttachRoot);
	}

	/**
	 * @deprecated Use addClassFolderWithImport(IJavaProject, String, IPath, IPath, File) to make sure that the zip file is correctly closed
	 */			
	public static IPackageFragmentRoot addClassFolderWithImport(IJavaProject jproject, String containerName, IPath sourceAttachPath, IPath sourceAttachRoot, ZipFile zipFile) throws CoreException, InvocationTargetException {
		IPackageFragmentRoot root= addClassFolder(jproject, containerName, sourceAttachPath, sourceAttachRoot);
		importFilesFromZip(zipFile, root.getPath(), null);
		return root;
	}
	
	/**
	 * Creates and adds a class folder to the class path and imports all files
	 * contained in the given Zip file.
	 */			
	public static IPackageFragmentRoot addClassFolderWithImport(IJavaProject jproject, String containerName, IPath sourceAttachPath, IPath sourceAttachRoot, File zipFile) throws IOException, CoreException, InvocationTargetException {
		ZipFile file= new ZipFile(zipFile);
		try {
			IPackageFragmentRoot root= addClassFolder(jproject, containerName, sourceAttachPath, sourceAttachRoot);
			importFilesFromZip(file, root.getPath(), null);
			return root;
		} finally {
			if (file != null) {
				file.close();
			}
		}
	}	

	/**
	 * Adds a library entry pointing to a JRE.
	 * Can return null, if no JRE installation was found.
	 */	
	public static IPackageFragmentRoot addRTJar(IJavaProject jproject) throws CoreException {

		IPath[] rtJarPath= findRtJar();
		if (rtJarPath != null) {
			return addLibrary(jproject, rtJarPath[0], rtJarPath[1], rtJarPath[2]);
		}
		return null;
	}
	
	/**
	 * Adds a variable entry with source attchment to a IJavaProject.
	 * Can return null if variable can not be resolved.
	 */			
	public static IPackageFragmentRoot addVariableEntry(IJavaProject jproject, IPath path, IPath sourceAttachPath, IPath sourceAttachRoot) throws JavaModelException {
		IClasspathEntry cpe= JavaCore.newVariableEntry(path, sourceAttachPath, sourceAttachRoot);
		addToClasspath(jproject, cpe);
		IPath resolvedPath= JavaCore.getResolvedVariablePath(path);
		if (resolvedPath != null) {
			return jproject.getPackageFragmentRoot(resolvedPath.toString());
		}
		return null;
	}
	
	/**
	 * Adds a variable entry pointing to a JRE.
	 * The arguments specify the names of the variable to be used.
	 * @param libVarName Name of the variable for the library
	 * @param srcVarName Name of the variable for the source attchment. Can be <code>null</code>.
	 * @param srcrootVarName Name of the variable for the source attchment root. Can be <code>null</code>.
	 * @return Returns <code>null</code>, if no JRE installation was found.
	 */	
	public static IPackageFragmentRoot addVariableRTJar(IJavaProject jproject, String libVarName, String srcVarName, String srcrootVarName) throws CoreException {
		IPath[] rtJarPaths= findRtJar();
		if (rtJarPaths != null) {
			IPath libVarPath= new Path(libVarName);
			IPath srcVarPath= null;
			IPath srcrootVarPath= null;
			JavaCore.setClasspathVariable(libVarName, rtJarPaths[0], null);
			if (srcVarName != null) {
				IPath varValue= rtJarPaths[1] != null ? rtJarPaths[1] : Path.EMPTY;
				JavaCore.setClasspathVariable(srcVarName, varValue, null);
				srcVarPath= new Path(srcVarName);
			}
			if (srcrootVarName != null) {
				IPath varValue= rtJarPaths[2] != null ? rtJarPaths[2] : Path.EMPTY;
				JavaCore.setClasspathVariable(srcrootVarName, varValue, null);
				srcrootVarPath= new Path(srcrootVarName);
			}
			return addVariableEntry(jproject, libVarPath, srcVarPath, srcrootVarPath);
		}
		return null;
	}	

	/**
	 * Adds a required project entry.
	 */		
	public static void addRequiredProject(IJavaProject jproject, IJavaProject required) throws JavaModelException {
		IClasspathEntry cpe= JavaCore.newProjectEntry(required.getProject().getFullPath());
		addToClasspath(jproject, cpe);
	}	
	
	public static void removeFromClasspath(IJavaProject jproject, IPath path) throws JavaModelException {
		IClasspathEntry[] oldEntries= jproject.getRawClasspath();
		int nEntries= oldEntries.length;
		ArrayList list= new ArrayList(nEntries);
		for (int i= 0 ; i < nEntries ; i++) {
			IClasspathEntry curr= oldEntries[i];
			if (!path.equals(curr.getPath())) {
				list.add(curr);			
			}
		}
		IClasspathEntry[] newEntries= (IClasspathEntry[])list.toArray(new IClasspathEntry[list.size()]);
		jproject.setRawClasspath(newEntries, null);
	}	

	/**
	 * Sets autobuilding state for the test workspace.
	 */
	public static boolean setAutoBuilding(boolean state) throws CoreException {
		// disable auto build
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		IWorkspaceDescription desc= workspace.getDescription();
		boolean result= desc.isAutoBuilding();
		desc.setAutoBuilding(state);
		workspace.setDescription(desc);
		return result;
	}

	public static void addToClasspath(IJavaProject jproject, IClasspathEntry cpe) throws JavaModelException {
		IClasspathEntry[] oldEntries= jproject.getRawClasspath();
		for (int i= 0; i < oldEntries.length; i++) {
			if (oldEntries[i].equals(cpe)) {
				return;
			}
		}
		int nEntries= oldEntries.length;
		IClasspathEntry[] newEntries= new IClasspathEntry[nEntries + 1];
		System.arraycopy(oldEntries, 0, newEntries, 0, nEntries);
		newEntries[nEntries]= cpe;
		jproject.setRawClasspath(newEntries, null);
	}
	
	/**
	 * Try to find rt.jar
	 */
	public static IPath[] findRtJar() {
		File rtStubs= JavaTestPlugin.getDefault().getFileInPlugin(RT_STUBS);
		if (rtStubs != null && rtStubs.exists()) {
			return new IPath[] {
				new Path(rtStubs.getPath()),
				null,
				null
			};
		}
		
		/*
		IVMInstall vmInstall= JavaRuntime.getDefaultVMInstall();
		if (vmInstall != null) {
			LibraryLocation loc= vmInstall.getVMInstallType().getDefaultLibraryLocation(vmInstall.getInstallLocation());
			if (loc != null) {
				return new IPath[] {
           			new Path(loc.getSystemLibrary().getPath()),
            		new Path(loc.getSystemLibrarySource().getPath()),
            		loc.getPackageRootPath()
				};
			}
		}*/
		return null;
	}
		
	private static void addNatureToProject(IProject proj, String natureId, IProgressMonitor monitor) throws CoreException {
		IProjectDescription description = proj.getDescription();
		String[] prevNatures= description.getNatureIds();
		String[] newNatures= new String[prevNatures.length + 1];
		System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
		newNatures[prevNatures.length]= natureId;
		description.setNatureIds(newNatures);
		proj.setDescription(description, monitor);
	}
	
	private static void importFilesFromZip(ZipFile srcZipFile, IPath destPath, IProgressMonitor monitor) throws InvocationTargetException {		
		ZipFileStructureProvider structureProvider=	new ZipFileStructureProvider(srcZipFile);
		try {
			ImportOperation op= new ImportOperation(destPath, structureProvider.getRoot(), structureProvider, new ImportOverwriteQuery());
			op.run(monitor);
		} catch (InterruptedException e) {
			// should not happen
		}
	}
	
	private static class ImportOverwriteQuery implements IOverwriteQuery {
		public String queryOverwrite(String file) {
			return ALL;
		}	
	}		
	
	private static class Requestor implements ITypeNameRequestor{
		
		public void acceptClass(char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
		}

		public void acceptInterface(char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
		}
	}
	
}

