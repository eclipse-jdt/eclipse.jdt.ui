/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.jarpackager;

import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.operation.IRunnableContext;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.jarpackager.JarFileExportOperation;
import org.eclipse.jdt.internal.ui.jarpackager.JarPackageReader;
import org.eclipse.jdt.internal.ui.jarpackager.JarPackageWriter;
import org.eclipse.jdt.internal.ui.jarpackager.JarPackagerUtil;
import org.eclipse.jdt.internal.ui.jarpackager.ManifestProvider;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;

/**
 * Model for a JAR package. Describes a JAR package including a bunch of
 * useful options to generate a JAR file with a JAR writer.
 * 
 * Clients may subclass.
 * 
 * @see org.eclipse.jdt.ui.jarpackager.JarWriter
 * @since 2.0
 */
public class JarPackageData {

	private String	fManifestVersion;

	/*
	 * What to export - internal locations
	 * The list fExported* is null if fExport* is false)
	 */	
	private boolean fExportClassFiles;	// export generated class files and resources
	private boolean fExportOutputFolders;	// export all output folder of enclosing projects
	private boolean fExportJavaFiles;		// export java files and resources

	/*
	 * Source folder hierarchy is created in the JAR if true
	 */
	private boolean fUseSourceFolderHierarchy;

	/*
	 * Projects of which files are expored will be built if true
	 * and autobuild is off.
	 */
	private boolean fBuildIfNeeded;

	/*
	 * Leaf elements (no containers) to export
	 */
	private Object[]	fElements; // inside workspace

	private IPath		fJarLocation; // external location
	private boolean	fOverwrite;
	private boolean	fCompress;
	
	private boolean	fSaveDescription;
	private IPath		fDescriptionLocation; // internal location

	/*
	 * A normal JAR has a manifest (fUsesManifest is true)
	 * The manifest can be contributed in two ways
	 * - it can be generated (fGenerateManifest is true) and
	 *		- saved (fSaveManifest is true)
	 *		- saved and reused (fReuseManifest is true implies: fSaveManifest is true)
	 * - it can be specified (fGenerateManifest is false and the
	 *		manifest location must be specified (fManifestLocation))
	 */
	private boolean	fUsesManifest;
	private boolean	fSaveManifest;
	private boolean	fReuseManifest;	
	private boolean	fGenerateManifest;
	private IPath		fManifestLocation; // internal location

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

 	private IType fManifestMainClass;
 	
 	private String fComment; // the JAR comment

	/*
	 * Error handling
	 */
	private boolean fExportErrors;
	private boolean fExportWarnings;
	
	// The provider for the manifest file
	private IManifestProvider fManifestProvider;
	
	public JarPackageData() {
		setExportClassFiles(true);
		setExportOutputFolders(false);
		setUseSourceFolderHierarchy(false);
		setCompress(true);
		setSaveDescription(false);
		setJarLocation(new Path("")); //$NON-NLS-1$
		setDescriptionLocation(new Path("")); //$NON-NLS-1$
		setUsesManifest(true);
		setGenerateManifest(true);
		setReuseManifest(false);
		setSaveManifest(false);
		setManifestLocation(new Path("")); //$NON-NLS-1$
		setExportErrors(true);
		setExportWarnings(true);		
		setBuildIfNeeded(true);
	}

	// ----------- Accessors -----------
	
	/**
	 * Tells whether the JAR is compressed or not.
	 * 
	 * @return	<code>true</code> if the JAR is compressed
	 */
	public boolean isCompressed() {
		return fCompress;
	}

	/**
	 * Set whether the JAR is compressed or not.
	 * 
	 * @param state a boolean indicating the new state
	 */
	public void setCompress(boolean state) {
		fCompress= state;
	}

	/**
	 * Tells whether files can be overwritten without warning.
	 * 
	 * @return	<code>true</code> if files can be overwritten without warning
	 */
	public boolean allowOverwrite() {
		return fOverwrite;
	}

	/**
	 * Set whether files can be overwritten without warning.
	 * 
	 * @param state a boolean indicating the new state
	 */
	public void setOverwrite(boolean state) {
		fOverwrite= state;
	}

