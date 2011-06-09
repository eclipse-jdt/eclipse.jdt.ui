/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.refactoring.code.ExtractTempRefactoring;

import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;

public class ExtractTempTests17 extends RefactoringTest {

	private static final Class clazz= ExtractTempTests17.class;
	private static final String REFACTORING_PATH= "ExtractTemp/";

	private Object fCompactPref;

	public ExtractTempTests17(String name) {
		super(name);
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	public static Test suite() {
		return new Java17Setup(new TestSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new Java17Setup(someTest);
	}

	private String getSimpleTestFileName(boolean canExtract, boolean input){
		String fileName = "A_" + getName();
		if (canExtract)
			fileName += input ? "_in": "_out";
		return fileName + ".java";
	}

	private String getTestFileName(boolean canExtract, boolean input){
		String fileName= TEST_PATH_PREFIX + getRefactoringPath();
		fileName+= canExtract ? "canExtract17/" : "cannotExtract17/";
		return fileName + getSimpleTestFileName(canExtract, input);
	}

	private ICompilationUnit createCUfromTestFile(IPackageFragment pack, boolean canExtract, boolean input) throws Exception {
		return createCU(pack, getSimpleTestFileName(canExtract, input), getFileContents(getTestFileName(canExtract, input)));
	}

	protected void setUp() throws Exception {
		super.setUp();
		Hashtable options= JavaCore.getOptions();

		String setting= DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR;
		fCompactPref= options.get(setting);
		options.put(setting, DefaultCodeFormatterConstants.TRUE);
		JavaCore.setOptions(options);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		Hashtable options= JavaCore.getOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR, fCompactPref);
		JavaCore.setOptions(options);
	}

	private void helper1(int startLine, int startColumn, int endLine, int endColumn, boolean replaceAll, boolean makeFinal, String tempName, String guessedTempName) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		ISourceRange selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
		ExtractTempRefactoring ref= new ExtractTempRefactoring(cu, selection.getOffset(), selection.getLength());

		RefactoringStatus activationResult= ref.checkInitialConditions(new NullProgressMonitor());
		assertTrue("activation was supposed to be successful", activationResult.isOK());

		ref.setReplaceAllOccurrences(replaceAll);
		ref.setDeclareFinal(makeFinal);
		ref.setTempName(tempName);

		assertEquals("temp name incorrectly guessed", guessedTempName, ref.guessTempName());

		RefactoringStatus checkInputResult= ref.checkFinalConditions(new NullProgressMonitor());
		assertTrue("precondition was supposed to pass but was " + checkInputResult.toString(), checkInputResult.isOK());

		performChange(ref, false);

		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String newCuName= getSimpleTestFileName(true, true);
		ICompilationUnit newcu= pack.getCompilationUnit(newCuName);
		assertTrue(newCuName + " does not exist", newcu.exists());
		assertEqualLines(getFileContents(getTestFileName(true, false)), newcu.getSource());
	}

	private void warningHelper1(int startLine, int startColumn, int endLine, int endColumn, boolean replaceAll, boolean makeFinal, String tempName, String guessedTempName, int expectedStatus) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		ISourceRange selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
		ExtractTempRefactoring ref= new ExtractTempRefactoring(cu, selection.getOffset(), selection.getLength());

		RefactoringStatus activationResult= ref.checkInitialConditions(new NullProgressMonitor());
		assertTrue("activation was supposed to be successful", activationResult.isOK());

		ref.setReplaceAllOccurrences(replaceAll);
		ref.setDeclareFinal(makeFinal);
		ref.setTempName(tempName);

		assertEquals("temp name incorrectly guessed", guessedTempName, ref.guessTempName());

		RefactoringStatus checkInputResult= ref.checkFinalConditions(new NullProgressMonitor());
		assertEquals("status", expectedStatus, checkInputResult.getSeverity());

		performChange(ref, false);

		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String newCuName= getSimpleTestFileName(true, true);
		ICompilationUnit newcu= pack.getCompilationUnit(newCuName);
		assertTrue(newCuName + " does not exist", newcu.exists());
		assertEqualLines(getFileContents(getTestFileName(true, false)), newcu.getSource());
	}

	private void failHelper1(int startLine, int startColumn, int endLine, int endColumn, boolean replaceAll, boolean makeFinal, String tempName, int expectedStatus) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), false, true);
		ISourceRange selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
		ExtractTempRefactoring ref= new ExtractTempRefactoring(cu, selection.getOffset(), selection.getLength());
		ref.setReplaceAllOccurrences(replaceAll);
		ref.setDeclareFinal(makeFinal);
		ref.setTempName(tempName);
		RefactoringStatus result= performRefactoring(ref);
		assertNotNull("precondition was supposed to fail", result);
		assertEquals("status", expectedStatus, result.getSeverity());
	}

	//--- TESTS

	public void test110() throws Exception {
		helper1(14, 13, 14, 15, true, false, "temp", "ex2");
	}
}
