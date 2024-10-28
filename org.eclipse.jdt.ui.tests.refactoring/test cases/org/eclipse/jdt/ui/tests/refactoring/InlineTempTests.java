/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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
 *     Nikolay Metchev <nikolaymetchev@gmail.com> - [inline] Inline local variable with initializer generates assignment where left-hand side is not a variable - https://bugs.eclipse.org/394721
 *     Pierre-Yves B. <pyvesdev@gmail.com> - [inline] Allow inlining of local variable initialized to null. - https://bugs.eclipse.org/93850
 *     Pierre-Yves B. <pyvesdev@gmail.com> - [inline] Inlining a local variable leads to ambiguity with overloaded methods - https://bugs.eclipse.org/434747
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.SourceRange;

import org.eclipse.jdt.internal.corext.refactoring.code.InlineTempRefactoring;

import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

public class InlineTempTests extends GenericRefactoringTest {
	private static final String REFACTORING_PATH= "InlineTemp/";

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	public InlineTempTests() {
		super(new RefactoringTestSetup());
	}

	protected InlineTempTests(RefactoringTestSetup rts) {
		super(rts);
	}

	protected String getSimpleTestFileName(boolean canInline, boolean input){
		StringBuilder fileName = new StringBuilder("A_").append(getName());
		if (canInline)
			fileName.append(input ? "_in": "_out");
		return fileName.append(".java").toString();
	}

