/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.actions;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.SourceRange;

import org.eclipse.jdt.ui.tests.refactoring.AbstractJunit4SelectionTestCase;
import org.eclipse.jdt.ui.tests.refactoring.GenericRefactoringTest;
import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectEnclosingAction;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectNextAction;
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectPreviousAction;

public class StructureSelectionActionTests extends GenericRefactoringTest {
	private static final String REFACTORING_PATH= "StructureSelectionAction/";

	public StructureSelectionActionTests() {
		rts= new RefactoringTestSetup();
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private String getSimpleTestFileName(boolean input){
		StringBuilder fileName = new StringBuilder("A_").append(getName());
		fileName.append(input ? "": "_out");
		fileName.append(input ? ".java": ".txt");
		return fileName.toString();
	}

	private String getTestFileName(boolean input){
		return TEST_PATH_PREFIX + getRefactoringPath() + getSimpleTestFileName(input);
	}

	//------------
	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, boolean input) throws Exception {
		String cuName= getSimpleTestFileName(input);
		ICompilationUnit cu= pack.getCompilationUnit(cuName);
		if (cu.exists())
			return cu;
		return createCU(pack, cuName, getFileContents(getTestFileName(input)));
	}

	private ISourceRange getSelection(ICompilationUnit cu) throws Exception {
		String source= cu.getSource();
		int offset= source.indexOf(AbstractJunit4SelectionTestCase.SQUARE_BRACKET_OPEN);
		int end= source.indexOf(AbstractJunit4SelectionTestCase.SQUARE_BRACKET_CLOSE);
		return new SourceRange(offset, end - offset);
	}

	private void check(ICompilationUnit cu, ISourceRange newRange) throws IOException, JavaModelException {
		String expected= getFileContents(getTestFileName(false));
		String actual= cu.getSource().substring(newRange.getOffset(), newRange.getOffset() + newRange.getLength());
//		assertEquals("selection incorrect length", expected.length(), actual.length());
		assertEqualLines("selection incorrect", expected, actual);
	}

