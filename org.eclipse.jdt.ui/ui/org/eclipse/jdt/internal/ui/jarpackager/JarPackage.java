package org.eclipse.jdt.internal.ui.jarpackager;/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
import java.io.ByteArrayOutputStream;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.util.MainMethodSearchEngine;

/**
 * Model for a JAR package. Describes a JAR package including all
 * options to regenerate the JAR.
 */
public class JarPackage implements java.io.Serializable {

	// Constants
	public static final String EXTENSION= "jar";
	public static final String DESCRIPTION_EXTENSION= "jardesc";

	private String	fManifestVersion;
	private boolean fInitializeFromDialog;

	/*
	 * What to export - internal locations
	 * The list fExportedX is null if fExportX is false)
	 */	
	private boolean fExportClassFiles;				// export generated class files and resources
	private boolean	fExportJavaFiles;				// export java files and resources
transient 	private List	fSelectedResources;		// internal locations

transient 	private IPath	fJarLocation;			// external location

	private boolean fOverwrite;
	private boolean fCompress;
	
	private boolean	fSaveDescription;
transient 	private IPath	fDescriptionLocation; // internal location
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
transient private IPath fManifestLocation; // internal location
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
transient 	private IPackageFragment[] fPackagesToSeal;
transient 	private IPackageFragment[] fPackagesToUnseal;

transient 	private IType fMainClass;
	
	private String fDownloadExtensionsPath;	

	public JarPackage() {
		setInitializeFromDialog(true);
		setExportClassFiles(true);
		setCompress(true);
		setSaveDescription(false);
		setJarLocation(new Path(""));
		setDescriptionLocation(new Path(""));
		setUsesManifest(true);
		setGenerateManifest(true);
		setReuseManifest(false);
		setSaveManifest(false);
		setManifestLocation(new Path(""));
		setDownloadExtensionsPath("");
	}

