/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.jarpackager;import java.io.ByteArrayInputStream;import java.io.ByteArrayOutputStream;import java.io.IOException;import java.lang.reflect.InvocationTargetException;import java.util.ArrayList;import java.util.Collections;import java.util.HashMap;import java.util.Iterator;import java.util.List;import java.util.Map;import java.util.jar.Manifest;import org.eclipse.core.resources.IContainer;import org.eclipse.core.resources.IFile;import org.eclipse.core.resources.IFolder;import org.eclipse.core.resources.IResource;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.MultiStatus;import org.eclipse.core.runtime.Status;import org.eclipse.swt.widgets.Shell;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.jface.operation.ModalContext;import org.eclipse.jface.util.Assert;import org.eclipse.ui.IEditorPart;import org.eclipse.ui.IFileEditorInput;import org.xml.sax.SAXException;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.IPackageFragmentRoot;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;import org.eclipse.jdt.internal.ui.util.JavaModelUtility;import org.eclipse.jdt.internal.ui.util.JdtHackFinder;
/**
 * Operation for exporting a resource and its children to a new  JAR file.
 */
public class JarFileExportOperation implements IRunnableWithProgress {

	private JarWriter fJarWriter;
	private JarPackage fJarPackage;	private IFile[] fDescriptionFiles;
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
	public JarFileExportOperation(JarPackage jarPackage, Shell parent) {		fJarPackage= jarPackage;		fParentShell= parent;	}	/**	 * Creates an instance of this class.	 *	 * @param	descriptions	an array with JAR package descriptions	 * @param	parent			the parent for the dialog,	 * 			or <code>null</code> if no dialog should be presented	 */	public JarFileExportOperation(IFile[] descriptions, Shell parent) {		fDescriptionFiles= descriptions;		fParentShell= parent;	}	/**
	 * Adds a new warning to the list with the passed information.	 * Normally the export operation continues after a warning.
	 * @param	message		the message
	 * @param	exception	the throwable that caused the warning, or <code>null</code>
	 */
	protected void addWarning(String message, Throwable error) {		if (fJarPackage == null || fJarPackage.logWarnings())
			fErrors.add(new Status(IStatus.WARNING, JavaPlugin.getPluginId(), 0, message, error));
	}	/**	 * Adds a new error to the list with the passed information.	 * Normally an error terminates the export operation.	 * @param	message		the message	 * @param	exception	the throwable that caused the error, or <code>null</code>	 */	protected void addError(String message, Throwable error) {		if (fJarPackage == null || fJarPackage.logErrors())			fErrors.add(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), 0, message, error));	}	/**
	 * Answers the number of file resources specified by the JAR package.
	 *
	 * @return int
	 */
	protected int countSelectedElements() {		return fJarPackage.getSelectedElements().size();
	}
	/**
	 * Exports the passed resource to the JAR file
	 *
	 * @param element the resource or JavaElement to export
	 */
	protected void exportElement(Object element, IProgressMonitor progressMonitor) throws InterruptedException {		int leadSegmentsToRemove= 1;		IPackageFragmentRoot pkgRoot= null;		boolean isJavaElement= false;		IResource resource= null;		IJavaProject jProject= null;		if (element instanceof IJavaElement) {			isJavaElement= true;			IJavaElement je= (IJavaElement)element;			try {				resource= je.getUnderlyingResource();			} catch (JavaModelException ex) {				addWarning("Underlying resource not found for compilation unit: " + je.getElementName(), ex);				return;			}			jProject= je.getJavaProject();			pkgRoot= JavaModelUtility.getPackageFragmentRoot(je);			if (pkgRoot != null)				leadSegmentsToRemove= pkgRoot.getPath().segmentCount();		}		else			resource= (IResource)element;		if (!resource.isAccessible()) {			addWarning("Resource not found or not  accessible: " + resource.getFullPath(), null);			return;		}
		if (resource.getType() == IResource.FILE) {			if (!resource.isLocal(IResource.DEPTH_ZERO))
				try {					resource.setLocal(true , IResource.DEPTH_ZERO, progressMonitor);				} catch (CoreException ex) {
					addWarning("Resource could not be retrieved locally: " + resource.getFullPath(), ex);					return;				}			if (!isJavaElement) {				// check if it's a Java resource				try {					isJavaElement= resource.getProject().hasNature(JavaCore.NATURE_ID);				} catch (CoreException ex) {					addWarning("Project nature could not be determined for: " + resource.getFullPath(), ex);					return;				}				if (isJavaElement) {
					jProject= JavaCore.create(resource.getProject());
					try {
						IPackageFragment pkgFragment= jProject.findPackageFragment(resource.getFullPath().removeLastSegments(1));
						if (pkgFragment != null) {
							pkgRoot= JavaModelUtility.getPackageFragmentRoot(pkgFragment);
							if (pkgRoot != null)
								leadSegmentsToRemove= pkgRoot.getPath().segmentCount();
						}
					} catch (JavaModelException ex) {
						addWarning("Java package could not be found for: " + resource.getFullPath(), ex);						return;					}
				}			}
			IPath destinationPath= resource.getFullPath().removeFirstSegments(leadSegmentsToRemove);
			progressMonitor.subTask(destinationPath.toString());
			
			try {				// Binary Export				if (fJarPackage.areClassFilesExported() && isJavaElement && pkgRoot != null) {
					// find corresponding file(s) on classpath and export
					Iterator iter= filesOnClasspath((IFile)resource, destinationPath, jProject, progressMonitor);
					IPath baseDestinationPath= destinationPath.removeLastSegments(1);
					while (iter.hasNext()) {
						IFile file= (IFile)iter.next();						if (!resource.isLocal(IResource.DEPTH_ZERO))													file.setLocal(true , IResource.DEPTH_ZERO, progressMonitor);						fJarWriter.write(file, baseDestinationPath.append(file.getName()));					}				}				// Java Files and resources				if (fJarPackage.areJavaFilesExported() && (!isJavaElement || isJavaFile(resource) || pkgRoot == null || !fJarPackage.areClassFilesExported()))					fJarWriter.write((IFile) resource, destinationPath);								} catch (IOException ex) {
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
				exportElement(children[i], progressMonitor);
		}
	}
	/**
	 * Exports the resources as specified by the JAR package.
	 */
	protected void exportSelectedElements(IProgressMonitor progressMonitor) throws InterruptedException {
		Iterator iter= fJarPackage.getSelectedElements().iterator();
		while (iter.hasNext())
			exportElement(iter.next(), progressMonitor);
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
	protected Iterator filesOnClasspath(IFile file, IPath pathInJar, IJavaProject javaProject, IProgressMonitor progressMonitor) throws CoreException, IOException {
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
			if (cpFile.isAccessible()) {				if (!cpFile.isLocal(IResource.DEPTH_ZERO))					cpFile.setLocal(true , IResource.DEPTH_ZERO, progressMonitor);				binaryFiles.add(cpFile);			}
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
		fErrors.toArray(errors);		MultiStatus status= new MultiStatus(JavaPlugin.getPluginId(), IStatus.OK, errors, "", null);		String message= "";		if (status.getSeverity() == IStatus.ERROR)			message= "JAR export failed:";		if (status.getSeverity() == IStatus.WARNING)			message= "JAR export finished with warnings:";		// need to recreate because no API to set message		return new MultiStatus(JavaPlugin.getPluginId(), IStatus.OK, errors, message, null);	}
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
	}	protected boolean canBeExported(boolean hasErrors, boolean hasWarnings) throws CoreException {		return (!hasErrors && !hasWarnings)			|| (hasErrors && fJarPackage.exportErrors())			|| (hasWarnings && fJarPackage.exportWarnings());	}	protected void reportPossibleCompileProblems(IFile file, boolean hasErrors, boolean hasWarnings, boolean canBeExported) {		String prefix;		if (canBeExported)			prefix= "Exported with compile ";		else			prefix= "Not exported due to compile ";		if (hasErrors)			addWarning(prefix + "errors: " + file.getFullPath(), null);		if (hasWarnings)			addWarning(prefix + "warnings: " + file.getFullPath(), null);	}		/**
	 * Exports the resources as specified by the JAR package.
	 * 
	 * @param	progressMonitor	the progress monitor that displays the progress
	 * @see		#getStatus()
	 */
	public void run(IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {		if (fJarPackage != null)			singleRun(progressMonitor);		else {			int jarCount= fDescriptionFiles.length;			for (int i= 0; i < jarCount; i++) {				fJarPackage= readJarPackage(fDescriptionFiles[i]);				if (fJarPackage != null)					singleRun(progressMonitor);			}		}	}	public void singleRun(IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {		int totalWork= countSelectedElements();
		progressMonitor.beginTask("Exporting:", totalWork);
		try {			if (!preconditionsOK())				throw new InvocationTargetException(null, "JAR creation failed. Details follow");			fJarWriter= new JarWriter(fJarPackage, fParentShell);
			exportSelectedElements(progressMonitor);			if (getStatus().getSeverity() != IStatus.ERROR) {				progressMonitor.subTask("Saving files...");				saveFiles();			}		} catch (IOException ex) {
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
		protected boolean preconditionsOK() {		if (!fJarPackage.areClassFilesExported() && !fJarPackage.areJavaFilesExported()) {			addError("No export type chosen", null);			return false;		}		if (fJarPackage.getSelectedElements() == null || fJarPackage.getSelectedElements().size() == 0) {			addError("No resources selected", null);			return false;		}		if (fJarPackage.getJarLocation() == null) {			addError("Invalid JAR location", null);			return false;		}		if (!fJarPackage.doesManifestExist()) {			addError("Manifest does not exist", null);			return false;		}		if (!fJarPackage.isMainClassValid(new BusyIndicatorRunnableContext())) {			addError("Main class is not valid", null);			return false;		}		IEditorPart[] dirtyEditors= JavaPlugin.getDirtyEditors();		if (dirtyEditors.length > 0) {			List unsavedFiles= new ArrayList(dirtyEditors.length);			List selection= fJarPackage.getSelectedElements();			for (int i= 0; i < dirtyEditors.length; i++) {				if (dirtyEditors[i].getEditorInput() instanceof IFileEditorInput) {					IFile dirtyFile= ((IFileEditorInput)dirtyEditors[i].getEditorInput()).getFile();					if (selection.contains(dirtyFile)) {						unsavedFiles.add(dirtyFile);						addError("File is unsaved: " + dirtyFile.getFullPath(), null);					}				}			}			if (!unsavedFiles.isEmpty())				return false;		}		return true;	}	protected void saveFiles() {		// Save the manifest		if (fJarPackage.isManifestSaved()) {			try {				saveManifest();			} catch (CoreException ex) {				addError("Saving manifest in workspace failed", ex);			} catch (IOException ex) {				addError("Saving manifest in workspace failed", ex);			}		}				// Save the description		if (fJarPackage.isDescriptionSaved()) {			try {				saveDescription();			} catch (CoreException ex) {				addError("Saving description in workspace failed", ex);			} catch (IOException ex) {				addError("Saving description in workspace failed", ex);			}		}	}	protected void saveDescription() throws CoreException, IOException {		// Adjust JAR package attributes		if (fJarPackage.isManifestReused())			fJarPackage.setGenerateManifest(false);		ByteArrayOutputStream objectStreamOutput= new ByteArrayOutputStream();		JarPackageWriter objectStream= new JarPackageWriter(objectStreamOutput);		ByteArrayInputStream fileInput= null;		try {			objectStream.writeXML(fJarPackage);			fileInput= new ByteArrayInputStream(objectStreamOutput.toByteArray());			if (fJarPackage.getDescriptionFile().isAccessible() && fJarPackage.allowOverwrite())				fJarPackage.getDescriptionFile().setContents(fileInput, true, true, null);			else {				org.eclipse.jdt.internal.ui.util.JdtHackFinder.fixme("Should ask again");				fJarPackage.getDescriptionFile().create(fileInput, true, null);				}		}		finally {			if (fileInput != null)				fileInput.close();			if (objectStream != null)				objectStream.close();		}	}	protected void saveManifest() throws CoreException, IOException {		ByteArrayOutputStream manifestOutput= new ByteArrayOutputStream();		ByteArrayInputStream fileInput= null;		try {			Manifest manifest= ManifestFactory.getInstance().create(fJarPackage);			manifest.write(manifestOutput);			fileInput= new ByteArrayInputStream(manifestOutput.toByteArray());			if (fJarPackage.getManifestFile().isAccessible() && fJarPackage.allowOverwrite())				fJarPackage.getManifestFile().setContents(fileInput, true, true, null);			else {				org.eclipse.jdt.internal.ui.util.JdtHackFinder.fixme("Should ask again");				fJarPackage.getManifestFile().create(fileInput, true, null);			}		}		finally {			if (manifestOutput != null)				manifestOutput.close();			if (fileInput != null)				fileInput.close();		}	}	/**	 * Reads the JAR package spec from file.	 */	protected JarPackage readJarPackage(IFile description) {		Assert.isLegal(description.isAccessible());		Assert.isNotNull(description.getFileExtension());		Assert.isLegal(description.getFileExtension().equals(JarPackage.DESCRIPTION_EXTENSION));		JarPackage jarPackage= null;		JarPackageReader reader= null;		try {			reader= new JarPackageReader(description.getContents());			// Do not save - only generate JAR			jarPackage= reader.readXML();			jarPackage.setSaveManifest(false);			jarPackage.setSaveDescription(false);		} catch (CoreException ex) {				addError("Error reading JAR package from description", ex);		} catch (IOException ex) {				addError("Error reading " + description.getFullPath() + ": " + ex.getMessage(), null);		} catch (SAXException ex) {				addError("Bad XML format in " + description.getFullPath() + ": " + ex.getMessage(), null);		} finally {			try {				if (reader != null)					reader.close();			}			catch (IOException ex) {				addError("Error closing JAR package description reader for" + description.getFullPath(), ex);			}		}		return jarPackage;	}}