	/**
	 * Tells whether class files and resources are exported.
	 * 
	 * @return	<code>true</code> if class files and resources are exported
	 */
	public boolean areClassFilesExported() {
		return fExportClassFiles;
	}

	/**
	 * Set option to export class files and resources.
	 * 
	 * @param state a boolean indicating the new state
	 */
	public void setExportClassFiles(boolean state) {
		fExportClassFiles= state;
	}
	
	/**
	 * Tells whether all output folders for the
	 * enclosing projects of the exported elements.
	 * 
	 * @return	<code>true</code> if output folder are exported
	 * @since 3.0
	 */
	public boolean areOutputFoldersExported() {
		return fExportOutputFolders;
	}

	/**
	 * Set option to export all output folders for the
	 * enclosing projects of the exported elements.
	 * 
	 * @param state a boolean indicating the new state
	 * @since 3.0
	 */
	public void setExportOutputFolders(boolean state) {
		fExportOutputFolders= state;
	}

	/**
	 * Tells whether files created by the Java builder are exported.
	 * 
	 * @return	<code>true</code> if output folder are exported
	 * @since 3.0
	 */
	public boolean areGeneratedFilesExported() {
		return fExportOutputFolders || fExportClassFiles;
	}

	/**
	 * Tells whether java files and resources are exported.
	 * 
	 * @return	<code>true</code> if java files and resources are exported
	 */
	public boolean areJavaFilesExported() {
		return fExportJavaFiles;
	}

	/**
	 * Set the option to export Java source and resources.
	 * 
	 * @param state the new state
	 */
	public void setExportJavaFiles(boolean state) {
		fExportJavaFiles= state;
	}

	/**
	 * Tells whether the source folder hierarchy is used.
	 * <p>
	 * Using the source folder hierarchy only makes sense if
	 * java files are but class files aren't exported.
	 * </p>
	 * 
	 * @return	<code>true</code> if source folder hierarchy is used
	 */
	public boolean useSourceFolderHierarchy() {
		return fUseSourceFolderHierarchy;
	}
	
	/**
	 * Set the option to export the source folder hierarchy.
	 * 
	 * @param state the new state
	 */
	public void setUseSourceFolderHierarchy(boolean state) {
		fUseSourceFolderHierarchy= state;
	}
	
	/**
	 * Gets the absolute location of the JAR file.
	 * This path is normally external to the workspace.
	 * 
	 * @return the absolute path representing the location of the JAR file
	 * 
	 * @since 3.0
	 */
	public IPath getAbsoluteJarLocation() {
		IPath workspaceLocation= ResourcesPlugin.getWorkspace().getRoot().getLocation();
		if (!fJarLocation.isAbsolute() && workspaceLocation != null)
			// prepend workspace path
			return workspaceLocation.append(fJarLocation);
		else
			return fJarLocation;
	}

	/**
	 * Gets the location of the JAR file.
	 * This path is normally external to the workspace.
	 * 
	 * @return the path representing the location of the JAR file
	 */
	public IPath getJarLocation() {
		return fJarLocation;
	}

	/**
	 * Set the JAR file location.
	 * 
	 * @param jarLocation a path denoting the location of the JAR file
	 */
	public void setJarLocation(IPath jarLocation) {
		fJarLocation= jarLocation;
	}

	/**
	 * Tells whether the manifest file must be generated.
	 * 
	 * @return <code>true</code> if the manifest has to be generated
	 */
	public boolean isManifestGenerated() {
		return fGenerateManifest;
	}
	
	/**
	 * Set whether a manifest must be generated or not.
	 * 
	 * @param state the new state
	 */
	public void setGenerateManifest(boolean state) {
		fGenerateManifest= state;
	}
	
	/**
	 * Tells whether the manifest file must be saved to the 
	 * specified file during the export operation.
	 * 
	 * @return	<code>true</code> if the manifest must be saved
	 * @see #getManifestLocation()
	 */
	public boolean isManifestSaved() {
		return fSaveManifest;
	}

