/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.jarpackager;import java.io.ByteArrayOutputStream;import java.io.File;import java.io.IOException;import java.io.InputStream;import java.io.OutputStream;import java.io.Serializable;import java.util.ArrayList;import java.util.HashSet;import java.util.Iterator;import java.util.List;import java.util.Set;import org.eclipse.core.resources.IFile;import org.eclipse.core.resources.IMarker;import org.eclipse.core.resources.IResource;import org.eclipse.core.runtime.CoreException;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.Path;import org.eclipse.swt.widgets.Display;import org.eclipse.swt.widgets.Shell;import org.eclipse.jface.dialogs.MessageDialog;import org.eclipse.jface.operation.IRunnableContext;import org.eclipse.jdt.core.ICompilationUnit;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IJavaModelMarker;import org.eclipse.jdt.core.IPackageFragment;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.core.search.IJavaSearchScope;import org.eclipse.jdt.core.search.SearchEngine;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;import org.eclipse.jdt.internal.ui.util.JavaModelUtil;import org.eclipse.jdt.internal.ui.util.MainMethodSearchEngine;

/**
 * Model for a JAR package. Describes a JAR package including all
 * options to regenerate the JAR.
 */
public class JarPackage implements Serializable {

	// Constants
	public static final String EXTENSION= "jar"; //$NON-NLS-1$
	public static final String DESCRIPTION_EXTENSION= "jardesc"; //$NON-NLS-1$

	private String	fManifestVersion;
	private transient boolean fIsUsedToInitialize;
	/*
	 * What to export - internal locations
	 * The list fExportedX is null if fExportX is false)
	 */	
	private boolean fExportClassFiles;		// export generated class files and resources
	private boolean	fExportJavaFiles;		// export java files and resources
	/*	 * List which contains all the the elements (no containers) to export	 */	private List	fSelectedElements;		// internal locations	/*	 * Closure of elements and containers selected for export	 */	private transient Set fSelectedElementsClosure;

	private IPath	fJarLocation;			// external location

	private boolean fOverwrite;
	private boolean fCompress;
	
	private boolean	fSaveDescription;
	private IPath	fDescriptionLocation; // internal location
	/*
	 * A normal JAR has a manifest (fUsesManifest is true)
	 * The manifest can be contributed in two ways
	 * - it can be generated (fGenerateManifest is true) and
	 *		- saved (fSaveManifest is true)
	 *		- saved and reused (fReuseManifest is true implies: fSaveManifest is true)
	 * - it can be specified (fGenerateManifest is false) and the
	 *		manifest location must be specified (fManifestLocation)
	 */
	private boolean fUsesManifest;
	private boolean fSaveManifest;
	private boolean fReuseManifest;	
	private boolean fGenerateManifest;
	private IPath fManifestLocation; // internal location
	/*
	 * Sealing: a JAR can be
	 * - sealed (fSealJar is true) and a list of 
	 *		unsealed packages can be defined (fPackagesToUnseal)
	 *		while the list of sealed packages is ignored
	 * - unsealed (fSealJar is false) and the list of
	 *		sealed packages can be defined (fPackagesToSeal)
	 *		while the list of unsealed packages is ignored
	 */
	private boolean fSealJar;
	private IPackageFragment[] fPackagesToSeal;
	private IPackageFragment[] fPackagesToUnseal;

 	private IType fMainClass;
	
