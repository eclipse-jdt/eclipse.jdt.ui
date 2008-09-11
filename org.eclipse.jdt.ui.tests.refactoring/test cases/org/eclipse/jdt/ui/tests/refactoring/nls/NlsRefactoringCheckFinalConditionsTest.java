/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.nls;

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSMessages;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSSubstitution;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.viewsupport.BasicElementLabels;

public class NlsRefactoringCheckFinalConditionsTest extends TestCase {

	//private IPath fPropertyFilePath;
	private IPackageFragment fAccessorPackage;
	private String fAccessorClassName;
	private String fSubstitutionPattern;
	private NlsRefactoringTestHelper fHelper;
	private IJavaProject javaProject;
	private IPackageFragment fResourceBundlePackage;
	private String fResourceBundleName;

	public NlsRefactoringCheckFinalConditionsTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(NlsRefactoringCheckFinalConditionsTest.class));
	}

	public static Test suite() {
		return allTests();
	}

	protected void setUp() throws Exception {
		javaProject= ProjectTestSetup.getProject();
		fHelper= new NlsRefactoringTestHelper(javaProject);
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(javaProject, ProjectTestSetup.getDefaultClasspath());
	}

	public void testCheckInputWithoutExistingPropertiesFile() throws Exception {
		ICompilationUnit cu= fHelper.getCu("/TestSetupProject/src1/p/WithStrings.java");
		IFile propertyFile= fHelper.getFile("/TestSetupProject/src2/p/test.properties");
		propertyFile.delete(false, fHelper.fNpm);
		initDefaultValues();

		RefactoringStatus res= createCheckInputStatus(cu);

		assertFalse("should info about properties", res.isOK());

		assertEquals("one info", 1, res.getEntries().length);
		RefactoringStatusEntry help= res.getEntryAt(0);
		assertEquals("info", RefactoringStatus.INFO, help.getSeverity());
		assertEquals(Messages.format(NLSMessages.NLSRefactoring_will_be_created, BasicElementLabels.getPathLabel(propertyFile.getFullPath(), false)), help.getMessage());
	}

	/*
	 * no substitutions -> nothing to do
	 */
	public void testCheckInputWithNoSubstitutions() throws Exception {
		ICompilationUnit cu= fHelper.getCu("/TestSetupProject/src1/p/WithoutStrings.java"); //$NON-NLS-1$
		initDefaultValues();

		checkNothingToDo(createCheckInputStatus(cu));
	}

	/*
	 * substitution checks
	 */
	public void testCheckInputWithSubstitutionPatterns() throws Exception {
		ICompilationUnit cu= fHelper.getCu("/TestSetupProject/src1/p/WithStrings.java"); //$NON-NLS-1$
		initDefaultValues();

		fSubstitutionPattern= ""; //$NON-NLS-1$

		RefactoringStatus res= createCheckInputStatus(cu);

		RefactoringStatusEntry[] results= res.getEntries();

		assertEquals("substitution pattern must be given", 2, results.length); //$NON-NLS-1$
		assertEquals("first is fatal", RefactoringStatus.ERROR, results[0].getSeverity()); //$NON-NLS-1$
		assertEquals("right fatal message", //$NON-NLS-1$
				NLSMessages.NLSRefactoring_pattern_empty,
				results[0].getMessage());

		assertEquals("warning no key given", RefactoringStatus.WARNING, //$NON-NLS-1$
				results[1].getSeverity());
		assertEquals("right warning message", //$NON-NLS-1$
				Messages.format(NLSMessages.NLSRefactoring_pattern_does_not_contain,
						"${key}"), results[1].getMessage()); //$NON-NLS-1$

		fSubstitutionPattern= "blabla${key}"; //$NON-NLS-1$
		res= createCheckInputStatus(cu);
		assertTrue("substitution pattern ok", res.isOK()); //$NON-NLS-1$

		fSubstitutionPattern= "${key}blabla${key}"; //$NON-NLS-1$
		res= createCheckInputStatus(cu);
		assertFalse("substitution pattern ko", res.isOK()); //$NON-NLS-1$

		results= res.getEntries();
		assertEquals("one warning", 1, results.length); //$NON-NLS-1$
		assertEquals("warning", RefactoringStatus.WARNING, results[0].getSeverity()); //$NON-NLS-1$
		assertEquals("warning message", //$NON-NLS-1$
				Messages.format(NLSMessages.NLSRefactoring_Only_the_first_occurrence_of,
						"${key}"), results[0].getMessage()); //$NON-NLS-1$

		// check for duplicate keys????
		// check for keys already defined
		// check for keys
	}

	private RefactoringStatus createCheckInputStatus(ICompilationUnit cu) throws CoreException {
		NLSRefactoring refac= prepareRefac(cu);
		RefactoringStatus res= refac.checkFinalConditions(fHelper.fNpm);
		return res;
	}

	private void initDefaultValues() {
		//fPropertyFilePath= fHelper.getFile("/TestSetupProject/src2/p/test.properties").getFullPath(); //$NON-NLS-1$
		fResourceBundlePackage= fHelper.getPackageFragment("/TestSetupProject/src2/p");
		fResourceBundleName= "test.properties";
		fAccessorPackage= fHelper.getPackageFragment("/TestSetupProject/src1/p"); //$NON-NLS-1$
		fAccessorClassName= "Help"; //$NON-NLS-1$
		fSubstitutionPattern= "${key}"; //$NON-NLS-1$
	}

	private NLSRefactoring prepareRefac(ICompilationUnit cu) {
		NLSRefactoring refac= NLSRefactoring.create(cu);
		NLSSubstitution[] subs= refac.getSubstitutions();
		refac.setPrefix("");
		for (int i= 0; i < subs.length; i++) {
			subs[i].setState(NLSSubstitution.EXTERNALIZED);
			subs[i].generateKey(subs, new Properties());
		}
		fillInValues(refac);
		return refac;
	}

	private void checkNothingToDo(RefactoringStatus status) {
		assertEquals("fatal error expected", 1, status.getEntries().length); //$NON-NLS-1$

		RefactoringStatusEntry fatalError= status.getEntryAt(0);
		assertEquals("fatalerror", RefactoringStatus.FATAL, fatalError.getSeverity()); //$NON-NLS-1$
		assertEquals("errormessage", //$NON-NLS-1$
				NLSMessages.NLSRefactoring_nothing_to_do,
				fatalError.getMessage());
	}

	private void fillInValues(NLSRefactoring refac) {
		refac.setAccessorClassPackage(fAccessorPackage);
		//refac.setPropertyFilePath(fPropertyFilePath);
		refac.setResourceBundleName(fResourceBundleName);
		refac.setResourceBundlePackage(fResourceBundlePackage);
		refac.setAccessorClassName(fAccessorClassName);
		refac.setSubstitutionPattern(fSubstitutionPattern);
	}

}
