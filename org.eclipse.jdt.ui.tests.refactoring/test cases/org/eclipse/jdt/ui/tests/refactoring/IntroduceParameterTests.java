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

package org.eclipse.jdt.ui.tests.refactoring;

import java.util.StringTokenizer;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;

import org.eclipse.jdt.internal.corext.refactoring.code.IntroduceParameterRefactoring;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class IntroduceParameterTests extends LineColumnSelectionTestCase {

	private static final String SLASH_OUT= "/out";
	public IntroduceParameterTests(String name) {
		super(name);
	}
	
	public static Test setUpTest(Test test) {
		return new MySetup(test);
	}

	/** for the JUnit Launcher */
	public static Test suite() {
		//TODO: re-runnable tests: setUp() should create project iff neccessary (circumvent TestDecorator "MySetup")
		if (true) {
			return new MySetup(new TestSuite(IntroduceParameterTests.class));
		} else {
			System.err.println("*** Running only parts of IntroduceParameterTests!");
			TestSuite suite= new TestSuite();
			suite.addTest(new IntroduceParameterTests("testSimple_StaticGetter1"));
			return new MySetup(suite);
		}
	}
	
	protected String getResourceLocation() {
		return "IntroduceParameter/";
	}

	/**
	 * get names from comment in source "//name: guessedName -> nameToUse"
	 * <br>relies on tabwidth == 4
	 * @return {"guessedName", "nameToUse"} or null iff no name comment found
	 */
	private String[] getNames(ICompilationUnit cu) throws Exception {
		String source= cu.getSource();
		String name= "//name:";
		int namStart= source.indexOf(name);
		if (namStart == -1)
			return null;

		int dataStart= namStart + name.length();
		StringTokenizer tokenizer= new StringTokenizer(source.substring(dataStart), " ->\t\r\n");
		String[] result= {tokenizer.nextToken(), tokenizer.nextToken()};
		return result;
	}

	private void performOK() throws Exception {
		perform(RefactoringStatus.OK, RefactoringStatus.OK);
	}

	private void performInvalidSelection() throws Exception {
		perform(RefactoringStatus.FATAL, RefactoringStatus.FATAL);
	}

	private void perform(int expectedActivationStatus, int expectedInputStatus) throws Exception {
		String packageName= adaptPackage(getName());
		IPackageFragment packageFragment= MySetup.getDefaultSourceFolder().createPackageFragment(packageName, true , null);
		ICompilationUnit cu= createCU(packageFragment, getName());

		ISourceRange selection= getSelection(cu);
		IntroduceParameterRefactoring refactoring= IntroduceParameterRefactoring.create(
			cu, selection.getOffset(), selection.getLength());		

		NullProgressMonitor pm= new NullProgressMonitor();
		RefactoringStatus status= refactoring.checkInitialConditions(pm);
		assertEquals("wrong activation status", expectedActivationStatus, status.getSeverity());
		if (! status.isOK())
			return;
		
		String[] names= getNames(cu);
		if (names == null) {
			refactoring.setParameterName(refactoring.guessedParameterName());
		} else {
			assertEquals("incorrectly guessed parameter name", names[0], refactoring.guessedParameterName());
			refactoring.setParameterName(names[1]);
		}

		status.merge(refactoring.checkFinalConditions(pm));
		assertEquals("wrong input status", expectedInputStatus, status.getSeverity());
		if (status.getSeverity() == RefactoringStatus.FATAL)
			return;
		
		String out= getProofedContent(packageName + SLASH_OUT, getName());
		performTest(cu, refactoring, out);
	}
		
// ---

	public void testInvalid_NotInMethod1() throws Exception {
		performInvalidSelection();
	}
	public void testInvalid_NotInMethod2() throws Exception {
		performInvalidSelection();
	}
	public void testInvalid_NotInMethod3() throws Exception {
		performInvalidSelection();
	}

	public void testInvalid_PartName1() throws Exception {
		performInvalidSelection();
	}

	public void testInvalid_PartString() throws Exception {
		performInvalidSelection();
	}

	public void testInvalid_NoMethodBinding() throws Exception {
		performInvalidSelection();
	}
	
	public void testInvalid_NoExpression1() throws Exception {
		performInvalidSelection();
	}
	
	//	---

	public void testSimple_ConstantExpression1() throws Exception {
		performOK();
	}

	public void testSimple_ConstantExpression2() throws Exception {
		performOK();
	}

	public void testSimple_NewInstance1() throws Exception {
		performOK();
	}

	public void testSimple_NewInstanceImport() throws Exception {
		performOK();
	}

	public void testSimple_StaticGetter1() throws Exception {
		performOK();
	}
	
	public void testSimple_Formatting1() throws Exception {
		performOK();
	}
	
	public void testSimple_Javadoc1() throws Exception {
		performOK();
	}

	public void testSimple_Javadoc2() throws Exception {
		performOK();
	}
	
	public void testSimple_Constructor1() throws Exception {
		performOK();
	}
}