	protected String getTestFileName(boolean canInline, boolean input){
		StringBuilder fileName= new StringBuilder(TEST_PATH_PREFIX).append(getRefactoringPath());
		fileName.append(canInline ? "canInline/": "cannotInline/");
		return fileName.append(getSimpleTestFileName(canInline, input)).toString();
	}

	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, boolean canInline, boolean input) throws Exception {
		return createCU(pack, getSimpleTestFileName(canInline, input), getFileContents(getTestFileName(canInline, input)));
	}

	private ISourceRange getSelection(ICompilationUnit cu) throws Exception{
		String source= cu.getSource();
		int offset= source.indexOf(AbstractJunit4SelectionTestCase.SQUARE_BRACKET_OPEN);
		int end= source.indexOf(AbstractJunit4SelectionTestCase.SQUARE_BRACKET_CLOSE);
		return new SourceRange(offset, end - offset);
	}

	private void helper1(ICompilationUnit cu, ISourceRange selection) throws Exception{
		InlineTempRefactoring ref= new InlineTempRefactoring(cu, selection.getOffset(), selection.getLength());
		if (ref.checkIfTempSelected().hasFatalError())
			ref= null;
		RefactoringStatus result= performRefactoring(ref);
		assertNull("precondition was supposed to pass", result);

		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String newCuName= getSimpleTestFileName(true, true);
		ICompilationUnit newcu= pack.getCompilationUnit(newCuName);
		assertTrue(newCuName + " does not exist", newcu.exists());
		assertEqualLines("incorrect inlining", getFileContents(getTestFileName(true, false)), newcu.getSource());
	}

	protected void helper1(int startLine, int startColumn, int endLine, int endColumn) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		ISourceRange selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
		helper1(cu, selection);
	}

	protected void helper2() throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), false, true);
		helper2(cu, getSelection(cu));
	}

	private void helper2(ICompilationUnit cu, ISourceRange selection) throws Exception{
		InlineTempRefactoring ref= new InlineTempRefactoring(cu, selection.getOffset(), selection.getLength());
		if (ref.checkIfTempSelected().hasFatalError())
			ref= null;
		if (ref != null){
			RefactoringStatus result= performRefactoring(ref);
			assertNotNull("precondition was supposed to fail", result);
		}
	}

	protected void helper2(int startLine, int startColumn, int endLine, int endColumn) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), false, true);
		ISourceRange selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
		helper2(cu, selection);
	}


	//--- tests

	@Test
	public void test0() throws Exception{
		helper1(4, 9, 4, 18);
	}

	@Test
	public void test1() throws Exception{
		helper1(4, 9, 4, 18);
	}

	@Test
	public void test2() throws Exception{
		helper1(4, 9, 4, 18);
	}

	@Test
	public void test3() throws Exception{
		helper1(4, 9, 4, 22);
	}

	@Test
	public void test4() throws Exception{
		helper1(4, 9, 4, 22);
	}

	@Test
	public void test5() throws Exception{
		helper1(4, 9, 4, 22);
	}

	@Test
	public void test6() throws Exception{
		//printTestDisabledMessage("bug#6429 declaration source start incorrect on local variable");
		helper1(9, 13, 9, 14);
	}

	@Test
	public void test7() throws Exception{
		helper1(9, 9, 9, 18);
	}

	@Test
	public void test8() throws Exception{
		//printTestDisabledMessage("bug#6429 declaration source start incorrect on local variable");
		helper1(5, 13, 5, 14);
	}

	@Test
	public void test9() throws Exception{
		helper1(5, 9, 5, 21);
	}

	@Test
	public void test10() throws Exception{
//		printTestDisabledMessage("regression test for bug#9001");
		helper1(4, 21, 4, 25);
	}

	@Test
	public void test11() throws Exception{
		helper1(5, 21, 5, 25);
	}

	@Test
	public void test12() throws Exception{
		helper1(5, 15, 5, 19);
	}

	@Test
	public void test13() throws Exception{
		helper1(5, 17, 5, 18);
	}

	@Test
	public void test14() throws Exception{
//		printTestDisabledMessage("regression for bug 11664");
		helper1(4, 13, 4, 14);
	}

	@Test
	public void test15() throws Exception{
//		printTestDisabledMessage("regression for bug 11664");
		helper1(4, 19, 4, 20);
	}

	@Test
	public void test16() throws Exception{
//		printTestDisabledMessage("regression test for 10751");
		helper1(5, 17, 5, 24);
	}

	@Test
	public void test17() throws Exception{
//		printTestDisabledMessage("regression test for 12200");
		helper1(8, 18, 8, 21);
	}

	@Test
	public void test18() throws Exception{
//		printTestDisabledMessage("regression test for 12200");
		helper1(6, 18, 6, 21);
	}

	@Test
	public void test19() throws Exception{
//		printTestDisabledMessage("regression test for 12212");
		helper1(6, 19, 6, 19);
	}

	@Test
	public void test20() throws Exception{
//		printTestDisabledMessage("regression test for 16054");
		helper1(4, 17, 4, 18);
	}

	@Test
	public void test21() throws Exception{
//		printTestDisabledMessage("regression test for 17479");
		helper1(6, 20, 6, 25);
	}

	@Test
	public void test22() throws Exception{
//		printTestDisabledMessage("regression test for 18284");
		helper1(5, 13, 5, 17);
	}

	@Test
	public void test23() throws Exception{
//		printTestDisabledMessage("regression test for 22938");
		helper1(5, 16, 5, 20);
	}

	@Test
	public void test24() throws Exception{
//		printTestDisabledMessage("regression test for 26242");
		helper1(5, 19, 5, 24);
	}

	@Test
	public void test25() throws Exception{
//		printTestDisabledMessage("regression test for 26242");
		helper1(5, 19, 5, 24);
	}

	@Test
	public void test26() throws Exception{
		helper1(5, 17, 5, 24);
	}

	@Test
	public void test27() throws Exception{
		helper1(5, 22, 5, 29);
	}

	@Test
	public void test28() throws Exception{
		helper1(11, 14, 11, 21);
	}

	@Test
	public void test29() throws Exception{
		helper1(4, 8, 4, 11);
	}

	@Test
	public void test30() throws Exception{
		helper1(4, 8, 4, 11);
	}

	@Test
	public void test31() throws Exception{
		helper1(8, 30, 8, 30);
	}

	@Test
	public void test32() throws Exception{
		helper1(10, 27, 10, 27);
	}

	@Test
	public void test33() throws Exception{
		// add explicit cast for primitive types: https://bugs.eclipse.org/bugs/show_bug.cgi?id=46216
		helper1(5, 14, 5, 15);
	}

	@Test
	public void test34() throws Exception{
		// add explicit cast for boxing: https://bugs.eclipse.org/bugs/show_bug.cgi?id=201434#c4
		helper1(5, 17, 5, 17);
	}

	@Test
	public void test35() throws Exception{
		// add explicit cast for unchecked conversion: https://bugs.eclipse.org/bugs/show_bug.cgi?id=201434#c0
		helper1(7, 32, 7, 36);
	}

	@Test
	public void test36() throws Exception{
		// parenthesize complex cast expression
		helper1(6, 21, 6, 24);
	}

	@Test
	public void test37() throws Exception{
		// parameterized method invocation needs class expression: https://bugs.eclipse.org/bugs/show_bug.cgi?id=277968
		helper1(5, 16, 5, 17);
	}

	@Test
	public void test38() throws Exception{
		// parameterized method invocation needs this expression: https://bugs.eclipse.org/bugs/show_bug.cgi?id=277968
		helper1(5, 16, 5, 17);
	}

	@Test
	public void test39() throws Exception{
		// parameterized method invocation needs to keep super expression: https://bugs.eclipse.org/bugs/show_bug.cgi?id=277968
		helper1(5, 16, 5, 17);
	}

	@Test
	public void test40() throws Exception{
		// better cast for unboxing: https://bugs.eclipse.org/bugs/show_bug.cgi?id=297868
		helper1(5, 43, 5, 46);
	}

	@Test
	public void test41() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=335173
		helper1(5, 13, 5, 14);
	}

	@Test
	public void test42() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=335173
		helper1(5, 13, 5, 14);
	}

	@Test
	public void test43() throws Exception {
		// parenthesize complex cast expression https://bugs.eclipse.org/bugs/show_bug.cgi?id=335173
		helper1(6, 21, 6, 24);
	}

	@Test
	public void test44() throws Exception {
		// don't add unnecessary cast to wildcard-parameterized type https://bugs.eclipse.org/bugs/show_bug.cgi?id=338271
		helper1(7, 35, 7, 41);
	}

	@Test
	public void test45() throws Exception {
		// don't delete comment right before the local variable declaration (bug 295200)
		helper1(5, 18, 5, 22);
	}

	@Test
	public void test46() throws Exception {
		// don't delete comment right after the local variable declaration (bug 318471)
		helper1(5, 16, 5, 17);
	}

	@Test
	public void test47() throws Exception {
		// don't delete comment right before and after the local variable declaration (bug 295200)
		helper1(5, 18, 5, 22);
	}

	@Test
	public void test48() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=388078
		helper1(6, 16, 6, 18);
	}

	@Test
	public void test49() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=394721
		helper1(5, 15, 5, 16);
	}

	@Test
	public void test50() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=93850
		helper1(4, 17, 4, 18);
	}

	@Test
	public void test51() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=434747
		helper1(4, 16, 4, 17);
	}

	@Test
	public void test52() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=434747
		helper1(4, 14, 4, 15);
	}

	@Test
	public void test53() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=367536
		helper1(12, 13, 12, 14);
	}

	@Test
	public void test54() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=367536
		helper1(7, 13, 7, 14);
	}

	@Test
	public void test55() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=367536
		helper1(11, 13, 11, 14);
	}

	@Test
	public void test56() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=367536
		helper1(12, 13, 12, 14);
	}

	@Test
	public void test57() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=367536
		helper1(9, 26, 9, 27);
	}

	@Test
	public void test58() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=367536
		helper1(9, 26, 9, 27);
	}

	@Test
	public void test59() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=367536
		helper1(11, 11, 11, 12);
	}

	@Test
	public void test60() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=367536
		helper1(7, 13, 7, 14);
	}

	@Test
	public void test61() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=367536
		helper1(9, 13, 9, 14);
	}

	@Test
	public void test62() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=367536
		helper1(14, 13, 14, 14);
	}

	@Test
	public void test63() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=367536
		helper1(11, 13, 11, 14);
	}

	@Test
	public void test64() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=367536
		helper1(7, 13, 7, 14);
	}

	@Test
	public void test65() throws Exception {
		//https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/1705
		helper1(6, 13, 6, 19);
	}

	//------

	@Ignore("compile errors are ok now")
	@Test
	public void testFail0() throws Exception{
		helper2();
	}

	@Ignore("compile errors are ok now")
	@Test
	public void testFail1() throws Exception{
			helper2();
	}

	@Test
	public void testFail2() throws Exception{
		helper2();
	}

	@Test
	public void testFail3() throws Exception{
		helper2();
	}

	@Test
	public void testFail4() throws Exception{
		helper2();
	}

	@Test
	public void testFail5() throws Exception{
		helper2();
	}

	@Test
	public void testFail6() throws Exception{
		helper2();
	}

	@Test
	public void testFail7() throws Exception{
		helper2();
	}

	@Test
	public void testFail8() throws Exception{
		helper2();
	}

	@Test
	public void testFail9() throws Exception{
		//test for 16737
		helper2(3, 9, 3, 13);
	}

	@Test
	public void testFail10() throws Exception{
		//test for 16737
		helper2(3, 5, 3, 17);
	}

	@Test
	public void testFail11() throws Exception{
		//test for 17253
		helper2(8, 14, 8, 18);
	}

	@Ignore("compile errors are ok now")
	@Test
	public void testFail12() throws Exception{
		//test for 19851
		helper2(10, 16, 10, 19);
	}

	@Ignore("12106")
	@Test
	public void testFail13() throws Exception{
		helper2(4, 18, 4, 19);
	}

	@Ignore("https://bugs.eclipse.org/bugs/show_bug.cgi?id=93850")
	@Test
	public void testFail14() throws Exception {
		helper2(5, 17, 5, 18);
	}

	@Ignore("https://bugs.eclipse.org/bugs/show_bug.cgi?id=367536")
	public void testFail15() throws Exception {
		helper2(12, 13, 12, 14);
	}

}
