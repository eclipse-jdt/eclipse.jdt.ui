/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Nikolay Metchev <nikolaymetchev@gmail.com> - [extract local] Extract to local variable not replacing multiple occurrences in same statement - https://bugs.eclipse.org/406347
 *     Nicolaj Hoess <nicohoess@gmail.com> - [extract local] puts declaration at wrong position - https://bugs.eclipse.org/65875
 *     Xiaye Chi <xychichina@gmail.com> - [extract local] Extract to local variable may result in NullPointerException. - https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/39
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.refactoring.code.ExtractTempRefactoring;

import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

public class ExtractTempTests extends GenericRefactoringTest {
	private static final String REFACTORING_PATH= "ExtractTemp/";

	private String fCompactPref;

	public ExtractTempTests() {
		rts= new RefactoringTestSetup();
	}

	protected ExtractTempTests(RefactoringTestSetup rts) {
		super(rts);
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	protected String getSimpleTestFileName(boolean canExtract, boolean input){
		StringBuilder fileName = new StringBuilder("A_").append(getName());
		if (canExtract)
			fileName.append(input ? "_in": "_out");
		return fileName.append(".java").toString();
	}

	protected String getTestFileName(boolean canExtract, boolean input){
		StringBuilder fileName= new StringBuilder(TEST_PATH_PREFIX).append(getRefactoringPath());
		fileName.append(canExtract ? "canExtract/": "cannotExtract/");
		return fileName.append(getSimpleTestFileName(canExtract, input)).toString();
	}

	private ICompilationUnit createCUfromTestFile(IPackageFragment pack, boolean canExtract, boolean input) throws Exception {
		return createCU(pack, getSimpleTestFileName(canExtract, input), getFileContents(getTestFileName(canExtract, input)));
	}

	@Before
	public void before() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();

		String setting= DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR;
		fCompactPref= options.get(setting);
		options.put(setting, DefaultCodeFormatterConstants.TRUE);
		JavaCore.setOptions(options);
	}

