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
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.actions;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;

import org.eclipse.jdt.ui.tests.refactoring.GenericRefactoringTest;
import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.GoToNextPreviousMemberAction;

public class GoToNextPreviousMemberActionTests extends GenericRefactoringTest{
	private static final String REFACTORING_PATH= "GoToNextPreviousMemberAction/";

	public GoToNextPreviousMemberActionTests() {
		rts= new RefactoringTestSetup();
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private String getSimpleTestFileName(){
		return "A_" + getName() + ".java";
	}

	private String getTestFileName(){
		return TEST_PATH_PREFIX + getRefactoringPath() + getSimpleTestFileName();
	}

	//------------
	protected ICompilationUnit createCUfromTestFile() throws Exception {
		return createCU(getPackageP(), getSimpleTestFileName(), getFileContents(getTestFileName()));
	}

	private void helper(int startLine, int startColumn, int endLine, int endColumn,
											int expectedStartLine, int expectedStartColumn, boolean isSelectNext) throws Exception {
		ICompilationUnit cu= createCUfromTestFile();
		ISourceRange selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
		ISourceRange actualNewRange= new GoToNextPreviousMemberAction(isSelectNext).getNewSelectionRange(selection, cu.getAllTypes());
		ISourceRange expectedNewRange= TextRangeUtil.getSelection(cu, expectedStartLine, expectedStartColumn, expectedStartLine, expectedStartColumn);
		assertEquals("incorrect selection offset", expectedNewRange.getOffset(), actualNewRange.getOffset());
		assertEquals("incorrect selection length", expectedNewRange.getLength(), actualNewRange.getLength());
	}

	private void helperNext(int startLine, int startColumn, int expectedStartLine, int expectedStartColumn) throws Exception{
		helper(startLine, startColumn, startLine, startColumn, expectedStartLine, expectedStartColumn, true);
	}

	private void helperPrevious(int startLine, int startColumn, int expectedStartLine, int expectedStartColumn) throws Exception{
		helper(startLine, startColumn, startLine, startColumn, expectedStartLine, expectedStartColumn, false);
	}

	//----
	@Test
	public void testPrevious0() throws Exception{
		helperPrevious(6, 5, 5, 11);
	}

	@Test
	public void testPrevious1() throws Exception{
		helperPrevious(8, 5, 7, 6);
	}

	@Test
	public void testPrevious2() throws Exception{
		helperPrevious(3, 1, 3, 1);
	}

	@Test
	public void testPrevious3() throws Exception{
		helperPrevious(15, 9, 13, 6);
	}

	@Test
	public void testPrevious4() throws Exception{
		helperPrevious(19, 1, 18, 9);
	}

	@Test
	public void testPrevious5() throws Exception{
		helperPrevious(31, 10, 27, 10);
	}

	@Test
	public void testPrevious6() throws Exception{
		helperPrevious(35, 3, 34, 2);
	}

	@Test
	public void testNext0() throws Exception{
		helperNext(3, 1, 4, 7);
	}

	@Test
	public void testNext1() throws Exception{
		helperNext(27, 10, 31, 10);
	}

	@Test
	public void testNext2() throws Exception{
		helperNext(35, 2, 35, 2);
	}

	@Test
	public void testNext3() throws Exception{
		helperNext(19, 1, 20, 13);
	}

}