	/**
	 * Reads the JAR package spec from file.
	 */
	public static JarPackage readJarPackage(IFile description) {
		org.eclipse.jdt.internal.ui.util.JdtHackFinder.fixme("Move to caller and use JarPackageReader");
		Assert.isLegal(description.isAccessible());
		Assert.isNotNull(description.getFileExtension());
		Assert.isLegal(description.getFileExtension().equals(DESCRIPTION_EXTENSION));
		JarPackageReader objectInput= null;
		try {
			objectInput= new JarPackageReader(description.getContents());
			return (JarPackage)objectInput.readObject();
		} catch (CoreException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		} finally {
			try {
				if (objectInput != null)
					objectInput.close();
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return null;
	}

	// ----------- Accessors -----------
	
	public boolean isCompressed() {
		return fCompress;
	}

	public void setCompress(boolean state) {
		fCompress= state;
	}

	public boolean allowOverwrite() {
		return fOverwrite;
	}

	public void setOverwrite(boolean state) {
		fOverwrite= state;
	}

	public boolean areClassFilesExported() {
		return fExportClassFiles;
	}

	public void setExportClassFiles(boolean state) {
		fExportClassFiles= state;
	}

	public boolean areJavaFilesExported() {
		return fExportJavaFiles;
	}

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

	public boolean isInitializedFromDialog() {
		return fInitializeFromDialog;
	}
	/**
	 * Sets the initializeFromDialog
	 * @param initializeFromDialog The initializeFromDialog to set
	 */
	public void setInitializeFromDialog(boolean initializeFromDialog) {
		fInitializeFromDialog= initializeFromDialog;
	}

	public boolean isDescriptionSaved() {
		return fSaveDescription;
	}
	/**
	 * Sets the saveDescription
	 * @param saveDescription The saveDescription to set
	 */
	public void setSaveDescription(boolean saveDescription) {
		fSaveDescription= saveDescription;
	}

	public boolean isManifestSaved() {
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

	public boolean isManifestReused() {
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
			return "";
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
	 * Gets the selectedResources
	 * @return Returns a List
	 */
	public List getSelectedResources() {
		return fSelectedResources;
	}
	/**
	 * Sets the selectedResources
	 * @param selectedResources The selectedResources to set
	 */
	public void setSelectedResources(List selectedResources) {
		fSelectedResources= selectedResources;
	}
	/**
	 * Gets the manifestVersion
	 * @return Returns a String
	 */
	public String getManifestVersion() {
		if (fManifestVersion == null)
			return "1.0";
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
	 * @return	a JarPackage specification reader
	 */
	protected JarPackageReader getReader(InputStream inputStream) {
		return new JarPackageReader(inputStream);
	}
	/**
	 * Returns a JAR package spec writer.
	 * Subclasses may override to provide their own writer.
	 * 
	 * @param	outputStream the underlying stream
	 * @return	a JarPackage specification writer
	 */
	protected JarPackageWriter getWriter(OutputStream outputStream) {
		return new JarPackageWriter(outputStream);
	}

	// ----------- Utility methods -----------

	/**
	 * Tells whether this JAR specification can be used to generate
	 * a valid JAR
	 */
	public boolean isValid() {
		return (areClassFilesExported() || areJavaFilesExported())
			&& getSelectedResources() != null && getSelectedResources().size() > 0
			&& getJarLocation() != null
			&& doesManifestExist()
			&& getDescriptionLocation().getFileExtension() != null
			&& getDescriptionLocation().getFileExtension().equals(DESCRIPTION_EXTENSION)
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
		return file != null && file.exists();
	}
	/**
	 * Tells whether the specified main class is valid.
	 * Returns <code>true</code> if no main class is specified.
	 */
	public boolean isMainClassValid(IRunnableContext context) {
		if (getMainClassName().length() == 0)
			return true;
		
		// test if main class is in available within the given scope
		List resources= getSelectedResources();
		IJavaSearchScope searchScope= MainMethodSearchEngine.createJavaSearchScope((IResource[])resources.toArray(new IResource[resources.size()]));
		List mainMethods= new MainMethodSearchEngine().searchMethod(context, searchScope, 0);
		return mainMethods.contains(getMainClass());
	}
	/**
	 * Returns the minimal set of packages which contain all the selected resources.
	 * @return	the Set of IPackageFragments which contain all the selected resources
	 */
	public Set getPackagesForSelectedResources() {
		Set packages= new HashSet();
		for (Iterator iter= getSelectedResources().iterator(); iter.hasNext();) {
			IResource resource= (IResource)iter.next();
			boolean isJavaProject;
			try {
				isJavaProject= resource.getProject().hasNature(JavaCore.NATURE_ID);
			} catch (CoreException ex) {
				isJavaProject= false;
			}
			if (isJavaProject) {
				IJavaProject jProject= JavaCore.create(resource.getProject());
				try {
					IPackageFragment pack= jProject.findPackageFragment(resource.getFullPath().removeLastSegments(1));
					if (pack != null)
						packages.add(pack);
				} catch (JavaModelException ex) {
					// don't add the package
				}
			}
		}
		return packages;
	}
	/**
	 * Checks if the JAR file can be overwritten.
	 * If the JAR package setting does not allow to overwrite the JAR
	 * then a dialog will ask the user again.
	 * 
	 * @param	parent	the parent for the dialog,
	 * 			or <code>null</code> if no dialog should be presented
	 * @return	<code>true</code> if it is OK to create the JAR
	 */
	public boolean canCreateJar(Shell parent) {
		if (allowOverwrite() || !getJarLocation().toFile().canWrite())
			return true;
		else if (parent != null)
			return askForOverwritePermission(parent);
		return false;
	}
	/**
	 * Returns human readable form of this JarPackage
	 * @return a human readable representation of this JAR package
	 */
	public String toString() {
		ByteArrayOutputStream out= new ByteArrayOutputStream();
		JarPackageWriter writer= getWriter(out);
		try {
			writer.writeString(this);
		}
		catch (IOException ex) {
			return "";
		}
		finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException ex) {
				// do nothing
			}
		}
		return out.toString();
	}

	private boolean askForOverwritePermission(final Shell parent) {
		
		class DialogReturnValue {
			boolean value;
		}
		
		Display display= parent.getDisplay();
		if (display == null || display.isDisposed())
			return false;
		final DialogReturnValue returnValue= new DialogReturnValue();
		Runnable runnable= new Runnable() {
			public void run() {
				returnValue.value= MessageDialog.openQuestion(parent, "Confirm Replace", "The JAR '" + getJarLocation().toOSString() + "' already exists.\nDo you want to overwrite it?");
			}
		};
		display.syncExec(runnable);	
		return returnValue.value;
	}
}