/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.jarpackager;import java.io.ByteArrayInputStream;import java.io.ByteArrayOutputStream;import java.io.IOException;import java.lang.reflect.InvocationTargetException;import java.util.ArrayList;import java.util.Collections;import java.util.HashMap;import java.util.Iterator;import java.util.List;import java.util.Map;import java.util.jar.Manifest;import org.eclipse.core.resources.IContainer;import org.eclipse.core.resources.IFile;import org.eclipse.core.resources.IFolder;import org.eclipse.core.resources.IResource;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IProgressMonitor;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.MultiStatus;import org.eclipse.core.runtime.Status;import org.eclipse.swt.widgets.Shell;import org.eclipse.jface.operation.IRunnableWithProgress;import org.eclipse.jface.operation.ModalContext;import org.eclipse.jface.util.Assert;import org.eclipse.ui.IEditorPart;import org.eclipse.ui.IFileEditorInput;import org.xml.sax.SAXException;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.IPackageFragmentRoot;import org.eclipse.jdt.core.JavaCore;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.internal.corext.util.JavaModelUtil;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.JavaStatusConstants;import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
/**
 * Operation for exporting a resource and its children to a new  JAR file.
 */
public class JarFileExportOperation implements IRunnableWithProgress {

	private JarWriter fJarWriter;
	private JarPackage fJarPackage;	private IFile[] fDescriptionFiles;
	private Shell fParentShell;
	private Map fJavaNameToClassFilesMap;
	private IContainer fClassFilesMapContainer;	private MultiStatus fProblems;		/**
	 * Creates an instance of this class.
	 *
	 * @param	jarPackage	the JAR package specification
	 * @param	parent	the parent for the dialog,
	 * 			or <code>null</code> if no dialog should be presented
	 */
	public JarFileExportOperation(JarPackage jarPackage, Shell parent) {		this(parent);		fJarPackage= jarPackage;	}	/**	 * Creates an instance of this class.	 *	 * @param	descriptions	an array with JAR package descriptions	 * @param	parent			the parent for the dialog,	 * 			or <code>null</code> if no dialog should be presented	 */	public JarFileExportOperation(IFile[] descriptions, Shell parent) {		this(parent);		fDescriptionFiles= descriptions;	}	/**
	 * Adds a new warning to the list with the passed information.	 * Normally the export operation continues after a warning.
	 * @param	message		the message
	 * @param	exception	the throwable that caused the warning, or <code>null</code>
	 */
	private JarFileExportOperation(Shell parent) {		fParentShell= parent;		fProblems= new MultiStatus(JavaPlugin.getPluginId(), JavaStatusConstants.INTERNAL_ERROR, JarPackagerMessages.getString("JarFileExportOperation.exportFinishedWithWarnings"), null); //$NON-NLS-1$	}	protected void addWarning(String message, Throwable error) {		if (fJarPackage == null || fJarPackage.logWarnings())
			fProblems.add(new Status(IStatus.WARNING, JavaPlugin.getPluginId(), JavaStatusConstants.INTERNAL_ERROR, message, error));
	}	/**	 * Adds a new error to the list with the passed information.	 * Normally an error terminates the export operation.	 * @param	message		the message	 * @param	exception	the throwable that caused the error, or <code>null</code>	 */	protected void addError(String message, Throwable error) {		if (fJarPackage == null || fJarPackage.logErrors())			fProblems.add(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), JavaStatusConstants.INTERNAL_ERROR, message, error));	}	/**
	 * Answers the number of file resources specified by the JAR package.
	 *
	 * @return int
	 */
	protected int countSelectedElements() {		int count= 0;		Iterator iter= fJarPackage.getSelectedElements().iterator();		while (iter.hasNext()) {			Object element= iter.next();			IResource resource= null;			if (element instanceof IJavaElement) {				IJavaElement je= (IJavaElement)element;				try {					resource= je.getUnderlyingResource();				} catch (JavaModelException ex) {					addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.underlyingResourceNotFound", je.getElementName()), ex); //$NON-NLS-1$					return count;				}			}			else				resource= (IResource)element;			if (resource.getType() == IResource.FILE)				count++;			else				count += getTotalChildCount((IContainer)resource);		}		return count;
	}		private int getTotalChildCount(IContainer container) {		IResource[] members;		try {			members= container.members();		} catch (CoreException ex) {			return 0;		}		int count= 0;		for (int i= 0; i < members.length; i++) {			if (members[i].getType() == IResource.FILE)				count++;			else				count += getTotalChildCount((IContainer)members[i]);		}		return count;	}
	/**
	 * Exports the passed resource to the JAR file
	 *
	 * @param element the resource or JavaElement to export
	 */
	protected void exportElement(Object element, IProgressMonitor progressMonitor) throws InterruptedException {		int leadSegmentsToRemove= 1;		IPackageFragmentRoot pkgRoot= null;		boolean isInJavaProject= false;		IResource resource= null;		IJavaProject jProject= null;		if (element instanceof IJavaElement) {			isInJavaProject= true;			IJavaElement je= (IJavaElement)element;			try {				resource= je.getUnderlyingResource();			} catch (JavaModelException ex) {				addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.underlyingResourceNotFound", je.getElementName()), ex); //$NON-NLS-1$				return;			}			jProject= je.getJavaProject();			pkgRoot= JavaModelUtil.getPackageFragmentRoot(je);		}		else			resource= (IResource)element;		if (!resource.isAccessible()) {			addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.resourceNotFound", resource.getFullPath()), null); //$NON-NLS-1$			return;		}
		if (resource.getType() == IResource.FILE) {			if (!resource.isLocal(IResource.DEPTH_ZERO))
				try {					resource.setLocal(true , IResource.DEPTH_ZERO, progressMonitor);				} catch (CoreException ex) {
					addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.resourceNotLocal", resource.getFullPath()), ex); //$NON-NLS-1$					return;				}			if (!isInJavaProject) {				// check if it's a Java resource				try {					isInJavaProject= resource.getProject().hasNature(JavaCore.NATURE_ID);				} catch (CoreException ex) {					addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.projectNatureNotDeterminable", resource.getFullPath()), ex); //$NON-NLS-1$					return;				}				if (isInJavaProject) {
					jProject= JavaCore.create(resource.getProject());
					try {						IPackageFragment pkgFragment= jProject.findPackageFragment(resource.getFullPath().removeLastSegments(1));
						if (pkgFragment != null)
							pkgRoot= JavaModelUtil.getPackageFragmentRoot(pkgFragment);						else							pkgRoot= jProject.findPackageFragmentRoot(resource.getFullPath().uptoSegment(2));					} catch (JavaModelException ex) {
						addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.javaPackageNotDeterminable", resource.getFullPath()), ex); //$NON-NLS-1$						return;					}
				}			}						if (pkgRoot != null) {				leadSegmentsToRemove= pkgRoot.getPath().segmentCount();				if (fJarPackage.useSourceFolderHierarchy()&& !pkgRoot.getElementName().equals(pkgRoot.DEFAULT_PACKAGEROOT_PATH))					leadSegmentsToRemove--;			}			
			IPath destinationPath= resource.getFullPath().removeFirstSegments(leadSegmentsToRemove);
						boolean isInOutputFolder= false;			if (isInJavaProject) {				try {					isInOutputFolder= jProject.getOutputLocation().isPrefixOf(resource.getFullPath());				} catch (JavaModelException ex) {					isInOutputFolder= false;				}			}			
			exportClassFiles(progressMonitor, pkgRoot, resource, jProject, destinationPath);
			exportResourceFiles(progressMonitor, pkgRoot, isInJavaProject, resource, destinationPath, isInOutputFolder);
			progressMonitor.worked(1);
			ModalContext.checkCanceled(progressMonitor);
		} else			exportContainer(progressMonitor, resource);
	}

	private void exportContainer(IProgressMonitor progressMonitor, IResource resource) throws java.lang.InterruptedException {
		IResource[] children= null;
		try {
			children= ((IContainer) resource).members();
		} catch (CoreException e) {
			// this should never happen because an #isAccessible check is done before #members is invoked
			addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.errorDuringExport", resource.getFullPath()), e); //$NON-NLS-1$
		}
		for (int i= 0; i < children.length; i++)
			exportElement(children[i], progressMonitor);
	}

	private void exportResourceFiles(IProgressMonitor progressMonitor, IPackageFragmentRoot pkgRoot, boolean isInJavaProject, IResource resource, IPath destinationPath, boolean isInOutputFolder) {
		boolean isNonJavaResource= !isInJavaProject || (pkgRoot == null && !isInOutputFolder);
		boolean isInClassFolder= false;
		try {
			isInClassFolder= pkgRoot != null && !pkgRoot.isArchive() && pkgRoot.getKind() == pkgRoot.K_BINARY;
		} catch (JavaModelException ex) {
			addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.cantGetRootKind", resource.getFullPath()), ex); //$NON-NLS-1$
		}
		if ((fJarPackage.areClassFilesExported() &&
					((isNonJavaResource || (pkgRoot != null && !isJavaFile(resource) && !isClassFile(resource)))
					|| isInClassFolder && isClassFile(resource)))
			|| (fJarPackage.areJavaFilesExported() && (isNonJavaResource || (pkgRoot != null && !isClassFile(resource))))) {
			try {				progressMonitor.subTask(destinationPath.toString());
				fJarWriter.write((IFile) resource, destinationPath);
			} catch (IOException ex) {
				String message= ex.getMessage();
				if (message == null)
					message= JarPackagerMessages.getFormattedString("JarFileExportOperation.ioErrorDuringExport", resource.getFullPath()); //$NON-NLS-1$
				addWarning(message , ex);
			} catch (CoreException ex) {
				String message= ex.getMessage();
				if (message == null)
					message= JarPackagerMessages.getFormattedString("JarFileExportOperation.coreErrorDuringExport", resource.getFullPath()); //$NON-NLS-1$
				addWarning(message, ex);
			}
		}					
	}

	private void exportClassFiles(IProgressMonitor progressMonitor, IPackageFragmentRoot pkgRoot, IResource resource, IJavaProject jProject, IPath destinationPath) {
		if (fJarPackage.areClassFilesExported() && isJavaFile(resource) && pkgRoot != null) {
			try {
				// find corresponding file(s) on classpath and export
				Iterator iter= filesOnClasspath((IFile)resource, destinationPath, jProject, progressMonitor);
				IPath baseDestinationPath= destinationPath.removeLastSegments(1);
				while (iter.hasNext()) {
					IFile file= (IFile)iter.next();
					if (!resource.isLocal(IResource.DEPTH_ZERO))						
						file.setLocal(true , IResource.DEPTH_ZERO, progressMonitor);					IPath classFilePath= baseDestinationPath.append(file.getName());
					progressMonitor.subTask(classFilePath.toString());					fJarWriter.write(file, classFilePath);				}
			} catch (IOException ex) {
				String message= ex.getMessage();
				if (message == null)
					message= JarPackagerMessages.getFormattedString("JarFileExportOperation.ioErrorDuringExport", resource.getFullPath()); //$NON-NLS-1$
				addWarning(message , ex);
			} catch (CoreException ex) {
				String message= ex.getMessage();
				if (message == null)
					message= JarPackagerMessages.getFormattedString("JarFileExportOperation.coreErrorDuringExport", resource.getFullPath()); //$NON-NLS-1$
				addWarning(message, ex);
			}
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
	protected Iterator filesOnClasspath(IFile file, IPath pathInJar, IJavaProject javaProject, IProgressMonitor progressMonitor) throws CoreException {
		IPath outputPath= javaProject.getOutputLocation();		IContainer outputContainer;				if (javaProject.getProject().getFullPath().equals(outputPath))			outputContainer= javaProject.getProject();		else {			outputContainer= createFolderHandle(outputPath);
			if (outputContainer == null || !outputContainer.isAccessible()) {
				String msg= JarPackagerMessages.getString("JarFileExportOperation.outputContainerNotAccessible"); //$NON-NLS-1$				throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), JavaStatusConstants.INTERNAL_ERROR, msg, null));			}		}
		if (isJavaFile(file)) {
			// Java CU - search files with .class ending			boolean hasErrors= fJarPackage.hasCompileErrors(file);			boolean hasWarnings= fJarPackage.hasCompileWarnings(file);			boolean canBeExported= canBeExported(hasErrors, hasWarnings);			reportPossibleCompileProblems(file, hasErrors, hasWarnings, canBeExported);			if (!canBeExported)				return Collections.EMPTY_LIST.iterator();			IContainer classContainer= outputContainer;			if (pathInJar.segmentCount() > 1)				classContainer= outputContainer.getFolder(pathInJar.removeLastSegments(1));
			if (fClassFilesMapContainer == null || !fClassFilesMapContainer.equals(classContainer)) {
				fJavaNameToClassFilesMap= buildJavaToClassMap(classContainer);
				fClassFilesMapContainer= classContainer;
			}
			ArrayList classFiles= (ArrayList)fJavaNameToClassFilesMap.get(file.getName());
			if (classFiles == null || classFiles.isEmpty()) {
				String msg= JarPackagerMessages.getFormattedString("JarFileExportOperation.classFileOnClasspathNotAccessible", file.getFullPath()); //$NON-NLS-1$				throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), JavaStatusConstants.INTERNAL_ERROR, msg, null));			}
			return classFiles.iterator();
		}
		else {
			// resource  - search file with same name
			List binaryFiles= new ArrayList(1);			IFile cpFile= outputContainer.getFile(pathInJar);
			if (cpFile.isAccessible()) {				if (!cpFile.isLocal(IResource.DEPTH_ZERO))					cpFile.setLocal(true , IResource.DEPTH_ZERO, progressMonitor);				binaryFiles.add(cpFile);			}
			else {				String msg= JarPackagerMessages.getFormattedString("JarFileExportOperation.resourceOnCasspathNotAccessible", cpFile.getFullPath()); //$NON-NLS-1$				throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), JavaStatusConstants.INTERNAL_ERROR, msg, null));			}
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
			&& file.getFileExtension().equalsIgnoreCase("java"); //$NON-NLS-1$
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
			&& file.getFileExtension().equalsIgnoreCase("class"); //$NON-NLS-1$
	}
	/*
	 * Builds and returns a map that has the class files
	 * for each java file in a given directory
	 */
	private Map buildJavaToClassMap(IContainer container) throws CoreException {		if (container == null || !container.isAccessible())
			return new HashMap(0);
		/*		 * XXX: Bug 6584: Need a way to get class files for a java file (or CU)		 */
		org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader cfReader;
		IResource[] members= container.members();
		Map map= new HashMap(members.length);
		for (int i= 0;  i < members.length; i++) {
			if (isClassFile(members[i])) {
				IFile classFile= (IFile)members[i];
				try {
					cfReader= org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader.read(classFile.getLocation().toFile());
				} catch (org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException ex) {
					addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.invalidClassFileFormat", classFile.getLocation().toFile()), ex); //$NON-NLS-1$
					continue;
				} catch (IOException ex) {
					addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.ioErrorDuringClassFileLookup", classFile.getLocation().toFile()), ex); //$NON-NLS-1$
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
		if (fProblems.getSeverity() == IStatus.ERROR) {			String message= null;			if (fDescriptionFiles != null && fDescriptionFiles.length > 1)				message= JarPackagerMessages.getString("JarFileExportOperation.creationOfSomeJARsFailed"); //$NON-NLS-1$			else				message= JarPackagerMessages.getString("JarFileExportOperation.jarCreationFailed"); //$NON-NLS-1$			// Create new status because we want another message - no API to set message			return new MultiStatus(JavaPlugin.getPluginId(), 0, fProblems.getChildren(), message, null);		}			return fProblems;	}
	/**
	 * Answer a boolean indicating whether the passed child is a descendant
	 * of one or more members of the passed resources collection
	 *
	 * @param	resources	a List contain potential parents
	 * @param	child		the resource to test
	 * @return	a <code>boolean</code> indicating if the child is a descendant
	 */
	protected boolean isDescendant(List resources, IResource child) {
		if (child.getType() == IResource.PROJECT)
			return false;

		IResource parent= child.getParent();
		if (resources.contains(parent))
			return true;

		return isDescendant(resources, parent);
	}	protected boolean canBeExported(boolean hasErrors, boolean hasWarnings) throws CoreException {		return (!hasErrors && !hasWarnings)			|| (hasErrors && fJarPackage.exportErrors())			|| (hasWarnings && fJarPackage.exportWarnings());	}	protected void reportPossibleCompileProblems(IFile file, boolean hasErrors, boolean hasWarnings, boolean canBeExported) {		if (hasErrors) {			if (canBeExported)				addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.exportedWithCompileErrors", file.getFullPath()), null); //$NON-NLS-1$			else				addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.notExportedDueToCompileErrors", file.getFullPath()), null); //$NON-NLS-1$		}		if (hasWarnings) {			if (canBeExported)				addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.exportedWithCompileWarnings", file.getFullPath()), null); //$NON-NLS-1$			else				addWarning(JarPackagerMessages.getFormattedString("JarFileExportOperation.notExportedDueToCompileWarnings", file.getFullPath()), null); //$NON-NLS-1$		}	}	/**
	 * Exports the resources as specified by the JAR package.
	 * 
	 * @param	progressMonitor	the progress monitor that displays the progress
	 * @see		#getStatus()
	 */
	public void run(IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {		if (fJarPackage != null)			singleRun(progressMonitor);		else {			int jarCount= fDescriptionFiles.length;			for (int i= 0; i < jarCount; i++) {				fJarPackage= readJarPackage(fDescriptionFiles[i]);				if (fJarPackage != null)					singleRun(progressMonitor);			}		}	}	public void singleRun(IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {		int totalWork= countSelectedElements();
		progressMonitor.beginTask(JarPackagerMessages.getString("JarFileExportOperation.exporting"), totalWork); //$NON-NLS-1$
		try {			if (!preconditionsOK())				throw new InvocationTargetException(null, JarPackagerMessages.getString("JarFileExportOperation.jarCreationFailedSeeDetails")); //$NON-NLS-1$			fJarWriter= new JarWriter(fJarPackage, fParentShell);
			exportSelectedElements(progressMonitor);			if (getStatus().getSeverity() != IStatus.ERROR) {				progressMonitor.subTask(JarPackagerMessages.getString("JarFileExportOperation.savingFiles")); //$NON-NLS-1$				saveFiles();			}		} catch (IOException ex) {			String message= JarPackagerMessages.getFormattedString("JarFileExportOperation.unableToCreateJarFle",  ex.getMessage()); //$NON-NLS-1$
			addError(message, ex);
			throw new InvocationTargetException(ex, message);
		} catch (CoreException ex) {			String message= JarPackagerMessages.getFormattedString("JarFileExportOperation.unableToCreateJarFileDueToInvalidManifest", ex.getMessage()); //$NON-NLS-1$
			addError(message, ex);
			throw new InvocationTargetException(ex, message);
		} finally {
			try {
				if (fJarWriter != null)
					fJarWriter.close();
			} catch (IOException ex) {				String message= JarPackagerMessages.getFormattedString("JarFileExportOperation.unableToCloseJarFile", ex.getMessage()); //$NON-NLS-1$
				addError(message, ex);
				throw new InvocationTargetException(ex, message);
			}
			progressMonitor.done();
		}
	}
		protected boolean preconditionsOK() {		if (!fJarPackage.areClassFilesExported() && !fJarPackage.areJavaFilesExported()) {			addError(JarPackagerMessages.getString("JarFileExportOperation.noExportTypeChosen"), null); //$NON-NLS-1$			return false;		}		if (fJarPackage.getSelectedElements() == null || fJarPackage.getSelectedElements().size() == 0) {			addError(JarPackagerMessages.getString("JarFileExportOperation.noResourcesSelected"), null); //$NON-NLS-1$			return false;		}		if (fJarPackage.getJarLocation() == null) {			addError(JarPackagerMessages.getString("JarFileExportOperation.invalidJarLocation"), null); //$NON-NLS-1$			return false;		}		if (!fJarPackage.doesManifestExist()) {			addError(JarPackagerMessages.getString("JarFileExportOperation.manifestDoesNotExist"), null); //$NON-NLS-1$			return false;		}		if (!fJarPackage.isMainClassValid(new BusyIndicatorRunnableContext())) {			addError(JarPackagerMessages.getString("JarFileExportOperation.invalidMainClass"), null); //$NON-NLS-1$			return false;		}		IEditorPart[] dirtyEditors= JavaPlugin.getDirtyEditors();		if (dirtyEditors.length > 0) {			List unsavedFiles= new ArrayList(dirtyEditors.length);			List selection= fJarPackage.getSelectedResources();			for (int i= 0; i < dirtyEditors.length; i++) {				if (dirtyEditors[i].getEditorInput() instanceof IFileEditorInput) {					IFile dirtyFile= ((IFileEditorInput)dirtyEditors[i].getEditorInput()).getFile();					if (selection.contains(dirtyFile)) {						unsavedFiles.add(dirtyFile);						addError(JarPackagerMessages.getFormattedString("JarFileExportOperation.fileUnsaved", dirtyFile.getFullPath()), null); //$NON-NLS-1$					}				}			}			if (!unsavedFiles.isEmpty())				return false;		}		return true;	}	protected void saveFiles() {		// Save the manifest		if (fJarPackage.isManifestSaved()) {			try {				saveManifest();			} catch (CoreException ex) {				addError(JarPackagerMessages.getString("JarFileExportOperation.errorSavingManifest"), ex); //$NON-NLS-1$			} catch (IOException ex) {				addError(JarPackagerMessages.getString("JarFileExportOperation.errorSavingManifest"), ex); //$NON-NLS-1$			}		}				// Save the description		if (fJarPackage.isDescriptionSaved()) {			try {				saveDescription();			} catch (CoreException ex) {				addError(JarPackagerMessages.getString("JarFileExportOperation.errorSavingDescription"), ex); //$NON-NLS-1$			} catch (IOException ex) {				addError(JarPackagerMessages.getString("JarFileExportOperation.errorSavingDescription"), ex); //$NON-NLS-1$			}		}	}	protected void saveDescription() throws CoreException, IOException {		// Adjust JAR package attributes		if (fJarPackage.isManifestReused())			fJarPackage.setGenerateManifest(false);		ByteArrayOutputStream objectStreamOutput= new ByteArrayOutputStream();		JarPackageWriter objectStream= new JarPackageWriter(objectStreamOutput);		ByteArrayInputStream fileInput= null;		try {			objectStream.writeXML(fJarPackage);			fileInput= new ByteArrayInputStream(objectStreamOutput.toByteArray());			if (fJarPackage.getDescriptionFile().isAccessible()) {				if (fJarPackage.canOverwriteDescription(fParentShell))					fJarPackage.getDescriptionFile().setContents(fileInput, true, true, null);			}			else {				fJarPackage.getDescriptionFile().create(fileInput, true, null);				}		} finally {			if (fileInput != null)				fileInput.close();			if (objectStream != null)				objectStream.close();		}	}	protected void saveManifest() throws CoreException, IOException {		ByteArrayOutputStream manifestOutput= new ByteArrayOutputStream();		ByteArrayInputStream fileInput= null;		try {			Manifest manifest= fJarPackage.getManifestProvider().create(fJarPackage);			manifest.write(manifestOutput);			fileInput= new ByteArrayInputStream(manifestOutput.toByteArray());			if (fJarPackage.getManifestFile().isAccessible()) {				if (fJarPackage.canOverwriteManifest(fParentShell))				fJarPackage.getManifestFile().setContents(fileInput, true, true, null);			}			else {				fJarPackage.getManifestFile().create(fileInput, true, null);			}		} finally {			if (manifestOutput != null)				manifestOutput.close();			if (fileInput != null)				fileInput.close();		}	}	/**	 * Reads the JAR package spec from file.	 */	protected JarPackage readJarPackage(IFile description) {		Assert.isLegal(description.isAccessible());		Assert.isNotNull(description.getFileExtension());		Assert.isLegal(description.getFileExtension().equals(JarPackage.DESCRIPTION_EXTENSION));		JarPackage jarPackage= null;		JarPackageReader reader= null;		try {			reader= new JarPackageReader(description.getContents());			// Do not save - only generate JAR			jarPackage= reader.readXML();			jarPackage.setSaveManifest(false);			jarPackage.setSaveDescription(false);		} catch (CoreException ex) {				addError(JarPackagerMessages.getString("JarFileExportOperation.errorReadingJarPackageFromDescription"), ex); //$NON-NLS-1$		} catch (IOException ex) {				String message= JarPackagerMessages.getFormattedString("JarFileExportOperation.errorReadingFile", description.getFullPath(), ex.getMessage()); //$NON-NLS-1$				addError(message, null);		} catch (SAXException ex) {				String message= JarPackagerMessages.getFormattedString("JarFileExportOperation.badXmlFormat", description.getFullPath(), ex.getMessage()); //$NON-NLS-1$				addError(message, null);		} finally {			if ((jarPackage == null || jarPackage.logWarnings()) && reader != null)				// AddWarnings				fProblems.addAll(reader.getWarnings());			try {				if (reader != null)					reader.close();			}			catch (IOException ex) {				addError(JarPackagerMessages.getFormattedString("JarFileExportOperation.errorClosingJarPackageDescriptionReader", description.getFullPath()), ex); //$NON-NLS-1$			}		}		return jarPackage;	}}