	/**
	 * Set whether the manifest file must be saved during export
	 * operation or not.
	 * 
	 * @param state the new state
	 * @see #getManifestLocation()
	 */
	public void setSaveManifest(boolean state) {
		fSaveManifest= state;
		if (!fSaveManifest)
			// can't reuse manifest if it is not saved
			setReuseManifest(false);
	}

	/**
	 * Tells whether a previously generated manifest should be reused.
	 * 
	 * @return	<code>true</code> if the generated manifest will be reused when regenerating this JAR,
	 * 			<code>false</code> if the manifest has to be regenerated
	 */
	public boolean isManifestReused() {
		return fReuseManifest;
	}

	/**
	 * Set whether a previously generated manifest should be reused.
	 * 
	 * @param state the new state
	 */
	public void setReuseManifest(boolean state) {
		fReuseManifest= state;
		if (fReuseManifest)
			// manifest must be saved in order to be reused
			setSaveManifest(true);
	}

	/**
	 * Returns the location of a user-defined manifest file.
	 * 
	 * @return	the path of the user-defined manifest file location,
	 * 			or <code>null</code> if none is specified
	 */
	public IPath getManifestLocation() {
		return fManifestLocation;
	}

	/**
	 * Set the location of a user-defined manifest file.
	 * 
	 * @param manifestLocation the path of the user-define manifest location
	 */
	public void setManifestLocation(IPath manifestLocation) {
		fManifestLocation= manifestLocation;
	}

	/**
	 * Gets the manifest file (as workspace resource).
	 * 
	 * @return a file which points to the manifest
	 */
	public IFile getManifestFile() {
		IPath path= getManifestLocation();
		if (path.isValidPath(path.toString()) && path.segmentCount() >= 2)
			return JavaPlugin.getWorkspace().getRoot().getFile(path);
		else
			return null;
	}
	
	/**
	 * Gets the manifest version.
	 * 
	 * @return a string containing the manifest version
	 */
	public String getManifestVersion() {
		if (fManifestVersion == null)
			return "1.0"; //$NON-NLS-1$
		return fManifestVersion;
	}
	
	/**
	 * Set the manifest version.
	 * 
	 * @param manifestVersion the string which contains the manifest version
	 */
	public void setManifestVersion(String manifestVersion) {
		fManifestVersion= manifestVersion;
	}
	
	/**
	 * Answers whether a manifest must be included in the JAR.
	 * 
	 * @return <code>true</code> if a manifest has to be included
	 */
	public boolean usesManifest() {
		return fUsesManifest;
	}
	
	/**
	 * Set whether a manifest must be included in the JAR.
	 * 
	 * @param state the new state
	 */
	public void setUsesManifest(boolean state) {
		fUsesManifest= state;
	}
	
	/**
	 * Gets the manifest provider for this JAR package.
	 * 
	 * @return the IManifestProvider
	 */
	public IManifestProvider getManifestProvider() {
		if (fManifestProvider == null)
			fManifestProvider= new ManifestProvider();
		return fManifestProvider;
	}
	
	/**
	 * Set the manifest provider.
	 * 
	 * @param manifestProvider the ManifestProvider to set
	 */
	public void setManifestProvider(IManifestProvider manifestProvider) {
		fManifestProvider= manifestProvider;
	}
	
	/**
	 * Answers whether the JAR itself is sealed.
	 * The manifest will contain a "Sealed: true" statement.
	 * <p>
	 * This option should only be considered when the
	 * manifest file is generated.
	 * </p>
	 * 
	 * @return <code>true</code> if the JAR must be selead
	 * @see #isManifestGenerated()
	 */
	public boolean isJarSealed() {
		return fSealJar;
	}

	/**
	 * Set whether the JAR itself is sealed.
	 * The manifest will contain the following entry:
	 * Sealed: true
	 * <p>
	 * This option should only be considered when the
	 * manifest file is generated.
	 * </p>
	 * 
	 * @param sealJar <code>true</code> if the JAR must be selead
	 * @see #isManifestGenerated()
	 */
	public void setSealJar(boolean sealJar) {
		fSealJar= sealJar;
	}

