package org.eclipse.jdt.testplugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;

/**
 * Helper methods to set up a IJavaProject.
 */
public class JavaProjectHelper {
	
	private static final IPath JCL_MAX= new Path("test-resources/jclmax.jar");
	

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
				binFolder.create(false, true, null);
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
	public static void delete(IJavaProject jproject) throws CoreException {
		jproject.getProject().delete(true, true, null);
	}


	/**
	 * Adds a source container to a IJavaProject.
	 */		
	public static IPackageFragmentRoot addSourceContainer(IJavaProject jproject, String containerName) throws CoreException {
		IProject project= jproject.getProject();
		IContainer container= null;
		if (containerName == null || containerName.length() == 0) {
			container= project;
		} else {
			IFolder folder= project.getFolder(containerName);
			if (!folder.exists()) {
				folder.create(false, true, null);
			}
			container= folder;
		}
		IPackageFragmentRoot root= jproject.getPackageFragmentRoot(container);
		
		IClasspathEntry cpe= JavaCore.newSourceEntry(root.getPath());
		addToClasspath(jproject, cpe);		
		return root;
	}

	/**
	 * Adds a source container to a IJavaProject and imports all files contained
	 * in the given Zip file.
	 */	
	public static IPackageFragmentRoot addSourceContainerWithImport(IJavaProject jproject, String containerName, ZipFile zipFile) throws InvocationTargetException, CoreException {
		IPackageFragmentRoot root= addSourceContainer(jproject, containerName);
		importFilesFromZip(zipFile, root.getPath(), null);
		return root;
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
		newFile.create(new FileInputStream(jarPath.toFile()), false, null);
		return addLibrary(jproject, newFile.getFullPath(), sourceAttachPath, sourceAttachRoot);
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

	private static void addToClasspath(IJavaProject jproject, IClasspathEntry cpe) throws JavaModelException {
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
		File jclMaxArchive= JavaTestPlugin.getDefault().getFileInPlugin(JCL_MAX);
		if (jclMaxArchive != null) {
			return new IPath[] {
				new Path(jclMaxArchive.getPath()),
				null,
				null
			};
		}
		
		/**IVMInstall vmInstall= JavaRuntime.getDefaultVMInstall();
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
	
	
}

