package org.eclipse.jdt.internal.ui.jarpackager;/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.ui.IEditorPart;import org.eclipse.ui.IFileEditorInput;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;import org.eclipse.jdt.internal.ui.util.JavaModelUtility;

/**
 * Operation for exporting a resource and its children to a new  JAR file.
 */
public class JarFileExportOperation implements IRunnableWithProgress {

	private JarWriter fJarWriter;
	private JarPackage fJarPackage;
	private Shell fParentShell;
	private List fErrors= new ArrayList(1); //IStatus
	private Map fJavaNameToClassFilesMap;
	private IFolder fClassFilesMapFolder;

	/**
	 * Creates an instance of this class.
	 *
	 * @param	jarPackage	the JAR package specification
	 * @param	parent	the parent for the dialog,
	 * 			or <code>null</code> if no dialog should be presented
	 */
	public JarFileExportOperation(JarPackage jarPackage, Shell parent) {
		fJarPackage= jarPackage;
		fParentShell= parent;
	}
	/**
	 * Adds a new warning to the list with the passed information.	 * Normally the export operation continues after a warning.
	 * @param	message		the message
	 * @param	exception	the throwable that caused the warning, or <code>null</code>
	 */
	protected void addWarning(String message, Throwable error) {
		fErrors.add(new Status(IStatus.WARNING, JavaPlugin.getPluginId(), 0, message, error));
	}	/**	 * Adds a new error to the list with the passed information.	 * Normally an error terminates the export operation.	 * @param	message		the message	 * @param	exception	the throwable that caused the error, or <code>null</code>	 */	protected void addError(String message, Throwable error) {		fErrors.add(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, message, error));	}	/**
	 * Answers the total number of file resources that exist at or below
	 * the given resources.
	 *
	 * @param	resource		the resource for which to count the children
	 * @return	int				the number of resources in the hierarchy
	 * @throws	CoreException	if the members of this resource can't be accessed
	 */
	protected int countWithChildren(IResource resource) throws CoreException {
		if (resource.getType() == IResource.FILE)
			return 1;

		int count= 0;
		if (resource.isAccessible()) {
			IResource[] children= ((IContainer) resource).members();
			for (int i= 0; i < children.length; i++)
				count += countWithChildren(children[i]);
		}

		return count;
	}
	/**
	 * Answers the number of file resources specified by the JAR package.
	 *
	 * @return int
	 */
	protected int countSelectedResources() throws CoreException {
		int result= 0;
		Iterator resources= fJarPackage.getSelectedResources().iterator();
		while (resources.hasNext())
			result += countWithChildren((IResource) resources.next());
		return result;
	}
	/**
	 * Exports the passed resource to the JAR file
	 *
	 * @param resource org.eclipse.core.resources.IResource
	 */
	protected void exportResource(IResource resource, IProgressMonitor progressMonitor) throws InterruptedException {
		if (!resource.isAccessible())
			return;

		if (resource.getType() == IResource.FILE) {
			int leadSegmentsToRemove= 1;
			boolean isJavaProject;
			IJavaProject jProject= null;
			IPackageFragmentRoot pkgRoot= null;
			try {
				isJavaProject= resource.getProject().hasNature(JavaCore.NATURE_ID);
			} catch (CoreException ex) {
				isJavaProject= false;
			}
			if (isJavaProject) {
				jProject= JavaCore.create(resource.getProject());
				try {
					IPackageFragment pkgFragment= jProject.findPackageFragment(resource.getFullPath().removeLastSegments(1));
					if (pkgFragment != null) {
						pkgRoot= JavaModelUtility.getPackageFragmentRoot(pkgFragment);
						if (pkgRoot != null)
							leadSegmentsToRemove= pkgRoot.getPath().segmentCount();
					}
				} catch (JavaModelException ex) {
					// this should never happen due to inital tests
					// use leadSegmentsToRemove 1 in that case
				}
			}
			IPath destinationPath= resource.getFullPath().removeFirstSegments(leadSegmentsToRemove);
			progressMonitor.subTask(destinationPath.toString());
			
			try {				// Binary Export				if (fJarPackage.areClassFilesExported() && isJavaProject && pkgRoot != null) {
					// find corresponding file(s) on classpath and export
					Iterator iter= filesOnClasspath((IFile)resource, destinationPath, jProject);
					IPath baseDestinationPath= destinationPath.removeLastSegments(1);
					while (iter.hasNext()) {
						IFile file= (IFile)iter.next();						fJarWriter.write(file, baseDestinationPath.append(file.getName()));					}				}				// Java Files and resources				if (fJarPackage.areJavaFilesExported() && (!isJavaProject || isJavaFile(resource) || pkgRoot == null || !fJarPackage.areClassFilesExported()))					fJarWriter.write((IFile) resource, destinationPath);								} catch (IOException ex) {
				String message= ex.getMessage() + " ";
				if (message == null)
					message= "IO Error exporting " + resource.getFullPath();
				addWarning(message , ex);
			} catch (CoreException ex) {
				String message= ex.getMessage() + " ";
				if (message == null)
					message= "Core Error exporting " + resource.getFullPath();
				addWarning(message, ex);
			}

			progressMonitor.worked(1);
			ModalContext.checkCanceled(progressMonitor);
		} else {
			IResource[] children= null;

			try {
				children= ((IContainer) resource).members();
			} catch (CoreException e) {
				// this should never happen because an #isAccessible check is done before #members is invoked
				addWarning("Error exporting " + resource.getFullPath(), e);
			}

			for (int i= 0; i < children.length; i++)
				exportResource(children[i], progressMonitor);
		}
	}
	/**
	 * Exports the resources as specified by the JAR package.
	 */
	protected void exportSelectedResources(IProgressMonitor progressMonitor) throws InterruptedException {
		Iterator resources= fJarPackage.getSelectedResources().iterator();
		while (resources.hasNext()) {
			IResource currentResource= (IResource) resources.next();
			exportResource(currentResource, progressMonitor);
		}
	}
	/**
	 * Returns an iterator on a list with files that correspond to the
	 * passed file and that are on the classpath of its project.
	 *
	 * @param	file			the file for which to find the corresponding classpath resources
	 * @param	pathInJar		the path that the file has in the JAR (i.e. project and source folder segments removed)
	 * @param	javaProject		the javaProject that contains the file
	 * @return	the iterator over the corresponding classpath files for the given file
	 */
	protected Iterator filesOnClasspath(IFile file, IPath pathInJar, IJavaProject javaProject) throws CoreException, IOException {
		IPath outputPath= javaProject.getOutputLocation();		IContainer outputContainer;				if (javaProject.getProject().getFullPath().equals(outputPath))			outputContainer= javaProject.getProject();		else {			outputContainer= createFolderHandle(outputPath);
			if (outputContainer == null || !outputContainer.isAccessible())
				throw new IOException("Output container not accessible");		}
		if (isJavaFile(file)) {
			// Java CU - search files with .class ending			boolean hasErrors= fJarPackage.hasCompileErrors(file);			boolean hasWarnings= fJarPackage.hasCompileWarnings(file);			boolean canBeExported= canBeExported(hasErrors, hasWarnings);			reportPossibleCompileProblems(file, hasErrors, hasWarnings, canBeExported);			if (!canBeExported)				return Collections.EMPTY_LIST.iterator();			IFolder classFolder= outputContainer.getFolder(pathInJar.removeLastSegments(1));
			if (fClassFilesMapFolder == null || !fClassFilesMapFolder.equals(classFolder)) {
				fJavaNameToClassFilesMap= buildJavaToClassMap(classFolder);
				fClassFilesMapFolder= classFolder;
			}
			ArrayList classFiles= (ArrayList)fJavaNameToClassFilesMap.get(file.getName());
			if (classFiles == null || classFiles.isEmpty())
				throw new IOException("class file(s) on classpath not found or not  accessible " + file.getFullPath());
			return classFiles.iterator();
		}
		else {
			// resource  - search file with same name
			List binaryFiles= new ArrayList(5);
			IFile cpFile= outputContainer.getFile(pathInJar);
			if (cpFile.isAccessible())
				binaryFiles.add(cpFile);
			else 
				throw new IOException("Resource on classpath not found or not  accessible " + cpFile.getFullPath().toString());
			return binaryFiles.iterator();
		}
	}
	/**
	 * Answers whether the given resource is a Java file.
	 * The resource must be a file whose file name ends with ".java".
	 * 
	 * @return a <code>true<code> if the given resource is a Java file
	 */
	boolean isJavaFile(IResource file) {
		return file != null
			&& file.getType() == IFile.FILE
			&& file.getFileExtension() != null
			&& file.getFileExtension().equalsIgnoreCase("java");
	}
	/**
	 * Answers whether the given resource is a class file.
	 * The resource must be a file whose file name ends with ".class".
	 * 
	 * @return a <code>true<code> if the given resource is a class file
	 */
	boolean isClassFile(IResource file) {
		return file != null
			&& file.getType() == IFile.FILE
			&& file.getFileExtension() != null
			&& file.getFileExtension().equalsIgnoreCase("class");
	}
	/*
	 * Builds and returns a map that has the class files
	 * for each java file in a given directory
	 */
	private Map buildJavaToClassMap(IFolder folder) throws CoreException {
		if (folder == null || !folder.isAccessible())
			return new HashMap(0);
		org.eclipse.jdt.internal.ui.util.JdtHackFinder.fixme("Should not use internal class");
		org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader cfReader;
		IResource[] members= folder.members();
		Map map= new HashMap(members.length);
		for (int i= 0;  i < members.length; i++) {
			if (isClassFile(members[i])) {
				IFile classFile= (IFile)members[i];
				try {
					cfReader= org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader.read(classFile.getLocation().toFile());
				} catch (org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException ex) {
					addWarning("Class file has invalid format: " + classFile.getLocation().toFile(), ex);
					continue;
				} catch (IOException ex) {
					addWarning("IOError while looking for class file: " + classFile.getLocation().toFile(), ex);
					continue;
				}
				if (cfReader != null) {
					String javaName= new String(cfReader.sourceFileName());
					Object classFiles= map.get(javaName);
					if (classFiles == null) {
						classFiles= new ArrayList(3);
						map.put(javaName, classFiles);
					}
					((ArrayList)classFiles).add(classFile);
				}
			}		
		}
		return map;
	}
	/**
	 * Creates a file resource handle for the file with the given workspace path.
	 * This method does not create the file resource; this is the responsibility
	 * of <code>createFile</code>.
	 *
	 * @param filePath the path of the file resource to create a handle for
	 * @return the new file resource handle
	 * @see #createFile
	 */
	protected IFile createFileHandle(IPath filePath) {
		if (filePath.isValidPath(filePath.toString()) && filePath.segmentCount() >= 2)
			return JavaPlugin.getWorkspace().getRoot().getFile(filePath);
		else
			return null;
	}
	/**
	 * Creates a folder resource handle for the folder with the given workspace path.
	 *
	 * @param folderPath the path of the folder to create a handle for
	 * @return the new folder resource handle
	 */
	protected IFolder createFolderHandle(IPath folderPath) {
		if (folderPath.isValidPath(folderPath.toString()) && folderPath.segmentCount() >= 2)
			return JavaPlugin.getWorkspace().getRoot().getFolder(folderPath);
		else			return null;
	}
	/**
	 * Returns the status of this operation.
	 * If there were any errors, the result is a status object containing
	 * individual status objects for each error.
	 * If there were no errors, the result is a status object with error code <code>OK</code>.
	 *
	 * @return the status of this operation
	 */
	public IStatus getStatus() {
		IStatus[] errors= new IStatus[fErrors.size()];
		fErrors.toArray(errors);		MultiStatus status= new MultiStatus(JavaPlugin.getPluginId(), IStatus.OK, errors, "", null);		String message= "";		if (status.getSeverity() == IStatus.ERROR)			message= "JAR export failed (no JAR generated):";		if (status.getSeverity() == IStatus.WARNING)			message= "JAR export finished with warnings:";		// need to recreate because no API to set message		return new MultiStatus(JavaPlugin.getPluginId(), IStatus.OK, errors, message, null);	}
	/**
	 * Answer a boolean indicating whether the passed child is a descendent
	 * of one or more members of the passed resources collection
	 *
	 * @param resources	java.util.Vector
	 * @param child		org.eclipse.core.resources.IResource
	 * @return boolean
	 */
	protected boolean isDescendent(List resources, IResource child) {
		if (child.getType() == IResource.PROJECT)
			return false;

		IResource parent= child.getParent();
		if (resources.contains(parent))
			return true;

		return isDescendent(resources, parent);
	}	protected boolean canBeExported(boolean hasErrors, boolean hasWarnings) throws CoreException {		return (!hasErrors && !hasWarnings)			|| (hasErrors && fJarPackage.exportErrors())			|| (hasWarnings && fJarPackage.exportWarnings());	}	protected void reportPossibleCompileProblems(IFile file, boolean hasErrors, boolean hasWarnings, boolean canBeExported) {		String prefix;		if (canBeExported)			prefix= "Exported with compile ";		else			prefix= "Not exported due to compile ";		if (hasErrors && fJarPackage.logErrors())			addWarning(prefix + "errors: " + file.getFullPath(), null);		if (hasWarnings && fJarPackage.logWarnings())			addWarning(prefix + "warnings: " + file.getFullPath(), null);	}		/**
	 * Exports the resources as specified by the JAR package.
	 * 
	 * @param	progressMonitor	the progress monitor that displays the progress
	 * @see		#getStatus()
	 */
	public void run(IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {

		int totalWork;
		try {
			totalWork= countSelectedResources();
		} catch (CoreException ex) {
			addError("Unable to traveres all resources: " +  ex.getMessage(), ex);
			throw new InvocationTargetException(ex, "Unable to traveres all resources: " + ex.getMessage());
		}			

		progressMonitor.beginTask("Exporting:", totalWork);
		try {			if (!preconditionsOK())				throw new InvocationTargetException(null, "JAR creation failed. Details follow");			fJarWriter= new JarWriter(fJarPackage, fParentShell);
			exportSelectedResources(progressMonitor);		} catch (IOException ex) {
			addError("Unable to create JAR file: " +  ex.getMessage(), ex);
			throw new InvocationTargetException(ex, "Unable to create JAR file: " + ex.getMessage());
		} catch (CoreException ex) {
			addError("Unable to create JAR file because specified manifest is not OK: " + ex.getMessage(), ex);
			throw new InvocationTargetException(ex, "Unable to create JAR file because specified manifest is not OK: " + ex.getMessage());
		} finally {
			try {
				if (fJarWriter != null)
					fJarWriter.finished();
			} catch (IOException ex) {
				addError("Unable to close the JAR file: " + ex.getMessage(), ex);
				throw new InvocationTargetException(ex, "Unable to close the JAR file: " + ex.getMessage());
			}
			progressMonitor.done();
		}
	}
		protected boolean preconditionsOK() {		if (!fJarPackage.areClassFilesExported() && !fJarPackage.areJavaFilesExported()) {			addError("No export type chosen", null);			return false;		}		if (fJarPackage.getSelectedResources() == null || fJarPackage.getSelectedResources().size() == 0) {			addError("No resources selected", null);			return false;		}		if (fJarPackage.getJarLocation() == null) {			addError("Invalid JAR location", null);			return false;		}		if (!fJarPackage.doesManifestExist()) {			addError("Manifest does not exist", null);			return false;		}		if (!fJarPackage.isMainClassValid(new BusyIndicatorRunnableContext())) {			addError("Main class is not valid", null);			return false;		}		IEditorPart[] dirtyEditors= JavaPlugin.getDirtyEditors();		if (dirtyEditors.length > 0) {			List unsavedFiles= new ArrayList(dirtyEditors.length);			List selection= fJarPackage.getSelectedResources();			for (int i= 0; i < dirtyEditors.length; i++) {				if (dirtyEditors[i].getEditorInput() instanceof IFileEditorInput) {					IFile dirtyFile= ((IFileEditorInput)dirtyEditors[i].getEditorInput()).getFile();					if (selection.contains(dirtyFile)) {						unsavedFiles.add(dirtyFile);						addError("File is unsaved: " + dirtyFile.getFullPath(), null);					}				}			}			if (!unsavedFiles.isEmpty())				return false;		}		return true;	}}