	private void helperSelectUp() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true);
		ISourceRange selection= getSelection(cu);

		ISourceRange newRange= new StructureSelectEnclosingAction().getNewSelectionRange(selection, cu);

		check(cu, newRange);
	}

	private void helperSelectUp(int startLine, int startColumn, int endLine, int endColumn) throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true);
		ISourceRange selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
		ISourceRange newRange= new StructureSelectEnclosingAction().getNewSelectionRange(selection, cu);

		check(cu, newRange);
	}

	private void helperSelectNext(int startLine, int startColumn, int endLine, int endColumn) throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true);
		ISourceRange selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);

		ISourceRange newRange= new StructureSelectNextAction().getNewSelectionRange(selection, cu);
		check(cu, newRange);
	}

	private void helperSelectPrevious(int startLine, int startColumn, int endLine, int endColumn) throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true);
		ISourceRange selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);

		ISourceRange newRange= new StructureSelectPreviousAction().getNewSelectionRange(selection, cu);
		check(cu, newRange);
	}

	private void helperZeroLength(int line, int column) throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true);
		ISourceRange selection= new SourceRange(TextRangeUtil.getOffset(cu, line, column), 1);

		//DebugUtils.dump(name() + ":<" + cu.getSource().substring(selection.getOffset()) + "/>");

		ISourceRange newRange= new StructureSelectEnclosingAction().getNewSelectionRange(selection, cu);
		check(cu, newRange);
	}

	private void offsetTest(int line, int column, int expected) throws Exception {
		String filePath= TEST_PATH_PREFIX + getRefactoringPath() + "OffsetTest.java";
		ICompilationUnit cu= createCU(getPackageP(), "OffsetTest.java", getFileContents(filePath));
		assertEquals("incorrect offset", expected, TextRangeUtil.getOffset(cu, line, column));
	}


	// ---- tests ---

	@Test
	public void test0() throws Exception {
		helperSelectUp(4, 9, 4, 13);
	}

	@Test
	public void test1() throws Exception {
		helperSelectUp();
	}

	@Test
	public void test2() throws Exception {
		helperSelectUp(4, 16, 4, 21);
	}

	@Test
	public void test3() throws Exception {
		helperSelectUp(4, 9, 4, 21);
	}

	@Test
	public void test4() throws Exception {
		helperSelectUp();
	}

	@Test
	public void test5() throws Exception {
		helperSelectUp();
	}

	@Test
	public void test6() throws Exception {
		helperSelectUp();
	}

	@Test
	public void test7() throws Exception {
		//helper1();
		helperSelectUp(3, 10, 3, 14);
	}

	@Test
	public void test8() throws Exception {
		helperSelectUp(3, 16, 3, 18);
	}

	@Test
	public void test9() throws Exception {
		helperSelectUp(3, 10, 3, 11);
	}

	@Test
	public void test10() throws Exception {
		helperSelectUp(4, 18, 4, 21);
	}

	@Test
	public void test11() throws Exception {
		helperSelectUp(4, 20, 4, 21);
	}

	@Test
	public void test12() throws Exception {
		helperSelectUp(4, 16, 4, 19);
	}

	@Test
	public void test13() throws Exception {
		helperSelectUp(4, 13, 4, 16);
	}

	@Test
	public void test14() throws Exception {
		helperSelectUp(4, 16, 4, 21);
	}

	@Test
	public void test15() throws Exception {
		// identical to test9 ???
		helperSelectUp(3, 10, 3, 11);
	}

	@Test
	public void test16() throws Exception {
		helperSelectUp(3, 16, 3, 17);
	}

	@Test
	public void test17() throws Exception {
		helperSelectUp(3, 5, 7, 6);
	}

	@Test
	public void test18() throws Exception {
		helperSelectUp(3, 5, 4, 6);
	}

	@Test
	public void test19() throws Exception {
		helperSelectUp(7, 14, 7, 16);
	}

	@Test
	public void test20() throws Exception {
		helperSelectUp(4, 18, 4, 19);
	}

	@Test
	public void test21() throws Exception {
		//regression test for bug#10182
		//printTestDisabledMessage("regression test for bug#11151");
		helperSelectNext(3, 21, 3, 28);
	}

	@Test
	public void test22() throws Exception {
		//regression test for bug#10182
		//printTestDisabledMessage("regression test for bug#11151");
		helperSelectPrevious(3, 21, 3, 28);
	}

	@Test
	public void test23() throws Exception {
//		printTestDisabledMessage("regression test for bug#10570");
		helperSelectPrevious(5, 30, 7, 10);
	}

	@Test
	public void test24() throws Exception {
		//regression test for bug#11424
		helperSelectPrevious(3, 13, 5, 6);
	}

	@Test
	public void test25() throws Exception {
		//regression test for bug#11879
		helperSelectNext(5, 5, 6, 6);
	}

	@Test
	public void test26() throws Exception {
		//regression test for bug#11879
		helperSelectPrevious(5, 5, 6, 6);
	}

	@Test
	public void test27() throws Exception {
		//regression test for bug#11879
		helperSelectNext(4, 1, 4, 10);
	}

	@Test
	public void test28() throws Exception {
		//regression test for bug#11879
		helperSelectPrevious(4, 1, 4, 10);
	}

	@Test
	public void test29() throws Exception {
//		printTestDisabledMessage("regression test for bug#16051");
		helperSelectUp(5, 13, 5, 17);
	}

	@Test
	public void test30() throws Exception {
//		printTestDisabledMessage("regression test for bug#80345 (not 22082)");
		helperSelectUp(3, 10, 3, 10);
	}

	@Test
	public void test31() throws Exception {
//		printTestDisabledMessage("regression test for bug#80345 (not 22082)");
		helperSelectUp(3, 10, 3, 10);
	}

	@Test
	public void test32() throws Exception {
//		printTestDisabledMessage("regression test for bug#22939");
		helperSelectUp(4, 18, 4, 18);
	}

	@Test
	public void test33() throws Exception {
//		printTestDisabledMessage("regression test for bug#22939");
		helperSelectUp(5, 23, 5, 23);
	}

	@Test
	public void test34() throws Exception {
//		printTestDisabledMessage("regression test for bug#23118");
		helperSelectUp(5, 14, 5, 14);
	}

	@Test
	public void test35() throws Exception {
//		printTestDisabledMessage("regression test for bug#23118");
		helperSelectUp(5, 14, 5, 14);
	}

	@Test
	public void test36() throws Exception {
//		printTestDisabledMessage("regression test for bug#23259");
		helperSelectUp(5, 14, 5, 14);
	}

	@Test
	public void test37() throws Exception {
//		printTestDisabledMessage("regression test for bug#23259");
		helperSelectUp(7, 14, 7, 14);
	}

	@Test
	public void test38() throws Exception {
//		printTestDisabledMessage("regression test for bug#23263");
		helperSelectPrevious(4, 5, 5, 16);
	}

	@Test
	public void test39() throws Exception {
//		printTestDisabledMessage("regression test for bug#23464");
		helperSelectPrevious(6, 13, 6, 20);
	}

	@Test
	public void test40() throws Exception {
//		printTestDisabledMessage("regression test for bug#23464 ");
		helperSelectPrevious(7, 13, 7, 20);
	}

	@Test
	public void test41() throws Exception {
		helperSelectPrevious(4, 1, 4, 29);
	}

	@Test
	public void test42() throws Exception {
		helperSelectNext(4, 1, 4, 29);
	}

	@Test
	public void test43() throws Exception {
		helperSelectNext(4, 1, 4, 32);
	}

	@Test
	public void testZeroLength0() throws Exception {
		//printTestDisabledMessage("");
		helperZeroLength(4, 20);
	}

	@Test
	public void testZeroLength1() throws Exception {
		helperSelectNext(4, 16, 4, 16);
		helperSelectPrevious(4, 17, 4, 17);
	}

	@Test
	public void testZeroLength2() throws Exception {
		helperSelectNext(4, 20, 4, 20);
		helperSelectPrevious(4, 21, 4, 21);
	}

	@Test
	public void testZeroLength3() throws Exception {
		helperSelectNext(3, 10, 3, 10);
		helperSelectPrevious(3, 11, 3, 11);
	}

	@Test
	public void testZeroLength4() throws Exception {
		helperSelectNext(4, 9, 4, 9);
		helperSelectPrevious(4, 10, 4, 10);
	}

	@Test
	public void testZeroLength5() throws Exception {
		helperSelectNext(4, 11, 4, 11);
		helperSelectPrevious(4, 14, 4, 14);
	}

	@Test
	public void testWholeCu() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true);
		ISourceRange selection= cu.getSourceRange();

		ISourceRange newRange= new StructureSelectEnclosingAction().getNewSelectionRange(selection, cu);

		String expected= getFileContents(getTestFileName(false));
		String actual= cu.getSource().substring(newRange.getOffset(), newRange.getOffset() + newRange.getLength());
		assertEqualLines("selection incorrect", expected, actual);
	}

	//--- offset calculation tests

	@Test
	public void testOffset0() throws Exception {
		offsetTest(4, 20, 44);
	}

	@Test
	public void testOffset1() throws Exception {
		offsetTest(5, 9, 49);
	}

	@Test
	public void testOffset2() throws Exception {
		offsetTest(7, 13, 75);
	}

	@Test
	public void testTabCount0(){
		int t= TextRangeUtil.calculateTabCountInLine("\t\t1", 9);
		assertEquals(2, t);
	}

	@Test
	public void testTabCount1(){
		int t= TextRangeUtil.calculateTabCountInLine("\t\tint i= 1 + 1;", 20);
		assertEquals(2, t);
	}

	@Test
	public void testTabCount2(){
		int t= TextRangeUtil.calculateTabCountInLine("\t\t\treturn;", 13);
		assertEquals(3, t);
	}

	@Test
	public void testTabCount3(){
		int t= TextRangeUtil.calculateTabCountInLine("\tvoid m(){m();", 18);
		assertEquals(1, t);
	}
}