	/**
	 * Set the packages which should be sealed.
	 * The following entry will be added to the manifest file for each package:
	 * Name: <name of the package>
	 * Sealed: true
	 * <p>
	 * This should only be used if the JAR itself is not sealed.
	 * </p>
	 * 
	 * @param packagesToSeal an array of <code>IPackageFragment</code> to seal
	 */
	public void setPackagesToSeal(IPackageFragment[] packagesToSeal) {
		fPackagesToSeal= packagesToSeal;
	}

	/**
	 * Gets the packages which should be sealed.
	 * The following entry will be added to the manifest file for each package:
	 * Name: <name of the package>
	 * Sealed: true
	 * <p>
	 * This should only be used if the JAR itself is not sealed.
	 * </p>
	 * 
	 * @return an array of <code>IPackageFragment</code>
	 */
	public IPackageFragment[] getPackagesToSeal() {
		if (fPackagesToSeal == null)
			return new IPackageFragment[0];
		else
			return fPackagesToSeal;
	}

	/**
	 * Gets the packages which should explicitly be unsealed.
	 * The following entry will be added to the manifest file for each package:
	 * Name: <name of the package>
	 * Sealed: false
	 * <p>
	 * This should only be used if the JAR itself is sealed.
	 * </p>
	 * 
	 * @return an array of <code>IPackageFragment</code>
	 */
	public IPackageFragment[] getPackagesToUnseal() {
		if (fPackagesToUnseal == null)
			return new IPackageFragment[0];
		else
			return fPackagesToUnseal;
	}

	/**
	 * Set the packages which should explicitly be unsealed.
	 * The following entry will be added to the manifest file for each package:
	 * Name: <name of the package>
	 * Sealed: false
	 * <p>
	 * This should only be used if the JAR itself is sealed.
	 * </p>
	 * 
	 * @param packagesToUnseal an array of <code>IPackageFragment</code>
	 */
	public void setPackagesToUnseal(IPackageFragment[] packagesToUnseal) {
		fPackagesToUnseal= packagesToUnseal;
	}

	/**
	 * Tells whether a description of this JAR package must be saved
	 * to a file by a JAR description writer during the export operation.
	 * <p>
	 * The JAR writer defines the format of the file.
	 * </p>
	 * 
	 * @return	<code>true</code> if this JAR package will be saved
	 * @see #getDescriptionLocation()
	 */
	public boolean isDescriptionSaved() {
		return fSaveDescription;
	}
	
	/**
	 * Set whether a description of this JAR package must be saved
	 * to a file by a JAR description writer during the export operation.
	 * <p>
	 * The format is defined by the client who implements the
	 * reader/writer pair.
	 * </p>
	 * @param state a boolean containing the new state
	 * @see #getDescriptionLocation()
	 * @see IJarDescriptionWriter
	 */
	public void setSaveDescription(boolean state) {
		fSaveDescription= state;
	}
	
	/**
	 * Returns the location of file containing the description of a JAR.
	 * This location is inside the workspace.
	 * 
	 * @return	the path of the description file location,
	 * 			or <code>null</code> if none is specified
	 */
	public IPath getDescriptionLocation() {
		return fDescriptionLocation;
	}

	/**
	 * Set the location of the JAR description file.
	 * 
	 * @param descriptionLocation the path of location
	 */
	public void setDescriptionLocation(IPath descriptionLocation) {
		fDescriptionLocation= descriptionLocation;
	}

	/**
	 * Gets the description file (as workspace resource).
	 * 
	 * @return a file which points to the description
	 */
	public IFile getDescriptionFile() {
		IPath path= getDescriptionLocation();
		if (path.isValidPath(path.toString()) && path.segmentCount() >= 2)
			return JavaPlugin.getWorkspace().getRoot().getFile(path);
		else
			return null;
	}	
	
	/**
	 * Gets the manifest's main class.
	 * 
	 * @return	the type which contains the main class or,
	 * 			<code>null</code> if none is specified
	 */
	public IType getManifestMainClass() {
		return fManifestMainClass;
	}

	/**
	 * Set the manifest's main class.
	 * 
	 * @param manifestMainClass the type with the main class for the manifest file
	 */
	public void setManifestMainClass(IType manifestMainClass) {
		fManifestMainClass= manifestMainClass;
	}