	private String fDownloadExtensionsPath;	
	/*	 * Error handling	 */	 private boolean fExportErrors;	 private boolean fExportWarnings;	 private boolean fLogErrors;	 private boolean fLogWarnings;
	public JarPackage() {		setIsUsedToInitialize(false);
		setExportClassFiles(true);
		setCompress(true);
		setSaveDescription(false);
		setJarLocation(new Path("")); //$NON-NLS-1$
		setDescriptionLocation(new Path("")); //$NON-NLS-1$
		setUsesManifest(true);
		setGenerateManifest(true);
		setReuseManifest(false);
		setSaveManifest(false);
		setManifestLocation(new Path("")); //$NON-NLS-1$
		setDownloadExtensionsPath(""); //$NON-NLS-1$		setExportErrors(true);		setExportWarnings(true);		
		setLogErrors(true);		setLogWarnings(true);			}
	// ----------- Accessors -----------
		/**	 * Tells whether the JAR is compressed or not.	 * 	 * @return	<code>true</code> if the JAR is compressed	 */
	public boolean isCompressed() {
		return fCompress;
	}
	/**	 * Sets whether the JAR is compressed or not.	 * 	 * @param state	a boolean indicating the new state	 */	public void setCompress(boolean state) {
		fCompress= state;
	}
	/**	 * Tells whether files can be overwritten without warning.	 * 	 * @return	<code>true</code> if files can be overwritten without warning	 */	public boolean allowOverwrite() {
		return fOverwrite;
	}
	/**	 * Sets whether files can be overwritten without warning.	 * 	 * @param state	a boolean indicating the new state	 */	public void setOverwrite(boolean state) {
		fOverwrite= state;
	}
	/**	 * Tells whether class files and resources are exported.	 * 	 * @return	<code>true</code> if class files and resources are exported	 */	public boolean areClassFilesExported() {
		return fExportClassFiles;
	}
	/**	 * Sets option to export class files and resources.	 * 	 * @param state	a boolean indicating the new state	 */	public void setExportClassFiles(boolean state) {
		fExportClassFiles= state;
	}