	@After
	public void after() throws Exception {
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR, fCompactPref);
		JavaCore.setOptions(options);
	}

	protected void helper1(int startLine, int startColumn, int endLine, int endColumn, boolean replaceAll, boolean makeFinal, String tempName, String guessedTempName) throws Exception {
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

	private void warningHelper1(int startLine, int startColumn, int endLine, int endColumn, boolean replaceAll, boolean makeFinal, String tempName, String guessedTempName, int expectedStatus) throws Exception {
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

	protected void failHelper1(int startLine, int startColumn, int endLine, int endColumn, boolean replaceAll, boolean makeFinal, String tempName, int expectedStatus) throws Exception {
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

	@Test
	public void test0() throws Exception {
		helper1(4, 16, 4, 17, false, false, "temp", "j");
	}

	@Test
	public void test1() throws Exception {
		helper1(4, 16, 4, 17, true, false, "temp", "j");
	}

	@Test
	public void test2() throws Exception {
		helper1(4, 16, 4, 17, true, true, "temp", "j");
	}

	@Test
	public void test3() throws Exception {
		helper1(4, 16, 4, 17, false, true, "temp", "j");
	}

	@Test
	public void test4() throws Exception {
		helper1(4, 16, 4, 21, false, false, "temp", "j");
	}

	@Test
	public void test5() throws Exception {
		helper1(4, 16, 4, 21, true, false, "temp", "j");
	}

	@Test
	public void test6() throws Exception {
		helper1(4, 16, 4, 21, true, true, "temp", "j");
	}

	@Test
	public void test7() throws Exception {
		helper1(4, 16, 4, 21, false, true, "temp", "j");
	}

	@Test
	public void test8() throws Exception {
		helper1(5, 20, 5, 25, true, false, "temp", "j");
	}

	@Test
	public void test9() throws Exception {
		helper1(5, 20, 5, 25, false, false, "temp", "j");
	}

	@Test
	public void test10() throws Exception {
		helper1(5, 20, 5, 25, true, false, "temp", "i");
	}

	@Test
	public void test11() throws Exception {
		helper1(5, 20, 5, 25, true, false, "temp", "i");
	}

	@Test
	public void test12() throws Exception {
		helper1(5, 17, 5, 22, true, false, "temp", "i");
	}

	@Test
	public void test13() throws Exception {
		helper1(7, 16, 7, 42, true, false, "temp", "iterator");
	}

	@Test
	public void test14() throws Exception {
		helper1(6, 15, 6, 20, false, false, "temp", "y2");
	}

	@Test
	public void test15() throws Exception {
		helper1(7, 23, 7, 28, false, false, "temp", "y2");
	}

	@Test
	public void test16() throws Exception {
		helper1(7, 23, 7, 28, false, false, "temp", "y2");
	}

	@Test
	public void test17() throws Exception {
		helper1(5, 20, 5, 25, true, false, "temp", "j");
	}

	@Test
	public void test18() throws Exception {
		helper1(6, 20, 6, 25, true, false, "temp", "i");
	}

	@Test
	public void test19() throws Exception {
		helper1(5, 20, 5, 23, true, false, "temp", "f");
	}

//cannot do it - see testFail16
//	@Test
//	public void test20() throws Exception {
//		printTestDisabledMessage("regression test for bug#11474");
//		helper1(5, 9, 5, 12, false, false, "temp", "temp");
//	}

	@Test
	public void test21() throws Exception {
		helper1(5, 16, 5, 17, false, false, "temp", "f2");
	}

//cannot do it - see testFail17
//	@Test
//	public void test22() throws Exception {
//		printTestDisabledMessage("regression test for bug#11474");
//		helper1(6, 13, 6, 16, false, false, "temp", "temp");
//	}

	@Test
	public void test23() throws Exception {
		helper1(7, 17, 7, 20, false, false, "temp", "b");
	}

//	@Test
//	public void test24() throws Exception {
//test disabled - trainling semicolons are disallowed now
//		//regression test for bug#8116
//		helper1(4, 16, 4, 18, false, false, "temp", "temp");
//	}

	@Test
	public void test25() throws Exception {
//		printTestDisabledMessage("regression test for bug#8895");
		helper1(4, 17, 4, 22, true, false, "temp", "i");
	}

	@Test
	public void test26() throws Exception {
//		printTestDisabledMessage("regression test for 9905");
		helper1(5, 19, 5, 23, true, false, "temp", "i");
	}

	@Test
	public void test27() throws Exception {
//		printTestDisabledMessage("regression test for 8123");
		helper1(4, 15, 4, 19, true, false, "temp", "j");
	}

	@Test
	public void test28() throws Exception {
//		printTestDisabledMessage("regression test for 11026");
		helper1(4, 16, 4, 31, true, false, "temp", "b");
	}

	@Test
	public void test29() throws Exception {
		helper1(4, 19, 4, 22, true, false, "temp", "string");
	}

	@Test
	public void test30() throws Exception {
		helper1(5, 16, 5, 20, true, false, "temp", "ff2");
	}

	@Test
	public void test31() throws Exception {
		helper1(5, 16, 5, 20, true, false, "temp", "j");
	}

	@Test
	public void test32() throws Exception {
		helper1(4, 16, 4, 22, true, false, "temp", "j");
	}

	@Test
	public void test33() throws Exception {
//		printTestDisabledMessage("regression test for bug#11449");
		helper1(4, 19, 4, 33, true, false, "temp", "object");
	}

	@Test
	public void test34() throws Exception {
//		printTestDisabledMessage("another regression test for bug#11449");
		helper1(4, 19, 4, 46, true, false, "temp", "arrayList");
	}

	@Test
	public void test35() throws Exception {
//		printTestDisabledMessage("another regression test for bug#11622");
		helper1(8, 19, 8, 28, true, false, "temp", "lists");
	}

	@Test
	public void test36() throws Exception {
//		printTestDisabledMessage("another regression test for bug#12205");
		helper1(11, 15, 11, 25, true, false, "temp", "foo");
	}

	@Test
	public void test37() throws Exception {
//		printTestDisabledMessage("another regression test for bug#15196");
		helper1(8, 20, 8, 25, true, false, "temp", "j");
	}

	@Test
	public void test38() throws Exception {
//		printTestDisabledMessage("regression test for bug#17473");
		helper1(5, 28, 5, 32, true, false, "temp1", "temp2");
	}

	@Test
	public void test39() throws Exception {
//		printTestDisabledMessage("regression test for bug#20520 ");
		helper1(4, 14, 4, 26, true, false, "temp", "object");
	}

	@Test
	public void test40() throws Exception {
//		printTestDisabledMessage("test for bug 21815");
		helper1(4, 9, 4, 16, true, false, "temp", "a");
	}

	@Test
	public void test41() throws Exception {
//		printTestDisabledMessage("test for bug 21815");
		helper1(4, 9, 4, 36, true, false, "temp", "length");
	}

	@Test
	public void test42() throws Exception {
//		printTestDisabledMessage("test for bug 19930");
		helper1(5, 16, 5, 35, true, false, "temp", "length");
	}

	@Test
	public void test43() throws Exception {
//		printTestDisabledMessage("test for bug 19930");
		helper1(5, 20, 5, 36, true, false, "temp", "fred");
	}

	@Test
	public void test44() throws Exception {
		//21939
		helper1(5, 20, 5, 28, true, false, "temp", "fred");
	}

	@Test
	public void test45() throws Exception {
		//21939
		helper1(4, 16, 4, 19, true, false, "temp", "f");
	}

	@Test
	public void test46() throws Exception {
//		printTestDisabledMessage("test for bug 21815");
		helper1(4, 9, 4, 12, true, false, "temp", "f");
	}

	@Test
	public void test47() throws Exception {
		helper1(5, 9, 5, 12, true, false, "temp", "r");
	}

	@Test
	public void test48() throws Exception {
//		printTestDisabledMessage("test for bug#22054");
		helper1(4, 16, 4, 32, true, false, "temp", "string");
	}

	@Test
	public void test49() throws Exception {
//		printTestDisabledMessage("test for bug#23282 ");
		helper1(5, 15, 5, 19, true, false, "temp", "flag2");
	}

	@Test
	public void test50() throws Exception {
//		printTestDisabledMessage("test for bug#23283 ");
		helper1(5, 15, 5, 19, true, false, "temp", "flag2");
	}

	@Test
	public void test51() throws Exception {
//		printTestDisabledMessage("test for bug#23281");
		helper1(5, 15, 5, 18, true, false, "temp", "i");
	}

	@Test
	public void test52() throws Exception {
//		printTestDisabledMessage("test for bug#26036");
		helper1(15, 47, 15, 60, true, false, "valueOnIndexI", "object");
	}

	@Test
	public void test53() throws Exception {
		helper1(6, 17, 6, 22, true, false, "temp", "i");
	}

	@Test
	public void test54() throws Exception {
		helper1(6, 37, 6, 43, true, false, "temp", "i");
	}

	@Test
	public void test55() throws Exception {
		helper1(6, 19, 6, 24, true, false, "temp", "i");
	}

	@Test
	public void test56() throws Exception {
		helper1(6, 24, 6, 29, true, false, "temp", "i");
	}

	@Test
	public void test57() throws Exception {
//		printTestDisabledMessage("test for bug 24808");
		helper1(8, 30, 8, 54, true, false, "newVariable", "string");
	}

	@Test
	public void test58() throws Exception {
//		printTestDisabledMessage("test for bug 30304");
		helper1(7, 14, 7, 30, true, false, "temp", "equals");
	}

	@Test
	public void test59() throws Exception {
//		printTestDisabledMessage("test for bug 30304");
		helper1(7, 17, 7, 18, true, false, "temp", "s2");
	}

	@Test
	public void test60() throws Exception {
//		printTestDisabledMessage("test for bug 30304");
		helper1(7, 17, 7, 18, true, false, "temp", "s2");
	}

	@Test
	public void test61() throws Exception {
//		printTestDisabledMessage("test for bug 30304");
		helper1(7, 17, 7, 18, true, false, "temp", "s2");
	}

	@Test
	public void test62() throws Exception {
//		printTestDisabledMessage("test for bug 33405 Refactoring extract local variable fails in nested if statements");
		helper1(10, 17, 10, 28, true, false, "temp", "string");
	}

	@Test
	public void test63() throws Exception {
//		printTestDisabledMessage("test for bug 33405 Refactoring extract local variable fails in nested if statements");
		helper1(9, 20, 9, 23, true, false, "temp", "string");
	}

	@Test
	public void test64() throws Exception {
//		printTestDisabledMessage("test for bug 33405 Refactoring extract local variable fails in nested if statements");
		helper1(10, 17, 10, 28, true, false, "temp", "string");
	}

	@Test
	public void test65() throws Exception {
//		printTestDisabledMessage("test for bug 35981 extract local variable causing exception [refactoring] ");
		helper1(6, 19, 6, 22, true, false, "temp", "bar2");
	}

	@Test
	public void test66() throws Exception {
		helper1(7, 32, 7, 33, true, false, "temp", "e2");
	}

	@Test
	public void test67() throws Exception {
//		printTestDisabledMessage("test for bug 37834");
		helper1(6, 16, 6, 21, true, false, "temp", "integer");
	}

	@Test
	public void test68() throws Exception {
//		printTestDisabledMessage("test for bug 37834");
		helper1(6, 14, 6, 21, true, false, "temp", "d2");
	}

	@Test
	public void test69() throws Exception {
		helper1(5, 24, 5, 26, true, false, "temp", "string2");
	}

	@Test
	public void test70() throws Exception {
//		printTestDisabledMessage("test for bug 40353");
		helper1(7, 28, 7, 42, true, true, "temp", "length");
	}

	@Test
	public void test71() throws Exception {
//		printTestDisabledMessage("test for bug 40353");
		helper1(8, 24, 8, 34, true, false, "temp", "string");
	}

	@Test
	public void test72() throws Exception {
//		printTestDisabledMessage("test for bug 40353");
		helper1(8, 32, 8, 33, true, false, "temp", "i2");
	}

	@Test
	public void test73() throws Exception {
//		printTestDisabledMessage("test for bug 40353");
		warningHelper1(6, 39, 6, 40, true, false, "temp", "i2", RefactoringStatus.WARNING);
		// (warning is superfluous, but detection would need flow analysis)
	}

	@Test
	public void test74() throws Exception {
//		printTestDisabledMessage("test for bug 40353");
		helper1(7, 36, 7, 49, true, false, "temp", "string");
	}

	@Test
	public void test75() throws Exception {
//		printTestDisabledMessage("test for bug 40353");
		helper1(7, 36, 7, 39, true, false, "temp", "j");
	}

	@Test
	public void test76() throws Exception {
//		printTestDisabledMessage("test for bug 40353");
		helper1(7, 48, 7, 49, true, false, "temp", "k2");
	}

	@Test
	public void test77() throws Exception {
//		printTestDisabledMessage("test for bug 40353: extracting fragment which matches for_initializer");
		helper1(10, 13, 10, 17, true, false, "temp", "f");
	}

	@Test
	public void test78() throws Exception {
//		printTestDisabledMessage("test for bug 50971: Extract temp: index out of bounds error [refactoring]");
		helper1(5, 21, 5, 27, true, false, "o2", "o");
	}

	@Test
	public void test79() throws Exception {
		helper1(10, 40, 10, 60, true, false, "strong", "string");
	}

	@Test
	public void test80() throws Exception {
		helper1(5, 37, 5, 45, true, false, "name", "string");
	}

	@Test
	public void test81() throws Exception {
		helper1(7, 18, 7, 23, true, false, "k", "const2");
	}

	@Test
	public void test82() throws Exception {
		helper1(5, 1, 6, 1, true, false, "one", "integer");
	}

	@Ignore("BUG_82166_ImportRewrite_context")
	@Test
	public void test83() throws Exception {
		helper1(7, 17, 7, 27, false, false, "temp", "test");
	}

	@Test
	public void test84() throws Exception {
		helper1(5, 16, 5, 17, false, false, "temp", "j");
	}

	@Ignore("BUG_82166_ImportRewrite_context")
	@Test
	public void test85() throws Exception {
		helper1(10, 22, 10, 32, true, true, "temp", "test2");
	}

	@Test
	public void test86() throws Exception {
//		printTestDisabledMessage("test for parameterized class instance creation");
		helper1(14, 22, 14, 37, true, true, "name", "a");
	}

	@Test
	public void test87() throws Exception {
//		printTestDisabledMessage("test for parameterized class instance creation");
		helper1(15, 17, 15, 27, true, true, "a2", "a2");
	}

	@Test
	public void test88() throws Exception {
		helper1(14, 14, 14, 19, true, false, "foo", "foo");
	}

	@Test
	public void test89() throws Exception {
		IPackageFragment a= getRoot().createPackageFragment("a", true,	null);
		ICompilationUnit aA= a.createCompilationUnit("A.java", "package a; public class A {}", true, null);
		aA.save(null, true);

		IPackageFragment b= getRoot().createPackageFragment("b", true,	null);
		ICompilationUnit bA= b.createCompilationUnit("A.java", "package b; public class A {}", true, null);
		bA.save(null, true);

		helper1(15, 7, 15, 15, true, false, "foo", "method");
	}

	@Test
	public void test90() throws Exception {
		helper1(8, 19, 8, 28, true, false, "temp", "number");
	}

	@Test
	public void test91() throws Exception {
		helper1(8, 19, 8, 28, true, false, "temp", "integer");
	}

	@Test
	public void test92() throws Exception { //bug 104293
		helper1(9, 32, 9, 44, true, false, "asList", "asList");
	}

	@Test
	public void test93() throws Exception { // syntax error
		helper1(6, 28, 6, 34, true, false, "bla", "string");
	}

	@Test
	public void test94() throws Exception {
		//test for bug 19851, syntax error
		helper1(6, 9, 6, 24, false, false, "temp", "string");
	}

	@Test
	public void test95() throws Exception {
		//test for bug 131556
		helper1(5, 23, 5, 33, true, false, "temp", "b");
	}

	@Test
	public void test96() throws Exception {
		//test for bug 71762
		helper1(6, 32, 6, 37, true, false, "isquared", "j");
	}

	@Test
	public void test97() throws Exception {
		//test for bug 48231
		helper1(10, 32, 10, 47, true, false, "temp", "nextElement");
	}

	@Test
	public void test98() throws Exception {
		//test for bug 132931
		helper1(8, 32, 8, 44, true, true, "temp", "string");
	}

	@Test
	public void test99() throws Exception {
		//test for bug 99963
		helper1(7, 32, 7, 36, true, false, "temp", "a");
	}

	@Ignore("BUG_161617_ASTRewrite_space")
	@Test
	public void test100() throws Exception {
		helper1(5, 28, 5, 40, true, false, "temp", "object");
	}

	@Test
	public void test101() throws Exception {
		helper1(9, 13, 9, 25, true, false, "temp", "object");
	}

	@Test
	public void test102() throws Exception {
		helper1(9, 24, 9, 29, true, false, "temp", "j");
	}

	@Test
	public void test103() throws Exception {
		//test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=243101
		helper1(7, 21, 7, 33, true, false, "temp", "valueOf");
	}

	@Test
	public void test104() throws Exception {
		//test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=307758
		helper1(6, 17, 6, 24, true, false, "temp", "i");
	}

	@Test
	public void test105() throws Exception {
		//test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=335173
		helper1(5, 21, 5, 26, true, false, "temp", "i");
	}

	@Test
	public void test106() throws Exception {
		//test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=335173
		helper1(5, 20, 5, 27, true, false, "temp", "i");
	}

	@Test
	public void test107() throws Exception {
		//test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=335173
		helper1(5, 22, 5, 27, true, false, "temp", "i");
	}

	@Test
	public void test108() throws Exception {
		//test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=335173
		helper1(5, 21, 5, 28, true, false, "temp", "i");
	}

	@Test
	public void test109() throws Exception {
		//test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=335173
		helper1(5, 20, 5, 29, true, false, "temp", "i");
	}

	@Test
	public void test110() throws Exception {
		//test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=65875
		helper1(6, 9, 6, 25, true, false, "temp", "calculateCount");
	}

	@Test
	public void test111() throws Exception {
		//test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=65875
		helper1(11, 9, 11, 25, true, false, "temp", "calculateCount");
	}

	@Test
	public void test113() throws Exception {
		//test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=406347
		helper1(7, 17, 7, 22, true, false, "temp", "i");
	}

	@Test
	public void test114() throws Exception {
		//test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=406347
		helper1(8, 17, 8, 22, true, false, "temp", "i");
	}

	@Test
	public void test115() throws Exception {
		//test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=406347
		helper1(7, 17, 7, 22, true, false, "temp", "i");
	}

	@Test
	public void test116() throws Exception {
		//test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=100430
		helper1(7, 16, 7, 28, true, false, "bar", "object");
	}

	@Test
	public void test117() throws Exception {
		//test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=377288
		helper1(8, 18, 8, 19, true, false, "temp", "j");
	}

	@Test
	public void test118() throws Exception {
		//test for https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/39
		helper1(8, 28, 8, 38, true, false, "length", "length");
	}

	@Test
	public void test119() throws Exception {
		//test for https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/39
		helper1(5, 28, 5, 38, true, false, "length", "length");
	}

	@Test
	public void test120() throws Exception {
		//test for https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/39
		helper1(7, 63, 7, 87, true, false, "charAt", "charAt");
	}

	@Test
	public void test121() throws Exception {
		//test for https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/39
		helper1(8, 44, 8, 58, true, false, "length", "length");
	}

	@Test
	public void test122() throws Exception {
		//test for https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/39
		helper1(5, 68, 5, 94, true, false, "intValue", "intValue");
	}

	@Test
	public void test123() throws Exception {
		//test for https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/39
		helper1(8, 28, 8, 38, true, false, "j", "j");
	}

	@Test
	public void test124() throws Exception {
		//test for https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/39
		helper1(8, 32, 8, 42, true, false, "length", "length");
	}

	@Test
	public void test125() throws Exception {
		//test for https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/39
		helper1(9, 32, 9, 42, true, false, "length", "length");
	}

	@Test
	public void testZeroLengthSelection0() throws Exception {
//		printTestDisabledMessage("test for bug 30146");
		helper1(4, 18, 4, 18, true, false, "temp", "j");
	}

	// -- testing failing preconditions
	@Test
	public void testFail0() throws Exception {
		failHelper1(5, 16, 5, 17, false, false, "temp", RefactoringStatus.ERROR);
	}

	@Test
	public void testFail1() throws Exception {
		failHelper1(4, 9, 5, 13, false, false, "temp", RefactoringStatus.FATAL);
	}

	@Test
	public void testFail2() throws Exception {
		failHelper1(4, 9, 4, 20, false, false, "temp", RefactoringStatus.FATAL);
	}

	@Test
	public void testFail3() throws Exception {
		failHelper1(4, 9, 4, 20, false, false, "temp", RefactoringStatus.FATAL);
	}

	@Test
	public void testFail4() throws Exception {
		failHelper1(5, 9, 5, 12, false, false, "temp", RefactoringStatus.FATAL);
	}

	@Test
	public void testFail5() throws Exception {
		failHelper1(3, 12, 3, 15, false, false, "temp", RefactoringStatus.FATAL);
	}

	@Test
	public void testFail6() throws Exception {
		failHelper1(4, 14, 4, 19, false, false, "temp", RefactoringStatus.FATAL);
	}

	@Test
	public void testFail7() throws Exception {
		failHelper1(4, 15, 4, 20, false, false, "temp", RefactoringStatus.FATAL);
	}

//	@Test
//	public void testFail8() throws Exception {
//		printTestDisabledMessage("removed");
//		failHelper1(5, 16, 5, 20, false, false, "temp", RefactoringStatus.FATAL);
//	}

	@Test
	public void testFail9() throws Exception {
		failHelper1(4, 19, 4, 23, false, false, "temp", RefactoringStatus.FATAL);
	}

	@Test
	public void testFail10() throws Exception {
		failHelper1(4, 33, 4, 39, false, false, "temp", RefactoringStatus.FATAL);
	}

	@Test
	public void testFail11() throws Exception {
//		printTestDisabledMessage("regression test for bug#13061");
		failHelper1(4, 18, 4, 19, false, false, "temp", RefactoringStatus.FATAL);
	}

// removed - allowe now (see bug 98847)
//	@Test
//	public void testFail12() throws Exception {
//		failHelper1(4, 16, 4, 29, false, false, "temp", RefactoringStatus.FATAL);
//	}

//removed
//	@Test
//	public void testFail13() throws Exception {
//		failHelper1(5, 16, 5, 20, false, false, "temp", RefactoringStatus.FATAL);
//	}

//removed
//	@Test
//	public void testFail14() throws Exception {
//		failHelper1(4, 16, 4, 22, false, false, "temp", RefactoringStatus.FATAL);
//	}

//removed
//	@Test
//	public void testFail15() throws Exception {
//		failHelper1(4, 19, 4, 22, false, false, "temp", RefactoringStatus.FATAL);
//	}

//removed - allowed now (see bug 21815)
//	@Test
//	public void testFail16() throws Exception {
//		failHelper1(5, 9, 5, 12, false, false, "temp", RefactoringStatus.FATAL);
//	}
//
//	@Test
//	public void testFail17() throws Exception {
//		failHelper1(6, 13, 6, 16, false, false, "temp", RefactoringStatus.FATAL);
//	}

	@Test
	public void testFail18() throws Exception {
//		printTestDisabledMessage("regression test for bug#8149");
//		printTestDisabledMessage("regression test for bug#37547");
		// test for bug 40353: is now FATAL"
		failHelper1(4, 27, 4, 28, false, false, "temp", RefactoringStatus.FATAL);
	}

	@Test
	public void testFail19() throws Exception {
//		printTestDisabledMessage("regression test for bug#8149");
		failHelper1(6, 16, 6, 18, false, false, "temp", RefactoringStatus.WARNING);
	}

	@Test
	public void testFail20() throws Exception {
//		printTestDisabledMessage("regression test for bug#13249");
		failHelper1(3, 9, 3, 41, false, false, "temp", RefactoringStatus.FATAL);
	}

//removed - allowed now (see bug 53243)
//	@Test
//	public void testFail21() throws Exception {
//		//test for bug 19851
//		failHelper1(6, 9, 6, 24, false, false, "temp", RefactoringStatus.FATAL);
//	}

	@Test
	public void testFail22() throws Exception {
//		printTestDisabledMessage("test for bug 21815");
		failHelper1(5, 9, 5, 12, false, false, "temp", RefactoringStatus.FATAL);
	}

	@Test
	public void testFail23() throws Exception {
//		printTestDisabledMessage("test for bug 24265");
		failHelper1(4, 13, 4, 14, false, false, "temp", RefactoringStatus.FATAL);
	}

	@Test
	public void testFail24() throws Exception {
//		printTestDisabledMessage("test for bug 24265");
		failHelper1(4, 13, 4, 14, false, false, "temp", RefactoringStatus.FATAL);
	}

	@Test
	public void testFail25() throws Exception {
		failHelper1(4, 16, 4, 18, false, false, "temp", RefactoringStatus.FATAL);
	}

	@Test
	public void testFail26() throws Exception {
		failHelper1(4, 15, 4, 20, false, false, "temp", RefactoringStatus.FATAL);
	}

	@Test
	public void testFail27() throws Exception {
//		printTestDisabledMessage("test for bug 29513");
		failHelper1(7, 13, 7, 24, true, false, "temp", RefactoringStatus.WARNING);
	}

	@Test
	public void testFail28() throws Exception {
//		printTestDisabledMessage("test for bug 29513");
		failHelper1(7, 17, 7, 28, true, false, "temp", RefactoringStatus.WARNING);
	}

	@Test
	public void testFail29() throws Exception {
//		printTestDisabledMessage("test for bug 40353: extracting for_updater referring to loop variable");
		failHelper1(5, 32, 5, 35, true, false, "temp", RefactoringStatus.FATAL);
	}

	@Test
	public void testFail30() throws Exception {
//		printTestDisabledMessage("test for bug 40353: extracting for_test referring to loop variable");
		failHelper1(5, 25, 5, 30, true, false, "temp", RefactoringStatus.FATAL);
	}

	@Test
	public void testFail31() throws Exception {
//		printTestDisabledMessage("test for bug 40353: extracting for_test referring to loop variable");
		failHelper1(5, 31, 5, 32, true, false, "temp", RefactoringStatus.FATAL);
	}

	@Test
	public void testFail32() throws Exception {
//		printTestDisabledMessage("test for bug 40353: extracting for_test referring to loop variable");
		failHelper1(6, 35, 6, 36, true, false, "temp", RefactoringStatus.FATAL);
	}

	@Test
	public void testFail33() throws Exception {
//		printTestDisabledMessage("test for bug 40353: extracting for_updater");
		failHelper1(6, 17, 6, 21, true, false, "temp", RefactoringStatus.FATAL);
	}

	@Test
	public void testFail34() throws Exception {
//		printTestDisabledMessage("test for bug 40353: extracting for_initializer");
		failHelper1(9, 20, 9, 24, true, false, "temp", RefactoringStatus.FATAL);
	}

	@Test
	public void testFail35() throws Exception {
//		printTestDisabledMessage("test for bug 45007: QualifiedName");
		failHelper1(6, 33, 6, 38, true, false, "temp", RefactoringStatus.FATAL);
	}

	@Test
	public void testFail36() throws Exception {
//		printTestDisabledMessage("test for bug 45007: FieldAccess");
		failHelper1(6, 33, 6, 38, true, false, "temp", RefactoringStatus.FATAL);
	}

	@Test
	public void testFail37() throws Exception {
//		printTestDisabledMessage("test for bug 45007: QualifiedName (nested)");
		failHelper1(5, 40, 5, 51, true, false, "temp", RefactoringStatus.FATAL);
	}

	@Test
	public void testFail38() throws Exception {
		failHelper1(4, 45, 4, 50, true, false, "temp", RefactoringStatus.FATAL);
	}

	@Test
	public void testFail39() throws Exception {
		//test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=100430
		failHelper1(7, 16, 7, 28, false, false, "bar", RefactoringStatus.WARNING);
	}

	@Test
	public void testFail40() throws Exception {
		//test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=100430
		failHelper1(7, 16, 7, 28, false, false, "bar", RefactoringStatus.WARNING);
	}
}