	/**
	 * Returns the elements which will be exported.
	 * These elements are leaf objects e.g. <code>IFile</code>
	 * and not containers.
	 * 
	 * @return an array of leaf objects
	 */
	public Object[] getElements() {
		if (fElements == null)
			setElements(new Object[0]);
		return fElements;
	}

	/**
	 * Set the elements which will be exported.
	 * 
	 * These elements are leaf objects e.g. <code>IFile</code>.
	 * and not containers.
	 * 
	 * @param elements	an array with leaf objects
	 */
	public void setElements(Object[] elements) {
		fElements= elements;
	}

	/**
	 * Returns the JAR's comment.
	 * 
	 * @return the comment string or <code>null</code>
	 * 			if the JAR does not contain a comment
	 */
	public String getComment() {
		return fComment;
	}

	/**
	 * Sets the JAR's comment.
	 * 
	 * @param comment	a string or <code>null</code>
	 * 					if the JAR does not contain a comment
	 */
	public void setComment(String comment) {
		fComment= comment;
	}

	/**
	 * Tell whether errors are logged.
	 * <p>
	 * The export operation decides where and
	 * how the errors are logged.
	 * </p>
	 * 
	 * @return <code>true</code> if errors are logged
	 * @deprecated will be removed in final 2.0
	 */
	public boolean logErrors() {
		return true;
	}

	/**
	 * Set whether errors are logged.
	 * <p>
	 * The export operation decides where and
	 * how the errors are logged.
	 * </p>
	 * 
	 * @param logErrors <code>true</code> if errors are logged
	 * @deprecated will be removed in final 2.0
	 */
	public void setLogErrors(boolean logErrors) {
		// always true
	}
		
	/**
	 * Tells whether warnings are logged or not.
	 * <p>
	 * The export operation decides where and
	 * how the warnings are logged.
	 * </p>
	 * 
	 * @return <code>true</code> if warnings are logged
	 * @deprecated will be removed in final 2.0
	 */
	public boolean logWarnings() {
		return true;
	}
	
	/**
	 * Set if warnings are logged.
	 * <p>
	 * The export operation decides where and
	 * how the warnings are logged.
	 * </p>
	 * 
	 * @param logWarnings <code>true</code> if warnings are logged
	 * @deprecated will be removed in final 2.0
	 */
	public void setLogWarnings(boolean logWarnings) {
		// always true
	}
	
	/**
	 * Answers if compilation units with errors are exported.
	 * 
	 * @return <code>true</code> if CUs with errors should be exported
	 */
	public boolean areErrorsExported() {
		return fExportErrors;
	}
	
	/**
	 * Set if compilation units with errors are exported.
	 * 
	 * @param exportErrors <code>true</code> if CUs with errors should be exported
	 */
	public void setExportErrors(boolean exportErrors) {
		fExportErrors= exportErrors;
	}
	
	/**
	 * Answers if compilation units with warnings are exported.
	 * 
	 * @return <code>true</code> if CUs with warnings should be exported
	 */
	public boolean exportWarnings() {
		return fExportWarnings;
	}
	
	/**
	 * Set if compilation units with warnings are exported.
	 * 
	 * @param exportWarnings <code>true</code> if CUs with warnings should be exported
	 */
	public void setExportWarnings(boolean exportWarnings) {
		fExportWarnings= exportWarnings;
	}
	
	/**
	 * Answers if a build should be performed before exporting files.
	 * This flag is only considered if auto-build is off.
	 * 
	 * @return a boolean telling if a build should be performed
	 */
	public boolean isBuildingIfNeeded() {
		return fBuildIfNeeded;
	}
	
	/**
	 * Set if a build should be performed before exporting files.
	 * This flag is only considered if auto-build is off.
	 * 
	 * @param buildIfNeeded a boolean telling if a build should be performed
	 */
	public void setBuildIfNeeded(boolean buildIfNeeded) {
		fBuildIfNeeded= buildIfNeeded;
	}
	// ----------- Utility methods -----------
	