	public boolean areJavaFilesExported() {
		return fExportJavaFiles;
	}
	/**	 * Sets the option to export Java source and resources	 * 	 * @param state the new state	 */
	public void setExportJavaFiles(boolean state) {
		fExportJavaFiles= state;
	}
	/**
	 * Gets the jarLocation
	 * @return Returns a IPath
	 */
	public IPath getJarLocation() {
		return fJarLocation;
	}
	/**
	 * Sets the jarLocation
	 * @param jarLocation The jarLocation to set
	 */
	public void setJarLocation(IPath jarLocation) {
		fJarLocation= jarLocation;
	}
	/**	 * Tells whether clients should read their init values from this instance.	 * 	 * @return	<code>true</code> if clients should read their init values from this instance	 */	public boolean isUsedToInitialize() {
		return fIsUsedToInitialize;
	}
	/**
	 * Sets if this JAR package is used to initialize the client
	 * @param isUsedToInitialize <code>ture</code> if this JAR package is used to initialize the client
	 */
	public void setIsUsedToInitialize(boolean isUsedToInitialize) {
		fIsUsedToInitialize= isUsedToInitialize;
	}
	/**	 * Tells whether this JAR package should be saved or not.	 * 	 * @return	<code>true</code> if this JAR package will be saved	 */	public boolean isDescriptionSaved() {
		return fSaveDescription;
	}
	/**
	 * Sets the saveDescription
	 * @param saveDescription The saveDescription to set
	 */
	public void setSaveDescription(boolean saveDescription) {
		fSaveDescription= saveDescription;
	}
	/**	 * Tells whether the manifest should be saved or not.	 * 	 * @return	<code>true</code> if the manifest will be saved	 */	public boolean isManifestSaved() {
		return fSaveManifest;
	}
	/**
	 * Sets the saveManifest
	 * @param saveManifest The saveManifest to set
	 */
	public void setSaveManifest(boolean saveManifest) {
		fSaveManifest= saveManifest;
		if (!fSaveManifest)
			setReuseManifest(false);
	}
	/**	 * Tells whether the manifest should be reused or not.	 * The manifest will be reused when this JAR package is regenerated.	 * 	 * @return	<code>true</code> if the manifest is reused	 */	public boolean isManifestReused() {
		return fReuseManifest;
	}
	/**
	 * Sets the reuseManifest
	 * @param reuseManifest The reuseManifest to set
	 */
	public void setReuseManifest(boolean reuseManifest) {
		fReuseManifest= reuseManifest;
		if (fReuseManifest)
			// manifest must be saved in order to be reused
			setSaveManifest(true);
	}
	/**
	 * Gets the manifestLocation
	 * @return Returns a IPath
	 */
	public IPath getManifestLocation() {
		return fManifestLocation;
	}
	/**
	 * Sets the manifestLocation
	 * @param manifestLocation The manifestLocation to set
	 */
	public void setManifestLocation(IPath manifestLocation) {
		fManifestLocation= manifestLocation;
	}
	/**
	 * Gets the manifest file (as workspace resource)
	 * @return Returns a IFile
	 */
	public IFile getManifestFile() {
		IPath path= getManifestLocation();
		if (path.isValidPath(path.toString()) && path.segmentCount() >= 2)
			return JavaPlugin.getWorkspace().getRoot().getFile(path);
		else
			return null;
	}
	/**
	 * Answers whether the JAR itself is sealed
	 */
	public boolean isJarSealed() {
		return fSealJar;
	}
	/**
	 * Sets the sealJar
	 * @param sealJar The sealJar to set
	 */
	public void setSealJar(boolean sealJar) {
		fSealJar= sealJar;
	}
	/**
	 * Sets the packagesToSeal
	 * @param packagesToSeal The packagesToSeal to set
	 */
	public void setPackagesToSeal(IPackageFragment[] packagesToSeal) {
		fPackagesToSeal= packagesToSeal;
	}
	/**
	 * Gets the packagesToSeal
	 * @return Returns a IPackageFragment[]
	 */
	public IPackageFragment[] getPackagesToSeal() {
		if (fPackagesToSeal == null)
			return new IPackageFragment[0];
		else
			return fPackagesToSeal;
	}
	/**
	 * Gets the packagesToUnseal
	 * @return Returns a IPackageFragment[]
	 */
	public IPackageFragment[] getPackagesToUnseal() {
		if (fPackagesToUnseal == null)
			return new IPackageFragment[0];
		else
			return fPackagesToUnseal;
	}
	/**
	 * Sets the packagesToUnseal
	 * @param packagesToUnseal The packagesToUnseal to set
	 */
	public void setPackagesToUnseal(IPackageFragment[] packagesToUnseal) {
		fPackagesToUnseal= packagesToUnseal;
	}
	/**
	 * Gets the generateManifest
	 * @return Returns a boolean
	 */
	public boolean isManifestGenerated() {
		return fGenerateManifest;
	}
	/**
	 * Sets the generateManifest
	 * @param generateManifest The generateManifest to set
	 */
	public void setGenerateManifest(boolean generateManifest) {
		fGenerateManifest= generateManifest;
	}
	/**
	 * Gets the descriptionLocation
	 * @return Returns a IPath
	 */
	public IPath getDescriptionLocation() {
		return fDescriptionLocation;
	}
	/**
	 * Gets the description file (as workspace resource)
	 * @return Returns a IFile
	 */
	public IFile getDescriptionFile() {
		IPath path= getDescriptionLocation();
		if (path.isValidPath(path.toString()) && path.segmentCount() >= 2)
			return JavaPlugin.getWorkspace().getRoot().getFile(path);
		else
			return null;
	}	
	/**
	 * Sets the descriptionLocation
	 * @param descriptionLocation The descriptionLocation to set
	 */
	public void setDescriptionLocation(IPath descriptionLocation) {
		fDescriptionLocation= descriptionLocation;
	}
	/**
	 * Gets the mainClass
	 * @return Returns a IType
	 */
	public IType getMainClass() {
		return fMainClass;
	}
	/**
	 * Sets the mainClass
	 * @param mainClass The mainClass to set
	 */
	public void setMainClass(IType mainClass) {
		fMainClass= mainClass;
	}
	/**
	 * Gets the mainClassName
	 * @return Returns a String
	 */
	public String getMainClassName() {
		if (fMainClass == null)
			return ""; //$NON-NLS-1$
		else
			return fMainClass.getFullyQualifiedName();
	}
	/**
	 * Gets the downloadExtensionsPath
	 * @return Returns a String
	 */
	public String getDownloadExtensionsPath() {
		return fDownloadExtensionsPath;
	}
	/**
	 * Sets the downloadExtensionsPath
	 * @param downloadExtensionsPath The downloadExtensionsPath to set
	 */
	public void setDownloadExtensionsPath(String downloadExtensionsPath) {
		fDownloadExtensionsPath= downloadExtensionsPath;
	}
	/**
	 * Gets the selected elements
	 * @return a List with the selected elements
	 */
	public List getSelectedElements() {		if (fSelectedElements == null)			setSelectedElements(new ArrayList());
		return fSelectedElements;
	}
	/**
	 * Sets the selected elements
	 * @param selectedElements the list with the selected elements
	 */
	public void setSelectedElements(List selectedElements) {
		fSelectedElements= selectedElements;
	}	/**	 * Computes and returns the selected resources.	 * The underlying resource is used for Java elements.	 * 	 * @return a List with the selected resources	 */	public List getSelectedResources() {		if (fSelectedElements == null)			return null;		List selectedResources= new ArrayList(fSelectedElements.size());		Iterator iter= fSelectedElements.iterator();		while (iter.hasNext()) {			Object element= iter.next();			if (element instanceof IJavaElement) {				try {					selectedResources.add(((IJavaElement)element).getUnderlyingResource());				} catch (JavaModelException ex) {					// ignore the resource for now				}			}			else				selectedResources.add(element);		}		return selectedResources;	}	/**
	 * Gets the manifestVersion
	 * @return Returns a String
	 */
	public String getManifestVersion() {
		if (fManifestVersion == null)
			return "1.0"; //$NON-NLS-1$
		return fManifestVersion;
	}
	/**
	 * Sets the manifestVersion
	 * @param manifestVersion The manifestVersion to set
	 */
	public void setManifestVersion(String manifestVersion) {
		fManifestVersion= manifestVersion;
	}
	/**
	 * Answers if a manifest should be included in the JAR
	 * @return Returns a boolean
	 */
	public boolean usesManifest() {
		return fUsesManifest;
	}
	/**
	 * Sets the usesManifest
	 * @param usesManifest The usesManifest to set
	 */
	public void setUsesManifest(boolean usesManifest) {
		fUsesManifest= usesManifest;
	}
	/**
	 * Returns a JAR package spec reader.
	 * Subclasses may override to provide their own reader.
	 * 
	 * @param	inputStream the underlying stream
	 * @return	a JarPackage specification reader	 * @deprecated	As of 0.114, create the  reader outside this class - will be removed
	 */
	protected JarPackageReader getReader(InputStream inputStream) {
		return new JarPackageReader(inputStream);
	}
	/**
	 * Returns a JAR package spec writer.
	 * Subclasses may override to provide their own writer.
	 * 
	 * @param	outputStream the underlying stream
	 * @return	a JarPackage specification writer	 * @deprecated	As of 0.114, create the  reader outside this class - will be removed	 */
	protected JarPackageWriter getWriter(OutputStream outputStream) {
		return new JarPackageWriter(outputStream);
	}
	/**	 * Gets the logErrors	 * @return Returns a boolean	 */	public boolean logErrors() {		return fLogErrors;	}	/**	 * Sets the logErrors	 * @param logErrors The logErrors to set	 */	public void setLogErrors(boolean logErrors) {		fLogErrors= logErrors;	}	/**	 * Gets the logWarnings	 * @return Returns a boolean	 */	public boolean logWarnings() {		return fLogWarnings;	}	/**	 * Sets the logWarnings	 * @param logWarnings The logWarnings to set	 */	public void setLogWarnings(boolean logWarnings) {		fLogWarnings= logWarnings;	}	/**	 * Answers if files with errors are exported	 * @return Returns a boolean	 */	public boolean exportErrors() {		return fExportErrors;	}	/**	 * Sets the exportErrors	 * @param exportErrors The exportErrors to set	 */	public void setExportErrors(boolean exportErrors) {		fExportErrors= exportErrors;	}	/**	 * Answers if files with warnings are exported	 * @return Returns a boolean	 */	public boolean exportWarnings() {		return fExportWarnings;	}	/**	 * Sets the exportWarnings	 * @param exportWarnings The exportWarnings to set	 */	public void setExportWarnings(boolean exportWarnings) {		fExportWarnings= exportWarnings;	}
	// ----------- Utility methods -----------

	/**
	 * Tells whether this JAR specification can be used to generate
	 * a valid JAR
	 */
	public boolean isValid() {
		return (areClassFilesExported() || areJavaFilesExported())
			&& getSelectedElements() != null && getSelectedElements().size() > 0
			&& getJarLocation() != null
			&& doesManifestExist()
			&& isMainClassValid(new BusyIndicatorRunnableContext());
	}
	/**
	 * Tells whether the manifest exists.
	 * Returns <code>true</code> if the manifest is generated.
	 */
	public boolean doesManifestExist() {
		if (isManifestGenerated())
			return true;
		IFile file= getManifestFile();
		return file != null && file.isAccessible();
	}
	/**
	 * Tells whether the specified main class is valid.
	 * Returns <code>true</code> if no main class is specified.
	 */
	public boolean isMainClassValid(IRunnableContext context) {
		if (getMainClassName().length() == 0)
			return true;		try {				return JavaModelUtil.hasMainMethod(getMainClass());		} catch (JavaModelException e) {			JavaPlugin.log(e.getStatus());		}		return false;
	}
	/**
	 * Returns the minimal set of packages which contain all the selected Java resources.
	 * @return	the Set of IPackageFragments which contain all the selected resources
	 */
	public Set getPackagesForSelectedResources() {
		Set packages= new HashSet();
		for (Iterator iter= getSelectedElements().iterator(); iter.hasNext();) {			Object element= iter.next();			if (element instanceof ICompilationUnit) {				IJavaElement pack= JavaModelUtil.findParentOfKind((IJavaElement)element, org.eclipse.jdt.core.IJavaElement.PACKAGE_FRAGMENT);				if (pack != null)					packages.add(pack);			}		}		return packages;
	}	/**	 * Tells whether the given resource (or its children) have compile errors.	 * The method acts on the current build state and does not recompile.	 * 	 * @param resource the resource to check for errors	 * @return <code>true</code> if the resource (and its children) are error free	 * @throws import org.eclipse.core.runtime.CoreException if there's a marker problem	 */	public boolean hasCompileErrors(IResource resource) throws CoreException {		IMarker[] problemMarkers= resource.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);		for (int i= 0; i < problemMarkers.length; i++) {			if (problemMarkers[i].getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR)				return true;		}		return false;	}	/**	 * Tells whether the given resource (or its children) have compile errors.	 * The method acts on the current build state and does not recompile.	 * 	 * @param resource the resource to check for errors	 * @return <code>true</code> if the resource (and its children) are error free	 * @throws import org.eclipse.core.runtime.CoreException if there's a marker problem	 */	public boolean hasCompileWarnings(IResource resource) throws CoreException {		IMarker[] problemMarkers= resource.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);		for (int i= 0; i < problemMarkers.length; i++) {			if (problemMarkers[i].getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_WARNING)				return true;		}		return false;	}	/**
	 * Checks if the JAR file can be overwritten.
	 * If the JAR package setting does not allow to overwrite the JAR
	 * then a dialog will ask the user again.
	 * 
	 * @param	parent	the parent for the dialog,
	 * 			or <code>null</code> if no dialog should be presented
	 * @return	<code>true</code> if it is OK to create the JAR
	 */
	public boolean canCreateJar(Shell parent) {		File file= getJarLocation().toFile();
		if (getJarLocation().toFile().canWrite() && allowOverwrite())
			return true;		else if ((getJarLocation().toFile().canWrite() && !allowOverwrite()) && parent != null)
			return askForOverwritePermission(parent, getJarLocation().toOSString());					// Test if directory exists		String path= file.getAbsolutePath();		int separatorIndex = path.lastIndexOf(File.separator);		if (separatorIndex == -1) // ie.- default dir, which is fine			return true;		File directory= new File(path.substring(0, separatorIndex));		if (!directory.exists()) {			if (askToCreateDirectory(parent))				return directory.mkdirs();		}		return true;
	}
	/**	 * Checks if the description file can be overwritten.	 * If the JAR package setting does not allow to overwrite the description	 * then a dialog will ask the user again.	 * 	 * @param	parent	the parent for the dialog,	 * 			or <code>null</code> if no dialog should be presented	 * @return	<code>true</code> if it is OK to create the JAR	 */	public boolean canOverwriteDescription(Shell parent) {		if (getDescriptionFile().isAccessible() && allowOverwrite())			return true;		return askForOverwritePermission(parent, getDescriptionFile().getFullPath().toString());	}	/**	 * Checks if the manifest file can be overwritten.	 * If the JAR package setting does not allow to overwrite the manifest	 * then a dialog will ask the user again.	 * 	 * @param	parent	the parent for the dialog,	 * 			or <code>null</code> if no dialog should be presented	 * @return	<code>true</code> if it is OK to create the JAR	 */	public boolean canOverwriteManifest(Shell parent) {		if (getManifestFile().isAccessible() && allowOverwrite())			return true;		return askForOverwritePermission(parent, getManifestFile().getFullPath().toString());	}	/**
	 * Returns human readable form of this JarPackage
	 * @return a human readable representation of this JAR package
	 */
	public String toString() {
		ByteArrayOutputStream out= new ByteArrayOutputStream();
		JarPackageWriter writer= new JarPackageWriter(out);
		try {
			writer.writeString(this);
		} catch (IOException ex) {
			return ""; //$NON-NLS-1$
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException ex) {
				// do nothing
			}
		}
		return out.toString();
	}
	private boolean askToCreateDirectory(final Shell parent) {		return queryDialog(parent, JarPackagerMessages.getString("JarPackage.confirmCreate.title"), JarPackagerMessages.getString("JarPackage.confirmCreate.message")); //$NON-NLS-2$ //$NON-NLS-1$	}	private boolean askForOverwritePermission(final Shell parent, String filePath) {		if (parent == null)			return false;		return queryDialog(parent, JarPackagerMessages.getString("JarPackage.confirmReplace.title"), JarPackagerMessages.getFormattedString("JarPackage.confirmReplace.message", filePath)); //$NON-NLS-2$ //$NON-NLS-1$	}	private boolean queryDialog(final Shell parent, final String title, final String message) {
		
		class DialogReturnValue {
			boolean value;
		}
		
		Display display= parent.getDisplay();
		if (display == null || display.isDisposed())
			return false;
		final DialogReturnValue returnValue= new DialogReturnValue();
		Runnable runnable= new Runnable() {
			public void run() {
				returnValue.value= MessageDialog.openQuestion(parent, title, message);
			}
		};
		display.syncExec(runnable);	
		return returnValue.value;
	}
	/**	 * Sets the minimal list of selected containers and elements.	 * A selected element is only in the list, if its container is	 * NOT in the list and a container is only in the list if all its	 * elements have been selected for export.	 * 	 * Note:	 * - This method is only valid during the export operation and	 * only if the JAR description is saved. <code>null</code> is returned	 * in all other cases.	 * - Only one call to this method is allowed per export operation.	 * - The same list could be computed from the list of selected	 *	 elements. This is not done because most of the information	 *	 (e.g. containers of which all elements are exported) is already	 *	 available in the JarPackageWizardPage.	 */	void setSelectedElementsClosure(Set elements) {		fSelectedElementsClosure= elements;	}	/**	 * Returns the minimal list of selected containers and elements.	 * A selected element is only in the list, if its container is	 * NOT in the list and a container is only in the list if all its	 * elements have been selected for export.	 * 	 * Note:	 * - This method is only valid during the export operation and	 * only if the JAR description is saved. <code>null</code> is returned	 * in all other cases.	 * - Only one call to this method is allowed per export operation.	 * - The same list could be computed from the list of selected	 *	 elements. This is not done because most of the information	 *	 (e.g. containers of which all elements are exported) is already	 *	 available in the JarPackageWizardPage.	 * 	 */	Iterator getSelectedElementsClosure() {		if (fSelectedElementsClosure == null)			/*			 * XXX: Should compute the closure here to be more flexible,			 *		but currently the closure is only used by the writer			 *		when the closure is known.			 */			return null;		else {			Set tmp= fSelectedElementsClosure;			fSelectedElementsClosure= null;			return tmp.iterator();		}	}}