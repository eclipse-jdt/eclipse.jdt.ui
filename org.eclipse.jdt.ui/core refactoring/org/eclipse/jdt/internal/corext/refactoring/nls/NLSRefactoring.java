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
package org.eclipse.jdt.internal.corext.refactoring.nls;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStringStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;

public class NLSRefactoring extends Refactoring {

	public static final String BUNDLE_NAME= "BUNDLE_NAME"; //$NON-NLS-1$
	public static final String PROPERTY_FILE_EXT= ".properties"; //$NON-NLS-1$
	public static final String DEFAULT_ACCESSOR_CLASSNAME= "Messages"; //$NON-NLS-1$
	
	public static final String KEY= "${key}"; //$NON-NLS-1$
	public static final String DEFAULT_SUBST_PATTERN= "getString(" + KEY + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	
	private static final String DEFAULT_PROPERTY_FILENAME= "messages"; //$NON-NLS-1$

	//private IPath fPropertyFilePath;

	private String fAccessorClassName;
	private IPackageFragment fAccessorClassPackage;
	private String fResourceBundleName;
	private IPackageFragment fResourceBundlePackage;

	private String fSubstitutionPattern;
	private ICompilationUnit fCu;
	private NLSSubstitution[] fSubstitutions;
	
	private String fPrefix;

	private NLSRefactoring(ICompilationUnit cu) {
		Assert.isNotNull(cu);

		fCu= cu;
		
		String cuName= cu.getElementName();
		setPrefix(cuName.substring(0, cuName.length() - 4)); // A.java -> A. 

		CompilationUnit astRoot= JavaPlugin.getDefault().getASTProvider().getAST(cu, true, null);
		NLSHint nlsHint= new NLSHint(cu, astRoot);

		fSubstitutions= nlsHint.getSubstitutions();
		setAccessorClassName(nlsHint.getAccessorClassName());
		setAccessorClassPackage(nlsHint.getAccessorClassPackage());
		setResourceBundleName(nlsHint.getResourceBundleName());
		setResourceBundlePackage(nlsHint.getResourceBundlePackage());
		
		fSubstitutionPattern= getDefaultSubstitutionPattern();
	}

	public static NLSRefactoring create(ICompilationUnit cu) {
		if (!isAvailable(cu))
			return null;
		return new NLSRefactoring(cu);
	}

	public static boolean isAvailable(ICompilationUnit cu) {
		if (cu == null)
			return false;

		if (!cu.exists())
			return false;

		return true;
	}

	/**
	 * no validation is done
	 * 
	 * @param pattern
	 *            Example: "Messages.getString(${key})". Must not be
	 *            <code>null</code>. should (but does not have to) contain
	 *            NLSRefactoring.KEY (default value is $key$) only the first
	 *            occurrence of this key will be used
	 */
	public void setSubstitutionPattern(String pattern) {
		Assert.isNotNull(pattern);
		fSubstitutionPattern= pattern;
	}

	/**
	 * to show the pattern in the UI
	 */
	public String getSubstitutionPattern() {
		return fSubstitutionPattern;
	}

	public static String getDefaultSubstitutionPattern() {
		return DEFAULT_SUBST_PATTERN;
	}

	public ICompilationUnit getCu() {
		return fCu;
	}

