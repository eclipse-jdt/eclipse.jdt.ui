/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.testplugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Vector;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;


public class JavaTestProject {
	
	private IJavaProject fJavaProject;			
	
	public JavaTestProject(IWorkspaceRoot root, String projectName, String binFolderName) throws CoreException {
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
		if (binFolderName != null) {
			IFolder binFolder= project.getFolder(binFolderName);
			if (!binFolder.exists()) {
				binFolder.create(false, true, null);
			}
			outputLocation= binFolder.getFullPath();
		} else {
			outputLocation= project.getFullPath();
		}
		
		if (!project.hasNature(JavaCore.NATURE_ID)) {
			addNatureToProject(project, JavaCore.NATURE_ID, null);
		}
		
		fJavaProject= JavaCore.create(project);
		
		fJavaProject.setOutputLocation(outputLocation, null);
		fJavaProject.setRawClasspath(new IClasspathEntry[0], null);
	}
	
	public IJavaProject getJavaProject() {
		return fJavaProject;
	}
	
	/**
	 * Removes the TestJavaProject
	 */		
	public void remove() throws CoreException {
		fJavaProject.getProject().delete(true, true, null);
		fJavaProject= null;
	}
	
	public void removeSourceContainer(String containerName) throws CoreException {
		IFolder folder= fJavaProject.getProject().getFolder(containerName);
		removeFromClasspath(folder.getFullPath());
		fJavaProject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);
		
		folder.delete(true, null);
	}
	
	public IPackageFragmentRoot addSourceContainer(String containerName) throws CoreException {
		IProject project= fJavaProject.getProject();
		IContainer container= null;
		if (containerName == null || containerName.length() == 0) {
			container= project;
		} else {
			IFolder folder= fJavaProject.getProject().getFolder(containerName);
			if (!folder.exists()) {
				folder.create(false, true, null);
			}
			container= folder;
		}
		IPackageFragmentRoot root= fJavaProject.getPackageFragmentRoot(container);
		
		IClasspathEntry cpe= JavaCore.newSourceEntry(root.getPath());
		addToClasspath(cpe);		
		return root;
	}
	
	public IPackageFragmentRoot addSourceContainerWithImport(String containerName, IPath importPath) throws IOException, CoreException {
		IPackageFragmentRoot root= addSourceContainer(containerName);
		IResource res= root.getUnderlyingResource();		
		importFiles(importPath, (IContainer)res, new String[] { "java" });
		return root;
	}	
	
	public IPackageFragmentRoot addLibrary(IPath path) throws JavaModelException {
		return addLibrary(path, null, null);
	}
	
	public IPackageFragmentRoot addLibrary(IPath path, IPath sourceAttachPath, IPath sourceAttachRoot) throws JavaModelException {
		IClasspathEntry cpe= JavaCore.newLibraryEntry(path, sourceAttachPath, sourceAttachRoot);
		addToClasspath(cpe);
		return fJavaProject.getPackageFragmentRoot(path.toString());
	}
	
	public IPackageFragmentRoot addLibraryWithImport(IPath jarPath, IPath sourceAttachPath, IPath sourceAttachRoot) throws IOException, CoreException {
		IProject project= fJavaProject.getProject();
		importFiles(jarPath, project, null);
		IFile file= project.getFile(jarPath.lastSegment());
		return addLibrary(file.getFullPath(), sourceAttachPath, sourceAttachRoot);
	}	
	
	public IPackageFragmentRoot addRTJar() throws CoreException {
		IPath[] rtJarPath= findRtJar();
		if (rtJarPath != null) {
			return addLibrary(rtJarPath[0], rtJarPath[1], rtJarPath[2]);
		}
		return null;
	}
	
	public void addRequiredProject(IJavaProject jproject) throws JavaModelException {
		IClasspathEntry cpe= JavaCore.newProjectEntry(jproject.getProject().getFullPath());
		addToClasspath(cpe);
	}

	private void addToClasspath(IClasspathEntry cpe) throws JavaModelException {
		IClasspathEntry[] oldEntries= fJavaProject.getRawClasspath();
		for (int i= 0; i < oldEntries.length; i++) {
			if (oldEntries[i].equals(cpe)) {
				return;
			}
		}
		int nEntries= oldEntries.length;
		IClasspathEntry[] newEntries= new IClasspathEntry[nEntries + 1];
		System.arraycopy(oldEntries, 0, newEntries, 0, nEntries);
		newEntries[nEntries]= cpe;
		fJavaProject.setRawClasspath(newEntries, null);
	}
	
	private void removeFromClasspath(IPath path) throws JavaModelException {
		IClasspathEntry[] oldEntries= fJavaProject.getRawClasspath();
		int nEntries= oldEntries.length;
		Vector vec= new Vector(nEntries);
		for (int i= 0 ; i < nEntries ; i++) {
			IClasspathEntry curr= oldEntries[i];
			if (!path.equals(curr.getPath())) {
				vec.addElement(curr);			}
		}
		
		IClasspathEntry[] newEntries= new IClasspathEntry[vec.size()];
		vec.copyInto(newEntries);
		fJavaProject.setRawClasspath(newEntries, null);
	}	
		
	private void importFiles(IPath sourcePath, IContainer destContainer, String[] suffixes) throws CoreException, JavaModelException, IOException {
		File file= sourcePath.toFile();
		if (file.isDirectory()) {
			File[] list= file.listFiles();
			if (list != null) {
				for (int i= 0; i < list.length; i++) {
					doImport(list[i], destContainer, suffixes);
				}
			}
		} else {
			doImport(file, destContainer, suffixes);
		}		
		destContainer.refreshLocal(IResource.DEPTH_INFINITE, null);
	}
	
	private void doImport(File file, IContainer parent, String[] suffixes) throws CoreException, IOException {
		if (file.exists()) {
			IPath filePath= new Path(file.getName());
			if (file.isFile()) {
				if (hasSuffix(filePath, suffixes)) {
					try {
						parent.getFile(filePath).create(new FileInputStream(file), false, null);
					} catch (CoreException e) {
						System.out.println("unable to create " + file.getAbsolutePath());
						throw e;
					}
				}
			} else {
				try {
					IFolder folder= parent.getFolder(filePath);
					if (!folder.exists()) {
						folder.create(false, true, null);
					}
					String[] list= file.list();
					for (int i= 0; i < list.length; i++) {
						String path= file.getAbsolutePath() + file.separatorChar + list[i];
						doImport(new java.io.File(path), folder, suffixes);
					}						
				} catch (CoreException e) {
					System.out.println("unable to create " + file.getAbsolutePath());
					throw e;
				}
			}
		}
	}
	
	private boolean hasSuffix(IPath path, String[] suffixes) {
		if (suffixes == null) {
			return true;
		}
		for (int i= 0; i < suffixes.length; i++) {
			if (path.getFileExtension().equals((suffixes[i]))) {
				return true;
			}
		}
		return false;
	}
	
	public static void addNatureToProject(IProject proj, String natureId, IProgressMonitor monitor) throws CoreException {
		IProjectDescription description = proj.getDescription();
		String[] prevNatures= description.getNatureIds();
		String[] newNatures= new String[prevNatures.length + 1];
		System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
		newNatures[prevNatures.length]= natureId;
		description.setNatureIds(newNatures);
		proj.setDescription(description, monitor);
	}	
	
	/**
	 * Try to find rt.jar
	 */
	private static IPath[] findRtJar() {
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
		}
		return null;
	}

}