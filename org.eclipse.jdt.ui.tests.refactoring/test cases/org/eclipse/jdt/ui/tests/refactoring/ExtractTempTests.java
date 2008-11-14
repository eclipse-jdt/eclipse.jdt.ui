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

public class ExtractTempTests extends RefactoringTest {

	private static final Class clazz= ExtractTempTests.class;
	private static final String REFACTORING_PATH= "ExtractTemp/";

	private static final boolean BUG_82166_ImportRewrite_context= true;
	private static final boolean BUG_161617_ASTRewrite_space= true;

	private Object fCompactPref;

	public ExtractTempTests(String name) {
		super(name);
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	public static Test suite() {
		return new RefactoringTestSetup(new TestSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new RefactoringTestSetup(someTest);
	}

	private String getSimpleTestFileName(boolean canExtract, boolean input){
		String fileName = "A_" + getName();
		if (canExtract)
			fileName += input ? "_in": "_out";
		return fileName + ".java";
	}

	private String getTestFileName(boolean canExtract, boolean input){
		String fileName= TEST_PATH_PREFIX + getRefactoringPath();
		fileName += canExtract ? "canExtract/": "cannotExtract/";
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

	public void test0() throws Exception{
		helper1(4, 16, 4, 17, false, false, "temp", "j");
	}

	public void test1() throws Exception{
		helper1(4, 16, 4, 17, true, false, "temp", "j");
	}

	public void test2() throws Exception{
		helper1(4, 16, 4, 17, true, true, "temp", "j");
	}

	public void test3() throws Exception{
		helper1(4, 16, 4, 17, false, true, "temp", "j");
	}

	public void test4() throws Exception{
		helper1(4, 16, 4, 21, false, false, "temp", "j");
	}

	public void test5() throws Exception{
		helper1(4, 16, 4, 21, true, false, "temp", "j");
	}

	public void test6() throws Exception{
		helper1(4, 16, 4, 21, true, true, "temp", "j");
	}

	public void test7() throws Exception{
		helper1(4, 16, 4, 21, false, true, "temp", "j");
	}

	public void test8() throws Exception{
		helper1(5, 20, 5, 25, true, false, "temp", "j");
	}

	public void test9() throws Exception{
		helper1(5, 20, 5, 25, false, false, "temp", "j");
	}

	public void test10() throws Exception{
		helper1(5, 20, 5, 25, true, false, "temp", "i");
	}

	public void test11() throws Exception{
		helper1(5, 20, 5, 25, true, false, "temp", "i");
	}

	public void test12() throws Exception{
		helper1(5, 17, 5, 22, true, false, "temp", "i");
	}

	public void test13() throws Exception{
		helper1(7, 16, 7, 42, true, false, "temp", "iterator");
	}

	public void test14() throws Exception{
		helper1(6, 15, 6, 20, false, false, "temp", "y2");
	}

	public void test15() throws Exception{
		helper1(7, 23, 7, 28, false, false, "temp", "y2");
	}

	public void test16() throws Exception{
		helper1(7, 23, 7, 28, false, false, "temp", "y2");
	}

	public void test17() throws Exception{
		helper1(5, 20, 5, 25, true, false, "temp", "j");
	}

	public void test18() throws Exception{
		helper1(6, 20, 6, 25, true, false, "temp", "i");
	}

	public void test19() throws Exception{
		helper1(5, 20, 5, 23, true, false, "temp", "f");
	}

//cannot do it - see testFail16
//	public void test20() throws Exception{
//		printTestDisabledMessage("regression test for bug#11474");
//		helper1(5, 9, 5, 12, false, false, "temp", "temp");
//	}

	public void test21() throws Exception{
		helper1(5, 16, 5, 17, false, false, "temp", "f2");
	}

//cannot do it - see testFail17
//	public void test22() throws Exception{
//		printTestDisabledMessage("regression test for bug#11474");
//		helper1(6, 13, 6, 16, false, false, "temp", "temp");
//	}

	public void test23() throws Exception{
		helper1(7, 17, 7, 20, false, false, "temp", "b");
	}

//	public void test24() throws Exception{
//test disabled - trainling semicolons are disallowed now
//		//regression test for bug#8116
//		helper1(4, 16, 4, 18, false, false, "temp", "temp");
//	}

	public void test25() throws Exception{
//		printTestDisabledMessage("regression test for bug#8895");
		helper1(4, 17, 4, 22, true, false, "temp", "i");
	}

	public void test26() throws Exception{
//		printTestDisabledMessage("regression test for 9905");
		helper1(5, 19, 5, 23, true, false, "temp", "i");
	}

	public void test27() throws Exception{
//		printTestDisabledMessage("regression test for 8123");
		helper1(4, 15, 4, 19, true, false, "temp", "j");
	}

	public void test28() throws Exception{
//		printTestDisabledMessage("regression test for 11026");
		helper1(4, 16, 4, 31, true, false, "temp", "b");
	}

	public void test29() throws Exception{
		helper1(4, 19, 4, 22, true, false, "temp", "string");
	}

	public void test30() throws Exception{
		helper1(5, 16, 5, 20, true, false, "temp", "ff2");
	}

	public void test31() throws Exception{
		helper1(5, 16, 5, 20, true, false, "temp", "j");
	}

	public void test32() throws Exception{
		helper1(4, 16, 4, 22, true, false, "temp", "j");
	}

	public void test33() throws Exception{
//		printTestDisabledMessage("regression test for bug#11449");
		helper1(4, 19, 4, 33, true, false, "temp", "object");
	}

	public void test34() throws Exception{
//		printTestDisabledMessage("another regression test for bug#11449");
		helper1(4, 19, 4, 46, true, false, "temp", "arrayList");
	}

	public void test35() throws Exception{
//		printTestDisabledMessage("another regression test for bug#11622");
		helper1(8, 19, 8, 28, true, false, "temp", "lists");
	}

	public void test36() throws Exception{
//		printTestDisabledMessage("another regression test for bug#12205");
		helper1(11, 15, 11, 25, true, false, "temp", "foo");
	}

	public void test37() throws Exception{
//		printTestDisabledMessage("another regression test for bug#15196");
		helper1(8, 20, 8, 25, true, false, "temp", "j");
	}

	public void test38() throws Exception{
//		printTestDisabledMessage("regression test for bug#17473");
		helper1(5, 28, 5, 32, true, false, "temp1", "temp2");
	}

	public void test39() throws Exception{
//		printTestDisabledMessage("regression test for bug#20520 ");
		helper1(4, 14, 4, 26, true, false, "temp", "object");
	}

	public void test40() throws Exception{
//		printTestDisabledMessage("test for bug 21815");
		helper1(4, 9, 4, 16, true, false, "temp", "a");
	}

	public void test41() throws Exception{
//		printTestDisabledMessage("test for bug 21815");
		helper1(4, 9, 4, 36, true, false, "temp", "length");
	}

	public void test42() throws Exception{
//		printTestDisabledMessage("test for bug 19930");
		helper1(5, 16, 5, 35, true, false, "temp", "length");
	}

	public void test43() throws Exception{
//		printTestDisabledMessage("test for bug 19930");
		helper1(5, 20, 5, 36, true, false, "temp", "fred");
	}

	public void test44() throws Exception{
		//21939
		helper1(5, 20, 5, 28, true, false, "temp", "fred");
	}

	public void test45() throws Exception{
		//21939
		helper1(4, 16, 4, 19, true, false, "temp", "f");
	}

	public void test46() throws Exception{
//		printTestDisabledMessage("test for bug 21815");
		helper1(4, 9, 4, 12, true, false, "temp", "f");
	}

	public void test47() throws Exception{
		helper1(5, 9, 5, 12, true, false, "temp", "r");
	}

	public void test48() throws Exception{
//		printTestDisabledMessage("test for bug#22054");
		helper1(4, 16, 4, 32, true, false, "temp", "string");
	}

	public void test49() throws Exception{
//		printTestDisabledMessage("test for bug#23282 ");
		helper1(5, 15, 5, 19, true, false, "temp", "flag2");
	}

	public void test50() throws Exception{
//		printTestDisabledMessage("test for bug#23283 ");
		helper1(5, 15, 5, 19, true, false, "temp", "flag2");
	}

	public void test51() throws Exception{
//		printTestDisabledMessage("test for bug#23281");
		helper1(5, 15, 5, 18, true, false, "temp", "i");
	}

	public void test52() throws Exception{
//		printTestDisabledMessage("test for bug#26036");
		helper1(15, 47, 15, 60, true, false, "valueOnIndexI", "object");
	}

	public void test53() throws Exception{
		helper1(6, 17, 6, 22, true, false, "temp", "i");
	}

	public void test54() throws Exception{
		helper1(6, 37, 6, 43, true, false, "temp", "i");
	}

	public void test55() throws Exception{
		helper1(6, 19, 6, 24, true, false, "temp", "i");
	}

	public void test56() throws Exception{
		helper1(6, 24, 6, 29, true, false, "temp", "i");
	}

	public void test57() throws Exception{
//		printTestDisabledMessage("test for bug 24808");
		helper1(8, 30, 8, 54, true, false, "newVariable", "string");
	}

	public void test58() throws Exception{
//		printTestDisabledMessage("test for bug 30304");
		helper1(7, 14, 7, 30, true, false, "temp", "equals");
	}

	public void test59() throws Exception{
//		printTestDisabledMessage("test for bug 30304");
		helper1(7, 17, 7, 18, true, false, "temp", "s2");
	}

	public void test60() throws Exception{
//		printTestDisabledMessage("test for bug 30304");
		helper1(7, 17, 7, 18, true, false, "temp", "s2");
	}

	public void test61() throws Exception{
//		printTestDisabledMessage("test for bug 30304");
		helper1(7, 17, 7, 18, true, false, "temp", "s2");
	}

	public void test62() throws Exception{
//		printTestDisabledMessage("test for bug 33405 Refactoring extract local variable fails in nested if statements");
		helper1(10, 17, 10, 28, true, false, "temp", "string");
	}

	public void test63() throws Exception{
//		printTestDisabledMessage("test for bug 33405 Refactoring extract local variable fails in nested if statements");
		helper1(9, 20, 9, 23, true, false, "temp", "string");
	}

	public void test64() throws Exception{
//		printTestDisabledMessage("test for bug 33405 Refactoring extract local variable fails in nested if statements");
		helper1(10, 17, 10, 28, true, false, "temp", "string");
	}

	public void test65() throws Exception{
//		printTestDisabledMessage("test for bug 35981 extract local variable causing exception [refactoring] ");
		helper1(6, 19, 6, 22, true, false, "temp", "bar2");
	}

	public void test66() throws Exception{
		helper1(7, 32, 7, 33, true, false, "temp", "e2");
	}

	public void test67() throws Exception{
//		printTestDisabledMessage("test for bug 37834");
		helper1(6, 16, 6, 21, true, false, "temp", "integer");
	}

	public void test68() throws Exception{
//		printTestDisabledMessage("test for bug 37834");
		helper1(6, 14, 6, 21, true, false, "temp", "d2");
	}

	public void test69() throws Exception{
		helper1(5, 24, 5, 26, true, false, "temp", "string2");
	}

	public void test70() throws Exception{
//		printTestDisabledMessage("test for bug 40353");
		helper1(7, 28, 7, 42, true, true, "temp", "length");
	}

	public void test71() throws Exception{
//		printTestDisabledMessage("test for bug 40353");
		helper1(8, 24, 8, 34, true, false, "temp", "string");
	}

	public void test72() throws Exception{
//		printTestDisabledMessage("test for bug 40353");
		helper1(8, 32, 8, 33, true, false, "temp", "i2");
	}

	public void test73() throws Exception{
//		printTestDisabledMessage("test for bug 40353");
		warningHelper1(6, 39, 6, 40, true, false, "temp", "i2", RefactoringStatus.WARNING);
		// (warning is superfluous, but detection would need flow analysis)
	}

	public void test74() throws Exception{
//		printTestDisabledMessage("test for bug 40353");
		helper1(7, 36, 7, 49, true, false, "temp", "string");
	}

	public void test75() throws Exception{
//		printTestDisabledMessage("test for bug 40353");
		helper1(7, 36, 7, 39, true, false, "temp", "j");
	}

	public void test76() throws Exception{
//		printTestDisabledMessage("test for bug 40353");
		helper1(7, 48, 7, 49, true, false, "temp", "k2");
	}

	public void test77() throws Exception {
//		printTestDisabledMessage("test for bug 40353: extracting fragment which matches for_initializer");
		helper1(10, 13, 10, 17, true, false, "temp", "f");
	}

	public void test78() throws Exception {
//		printTestDisabledMessage("test for bug 50971: Extract temp: index out of bounds error [refactoring]");
		helper1(5, 21, 5, 27, true, false, "o2", "o");
	}

	public void test79() throws Exception {
		helper1(10, 40, 10, 60, true, false, "strong", "string");
	}

	public void test80() throws Exception {
		helper1(5, 37, 5, 45, true, false, "name", "string");
	}

	public void test81() throws Exception {
		helper1(7, 18, 7, 23, true, false, "k", "const2");
	}

	public void test82() throws Exception {
		helper1(5, 1, 6, 1, true, false, "one", "integer");
	}

	public void test83() throws Exception{
		if (BUG_82166_ImportRewrite_context) {
			printTestDisabledMessage("BUG_82166_ImportRewrite_context");
			return;
		}
		helper1(7, 17, 7, 27, false, false, "temp", "test");
	}

	public void test84() throws Exception{
		helper1(5, 16, 5, 17, false, false, "temp", "j");
	}

	public void test85() throws Exception{
		if (BUG_82166_ImportRewrite_context) {
			printTestDisabledMessage("BUG_82166_ImportRewrite_context");
			return;
		}
		helper1(10, 22, 10, 32, true, true, "temp", "test2");
	}

	public void test86() throws Exception{
//		printTestDisabledMessage("test for parameterized class instance creation");
		helper1(14, 22, 14, 37, true, true, "name", "a");
	}

	public void test87() throws Exception{
//		printTestDisabledMessage("test for parameterized class instance creation");
		helper1(15, 17, 15, 27, true, true, "a2", "a2");
	}

	public void test88() throws Exception{
		helper1(14, 14, 14, 19, true, false, "foo", "foo");
	}

	public void test89() throws Exception{
		IPackageFragment a= getRoot().createPackageFragment("a", true,	null);
		ICompilationUnit aA= a.createCompilationUnit("A.java", "package a; public class A {}", true, null);
		aA.save(null, true);

		IPackageFragment b= getRoot().createPackageFragment("b", true,	null);
		ICompilationUnit bA= b.createCompilationUnit("A.java", "package b; public class A {}", true, null);
		bA.save(null, true);

		helper1(15, 7, 15, 15, true, false, "foo", "method");
	}

	public void test90() throws Exception {
		helper1(8, 19, 8, 28, true, false, "temp", "number");
	}

	public void test91() throws Exception {
		helper1(8, 19, 8, 28, true, false, "temp", "integer");
	}

	public void test92() throws Exception { //bug 104293
		helper1(9, 32, 9, 44, true, false, "asList", "asList");
	}

	public void test93() throws Exception { // syntax error
		helper1(6, 28, 6, 34, true, false, "bla", "string");
	}

	public void test94() throws Exception {
		//test for bug 19851, syntax error
		helper1(6, 9, 6, 24, false, false, "temp", "string");
	}

	public void test95() throws Exception {
		//test for bug 131556
		helper1(5, 23, 5, 33, true, false, "temp", "b");
	}

	public void test96() throws Exception {
		//test for bug 71762
		helper1(6, 32, 6, 37, true, false, "isquared", "j");
	}

	public void test97() throws Exception {
		//test for bug 48231
		helper1(10, 32, 10, 47, true, false, "temp", "nextElement");
	}

	public void test98() throws Exception {
		//test for bug 132931
		helper1(8, 32, 8, 44, true, true, "temp", "string");
	}

	public void test99() throws Exception {
		//test for bug 99963
		helper1(7, 32, 7, 36, true, false, "temp", "a");
	}

	public void test100() throws Exception {
		//test for bug 161617
		if (BUG_161617_ASTRewrite_space) {
			printTestDisabledMessage("BUG_161617_ASTRewrite_space");
			return;
		}
		helper1(5, 28, 5, 40, true, false, "temp", "object");
	}

	public void test101() throws Exception {
		helper1(9, 13, 9, 25, true, false, "temp", "object");
	}

	public void test102() throws Exception {
		helper1(9, 24, 9, 29, true, false, "temp", "j");
	}
	
	public void test103() throws Exception {
		//test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=243101
		helper1(7, 21, 7, 33, true, false, "temp", "valueOf");
	}


	public void testZeroLengthSelection0() throws Exception {
//		printTestDisabledMessage("test for bug 30146");
		helper1(4, 18, 4, 18, true, false, "temp", "j");
	}

	// -- testing failing preconditions
	public void testFail0() throws Exception{
		failHelper1(5, 16, 5, 17, false, false, "temp", RefactoringStatus.ERROR);
	}

	public void testFail1() throws Exception{
		failHelper1(4, 9, 5, 13, false, false, "temp", RefactoringStatus.FATAL);
	}

	public void testFail2() throws Exception{
		failHelper1(4, 9, 4, 20, false, false, "temp", RefactoringStatus.FATAL);
	}

	public void testFail3() throws Exception{
		failHelper1(4, 9, 4, 20, false, false, "temp", RefactoringStatus.FATAL);
	}

	public void testFail4() throws Exception{
		failHelper1(5, 9, 5, 12, false, false, "temp", RefactoringStatus.FATAL);
	}

	public void testFail5() throws Exception{
		failHelper1(3, 12, 3, 15, false, false, "temp", RefactoringStatus.FATAL);
	}

	public void testFail6() throws Exception{
		failHelper1(4, 14, 4, 19, false, false, "temp", RefactoringStatus.FATAL);
	}

	public void testFail7() throws Exception{
		failHelper1(4, 15, 4, 20, false, false, "temp", RefactoringStatus.FATAL);
	}

//	public void testFail8() throws Exception{
//		printTestDisabledMessage("removed");
//		failHelper1(5, 16, 5, 20, false, false, "temp", RefactoringStatus.FATAL);
//	}

	public void testFail9() throws Exception{
		failHelper1(4, 19, 4, 23, false, false, "temp", RefactoringStatus.FATAL);
	}

	public void testFail10() throws Exception{
		failHelper1(4, 33, 4, 39, false, false, "temp", RefactoringStatus.FATAL);
	}

	public void testFail11() throws Exception{
//		printTestDisabledMessage("regression test for bug#13061");
		failHelper1(4, 18, 4, 19, false, false, "temp", RefactoringStatus.FATAL);
	}

// removed - allowe now (see bug 98847)
//	public void testFail12() throws Exception{
//		failHelper1(4, 16, 4, 29, false, false, "temp", RefactoringStatus.FATAL);
//	}

//removed
//	public void testFail13() throws Exception{
//		failHelper1(5, 16, 5, 20, false, false, "temp", RefactoringStatus.FATAL);
//	}

//removed
//	public void testFail14() throws Exception{
//		failHelper1(4, 16, 4, 22, false, false, "temp", RefactoringStatus.FATAL);
//	}

//removed
//	public void testFail15() throws Exception{
//		failHelper1(4, 19, 4, 22, false, false, "temp", RefactoringStatus.FATAL);
//	}

//removed - allowed now (see bug 21815)
//	public void testFail16() throws Exception{
//		failHelper1(5, 9, 5, 12, false, false, "temp", RefactoringStatus.FATAL);
//	}
//
//	public void testFail17() throws Exception{
//		failHelper1(6, 13, 6, 16, false, false, "temp", RefactoringStatus.FATAL);
//	}

	public void testFail18() throws Exception{
//		printTestDisabledMessage("regression test for bug#8149");
//		printTestDisabledMessage("regression test for bug#37547");
		// test for bug 40353: is now FATAL"
		failHelper1(4, 27, 4, 28, false, false, "temp", RefactoringStatus.FATAL);
	}

	public void testFail19() throws Exception{
//		printTestDisabledMessage("regression test for bug#8149");
		failHelper1(6, 16, 6, 18, false, false, "temp", RefactoringStatus.WARNING);
	}

	public void testFail20() throws Exception{
//		printTestDisabledMessage("regression test for bug#13249");
		failHelper1(3, 9, 3, 41, false, false, "temp", RefactoringStatus.FATAL);
	}

//removed - allowed now (see bug 53243)
//	public void testFail21() throws Exception{
//		//test for bug 19851
//		failHelper1(6, 9, 6, 24, false, false, "temp", RefactoringStatus.FATAL);
//	}

	public void testFail22() throws Exception{
//		printTestDisabledMessage("test for bug 21815");
		failHelper1(5, 9, 5, 12, false, false, "temp", RefactoringStatus.FATAL);
	}

	public void testFail23() throws Exception{
//		printTestDisabledMessage("test for bug 24265");
		failHelper1(4, 13, 4, 14, false, false, "temp", RefactoringStatus.FATAL);
	}

	public void testFail24() throws Exception{
//		printTestDisabledMessage("test for bug 24265");
		failHelper1(4, 13, 4, 14, false, false, "temp", RefactoringStatus.FATAL);
	}

	public void testFail25() throws Exception{
		failHelper1(4, 16, 4, 18, false, false, "temp", RefactoringStatus.FATAL);
	}

	public void testFail26() throws Exception{
		failHelper1(4, 15, 4, 20, false, false, "temp", RefactoringStatus.FATAL);
	}

	public void testFail27() throws Exception{
//		printTestDisabledMessage("test for bug 29513");
		failHelper1(7, 13, 7, 24, true, false, "temp", RefactoringStatus.WARNING);
	}

	public void testFail28() throws Exception{
//		printTestDisabledMessage("test for bug 29513");
		failHelper1(7, 17, 7, 28, true, false, "temp", RefactoringStatus.WARNING);
	}

	public void testFail29() throws Exception {
//		printTestDisabledMessage("test for bug 40353: extracting for_updater referring to loop variable");
		failHelper1(5, 32, 5, 35, true, false, "temp", RefactoringStatus.FATAL);
	}

	public void testFail30() throws Exception {
//		printTestDisabledMessage("test for bug 40353: extracting for_test referring to loop variable");
		failHelper1(5, 25, 5, 30, true, false, "temp", RefactoringStatus.FATAL);
	}

	public void testFail31() throws Exception {
//		printTestDisabledMessage("test for bug 40353: extracting for_test referring to loop variable");
		failHelper1(5, 31, 5, 32, true, false, "temp", RefactoringStatus.FATAL);
	}

	public void testFail32() throws Exception {
//		printTestDisabledMessage("test for bug 40353: extracting for_test referring to loop variable");
		failHelper1(6, 35, 6, 36, true, false, "temp", RefactoringStatus.FATAL);
	}

	public void testFail33() throws Exception {
//		printTestDisabledMessage("test for bug 40353: extracting for_updater");
		failHelper1(6, 17, 6, 21, true, false, "temp", RefactoringStatus.FATAL);
	}

	public void testFail34() throws Exception {
//		printTestDisabledMessage("test for bug 40353: extracting for_initializer");
		failHelper1(9, 20, 9, 24, true, false, "temp", RefactoringStatus.FATAL);
	}

	public void testFail35() throws Exception {
//		printTestDisabledMessage("test for bug 45007: QualifiedName");
		failHelper1(6, 33, 6, 38, true, false, "temp", RefactoringStatus.FATAL);
	}

	public void testFail36() throws Exception {
//		printTestDisabledMessage("test for bug 45007: FieldAccess");
		failHelper1(6, 33, 6, 38, true, false, "temp", RefactoringStatus.FATAL);
	}

	public void testFail37() throws Exception {
//		printTestDisabledMessage("test for bug 45007: QualifiedName (nested)");
		failHelper1(5, 40, 5, 51, true, false, "temp", RefactoringStatus.FATAL);
	}

	public void testFail38() throws Exception {
		failHelper1(4, 45, 4, 50, true, false, "temp", RefactoringStatus.FATAL);
	}
}