	public String getName() {
		return NLSMessages.getFormattedString("NLSRefactoring.compilation_unit", fCu.getElementName());//$NON-NLS-1$
	}

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {

		if (fSubstitutions.length == 0) {
			String message= NLSMessages.getFormattedString("NLSRefactoring.no_strings", fCu.getElementName());//$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(message);
		}
		return new RefactoringStatus();
	}

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		checkParameters();
		try {

			pm.beginTask(NLSMessages.getString("NLSRefactoring.checking"), 5); //$NON-NLS-1$

			RefactoringStatus result= new RefactoringStatus();

			result.merge(checkIfAnythingToDo());
			if (result.hasFatalError()) {
				return result;
			}
			pm.worked(1);

			result.merge(validateModifiesFiles());
			if (result.hasFatalError()) {
				return result;
			}
			pm.worked(1);
			if (pm.isCanceled())
				throw new OperationCanceledException();

			result.merge(checkSubstitutionPattern());
			pm.worked(1);

			if (pm.isCanceled())
				throw new OperationCanceledException();


			result.merge(checkKeys());
			pm.worked(1);
			if (pm.isCanceled())
				throw new OperationCanceledException();

			if (!propertyFileExists() && willModifyPropertyFile()) {
				String msg= NLSMessages.getFormattedString("NLSRefactoring.will_be_created", getPropertyFilePath().toString()); //$NON-NLS-1$
				result.addInfo(msg);
			}
			pm.worked(1);

			return result;
		} finally {
			pm.done();
		}
	}

	public Change createChange(IProgressMonitor pm) throws CoreException {
		try {
			checkParameters();

			pm.beginTask("", 3); //$NON-NLS-1$

			final DynamicValidationStateChange result= new DynamicValidationStateChange("NLS Refactoring"); //$NON-NLS-1$

			if (willCreateAccessorClass()) {
				result.add(AccessorClass.create(fCu, fAccessorClassName, getAccessorCUPath(), fAccessorClassPackage, getPropertyFilePath(), new SubProgressMonitor(pm, 1)));
			}
			pm.worked(1);

			if (willModifySource()) {
				result.add(NLSSourceModifier.create(getCu(), fSubstitutions, fSubstitutionPattern, fAccessorClassPackage, fAccessorClassName));
			}
			pm.worked(1);

			if (willModifyPropertyFile()) {
				result.add(NLSPropertyFileModifier.create(fSubstitutions, getPropertyFilePath()));
			}
			pm.worked(1);

			return result;
		} finally {
			pm.done();
		}
	}

	private void checkParameters() {
		Assert.isNotNull(fSubstitutions);
		Assert.isNotNull(fAccessorClassPackage);

		// these values have defaults ...
		Assert.isNotNull(fAccessorClassName);
		Assert.isNotNull(fSubstitutionPattern);
	}

	private IFile[] getAllFilesToModify() {

		List files= new ArrayList(2);
		if (willModifySource()) {
			IResource resource= fCu.getResource();
			if (resource.exists()) {
				files.add(resource);
			}
		}

		if (willModifyPropertyFile()) {
			IFile file= getPropertyFileHandle();
			if (file.exists()) {
				files.add(file);
			}
		}

		return (IFile[]) files.toArray(new IFile[files.size()]);
	}

	public IFile getPropertyFileHandle() {
		return ResourcesPlugin.getWorkspace().getRoot().getFile(getPropertyFilePath());
	}

	public IPath getPropertyFilePath() {
		return fResourceBundlePackage.getPath().append(fResourceBundleName);
	}

	private RefactoringStatus validateModifiesFiles() {
		return Checks.validateModifiesFiles(getAllFilesToModify(), getValidationContext());
	}

	//should stop checking if fatal error
	private RefactoringStatus checkIfAnythingToDo() throws JavaModelException {
		if (willCreateAccessorClass())
			return null;

		if (willModifyPropertyFile())
			return null;

		if (willModifySource())
			return null;

		RefactoringStatus result= new RefactoringStatus();
		result.addFatalError(NLSMessages.getString("NLSRefactoring.nothing_to_do")); //$NON-NLS-1$
		return result;
	}

	private boolean propertyFileExists() {
		return getPropertyFileHandle().exists();
	}

	private RefactoringStatus checkSubstitutionPattern() {
		String pattern= getSubstitutionPattern();

		RefactoringStatus result= new RefactoringStatus();
		if (pattern.trim().length() == 0) {//$NON-NLS-1$ 
			result.addError(NLSMessages.getString("NLSRefactoring.pattern_empty")); //$NON-NLS-1$
		}

		if (pattern.indexOf(KEY) == -1) {
			String msg= NLSMessages.getFormattedString("NLSRefactoring.pattern_does_not_contain", KEY); //$NON-NLS-1$
			result.addWarning(msg);
		}

		if (pattern.indexOf(KEY) != pattern.lastIndexOf(KEY)) {
			String msg= NLSMessages.getFormattedString("NLSRefactoring.Only_the_first_occurrence_of", KEY);//$NON-NLS-1$
			result.addWarning(msg);
		}

		return result;
	}

	private RefactoringStatus checkKeys() {
		RefactoringStatus result= new RefactoringStatus();
		NLSSubstitution[] subs= fSubstitutions;
		for (int i= 0; i < subs.length; i++) {
			NLSSubstitution substitution= subs[i];
			if ((substitution.getState() == NLSSubstitution.EXTERNALIZED) && substitution.hasStateChanged()) {
				result.merge(checkKey(substitution.getKey()));
			}
		}
		return result;
	}

	private static RefactoringStatus checkKey(String key) {
		RefactoringStatus result= new RefactoringStatus();

		if (key == null)
			result.addFatalError(NLSMessages.getString("NLSRefactoring.null")); //$NON-NLS-1$

		if (key.startsWith("!") || key.startsWith("#")) { //$NON-NLS-1$ //$NON-NLS-2$
			RefactoringStatusContext context= new JavaStringStatusContext(key, new SourceRange(0, 0));
			result.addWarning(NLSMessages.getString("NLSRefactoring.warning"), context); //$NON-NLS-1$
		}

		if ("".equals(key.trim())) //$NON-NLS-1$
			result.addFatalError(NLSMessages.getString("NLSRefactoring.empty")); //$NON-NLS-1$

		final String[] UNWANTED_STRINGS= {" ", ":", "\"", "\\", "'", "?", "="}; //$NON-NLS-7$ //$NON-NLS-6$ //$NON-NLS-5$ //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
		//feature in resource bundle - does not work properly if keys have ":"
		for (int i= 0; i < UNWANTED_STRINGS.length; i++) {
			if (key.indexOf(UNWANTED_STRINGS[i]) != -1) {
				String[] args= {key, UNWANTED_STRINGS[i]};
				String msg= NLSMessages.getFormattedString("NLSRefactoring.should_not_contain", args); //$NON-NLS-1$
				result.addError(msg);
			}
		}
		return result;
	}

	private boolean willCreateAccessorClass() throws JavaModelException {

		NLSSubstitution[] subs= fSubstitutions;
		if (NLSSubstitution.countItems(subs, NLSSubstitution.EXTERNALIZED) == 0) {
			return false;
		}

		ICompilationUnit compilationUnit= getAccessorCu();
		if (compilationUnit.exists()) {
			return false;
		}

		if (typeNameExistsInPackage(fAccessorClassPackage, fAccessorClassName)) {
			return false;
		}

		return (!Checks.resourceExists(getAccessorCUPath()));
	}

	private ICompilationUnit getAccessorCu() {
		return fAccessorClassPackage.getCompilationUnit(getAccessorCUName());
	}

	private boolean willModifySource() {
		NLSSubstitution[] subs= fSubstitutions;
		for (int i= 0; i < subs.length; i++) {
			if (subs[i].hasSourceChange())
				return true;
		}
		return false;
	}

	private boolean willModifyPropertyFile() {
		NLSSubstitution[] subs= fSubstitutions;
		for (int i= 0; i < subs.length; i++) {
			NLSSubstitution substitution= subs[i];
			if (substitution.hasPropertyFileChange()) {
				return true;
			}
		}
		return false;
	}

	private boolean typeNameExistsInPackage(IPackageFragment pack, String name) throws JavaModelException {
		return Checks.findTypeInPackage(pack, name) != null;
	}

	private String getAccessorCUName() {
		return fAccessorClassName + ".java"; //$NON-NLS-1$
	}

	private IPath getAccessorCUPath() {
		IPath res= fAccessorClassPackage.getPath().append(getAccessorCUName());
		return res;
	}



	public NLSSubstitution[] getSubstitutions() {
		return fSubstitutions;
	}

	public String getPrefix() {
		return fPrefix;
	}
	
	public void setPrefix(String prefix) {
		fPrefix = prefix;
		NLSSubstitution.setPrefix(prefix);
	}


	public static String getDefaultPropertiesFilename() {
		return DEFAULT_PROPERTY_FILENAME + PROPERTY_FILE_EXT;
	}
	
	
	public void setAccessorClassName(String name) {
		Assert.isNotNull(name);
		fAccessorClassName= name;
	}

	public void setAccessorClassPackage(IPackageFragment packageFragment) {
		Assert.isNotNull(packageFragment);
		fAccessorClassPackage= packageFragment;
	}

	public void setResourceBundlePackage(IPackageFragment resourceBundlePackage) {
		Assert.isNotNull(resourceBundlePackage);
		fResourceBundlePackage= resourceBundlePackage;
	}

	public void setResourceBundleName(String resourceBundleName) {
		Assert.isNotNull(resourceBundleName);
		fResourceBundleName= resourceBundleName;
	}

	public IPackageFragment getAccessorClassPackage() {
		return fAccessorClassPackage;
	}

	public IPackageFragment getResourceBundlePackage() {
		return fResourceBundlePackage;
	}

	public String getAccessorClassName() {
		return fAccessorClassName;
	}

	public String getResourceBundleName() {
		return fResourceBundleName;
	}
}