	/**
	 * Finds the class files for the given java file 
	 * and returns them.
	 * <p>
	 * This is a hook for subclasses which want to implement
	 * a different strategy for finding the class files. The default
	 * strategy is to query the class files for the source file
	 * name attribute. If this attribute is missing then all class
	 * files in the corresponding output folder are exported.
	 * </p>
	 * <p>
	 * A CoreException can be thrown if an error occurs during this
	 * operation. The <code>CoreException</code> will not stop the export
	 * process but adds the status object to the status of the
	 * export runnable.
	 * </p>
	 * 
	 * @param	javaFile a .java file
	 * @return	an array with class files or <code>null</code> to used the default strategy
	 * @throws	CoreException	if find failed, e.g. I/O error or resource out of synch
	 * @see	IJarExportRunnable#getStatus()
	 */
	public IFile[] findClassfilesFor(IFile javaFile) throws CoreException {
		return null;
	}

	/**
	 * Creates and returns a JarWriter for this JAR package.
	 *
	 * @param parent	the shell used to display question dialogs,
	 *				 	or <code>null</code> if "false/no/cancel" is the answer
	 * 					and no dialog should be shown
	 * @return a JarWriter
	 * @throws CoreException if the {@link JarWriter} could not be created
	 * @see JarWriter
	 */
	public JarWriter createJarWriter(Shell parent) throws CoreException {
		return new JarWriter(this, parent);
	}
	
	/**
	 * Creates and returns a JarExportRunnable.
	 *
	 * @param	parent	the parent for the dialog,
	 * 			or <code>null</code> if no questions should be asked and
	 * 			no checks for unsaved files should be made.
	 * @return a JarExportRunnable
	 */
	public IJarExportRunnable createJarExportRunnable(Shell parent) {
		return new JarFileExportOperation(this, parent);
	}
	
	/**
	 * Creates and returns a JarExportRunnable for a list of JAR package
	 * data objects.
	 *
	 * @param	jarPackagesData	an array with JAR package data objects
	 * @param	parent			the parent for the dialog,
	 * 							or <code>null</code> if no dialog should be presented
	 * @return the {@link IJarExportRunnable}
	 */
	public IJarExportRunnable createJarExportRunnable(JarPackageData[] jarPackagesData, Shell parent) {
		return new JarFileExportOperation(jarPackagesData, parent);
	}
	
	/**
	 * Creates and returns a JAR package data description writer
	 * for this JAR package data object.
	 * <p>
     * It is the client's responsibility to close this writer.
	 * </p>
	 * @param outputStream	the output stream to write to
	 * @return a JarWriter
	 */
	public IJarDescriptionWriter createJarDescriptionWriter(OutputStream outputStream) {
		return new JarPackageWriter(outputStream);
	}
	
	/**
	 * Creates and returns a JAR package data description reader
	 * for this JAR package data object.
	 * <p>
     * It is the client's responsibility to close this reader.
	 * </p>
	 * @param inputStream	the input stream to read from
	 * @return a JarWriter
	 */
	public IJarDescriptionReader createJarDescriptionReader(InputStream inputStream) {
		return new JarPackageReader(inputStream);
	}
	
	/**
	 * Tells whether this JAR package data can be used to generate
	 * a valid JAR.
	 * 
	 * @return <code>true</code> if the JAR Package info is valid
	 */
	public boolean isValid() {
		return (areGeneratedFilesExported() || areJavaFilesExported())
			&& getElements() != null && getElements().length > 0
			&& getAbsoluteJarLocation() != null
			&& isManifestAccessible()
			&& isMainClassValid(new BusyIndicatorRunnableContext());
	}

	/**
	 * Tells whether a manifest is available.
	 * 
	 * @return <code>true</code> if the manifest is generated or the provided one is accessible
	 */
	public boolean isManifestAccessible() {
		if (isManifestGenerated())
			return true;
		IFile file= getManifestFile();
		return file != null && file.isAccessible();
	}

	/**
	 * Tells whether the specified manifest main class is valid.
	 * 
	 * @param context the {@link IRunnableContext}
	 * @return <code>true</code> if a main class is specified and valid
	 */
	public boolean isMainClassValid(IRunnableContext context) {
		return JarPackagerUtil.isMainClassValid(this, context);
	}
}