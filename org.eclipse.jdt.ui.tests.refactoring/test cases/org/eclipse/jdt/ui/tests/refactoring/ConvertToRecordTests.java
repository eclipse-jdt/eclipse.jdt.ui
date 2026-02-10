/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation based on InlineTempTests
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.SourceRange;

import org.eclipse.jdt.internal.corext.refactoring.code.ConvertToRecordRefactoring;

import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;
import org.eclipse.jdt.ui.tests.refactoring.rules.Java14Setup;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

public class ConvertToRecordTests extends GenericRefactoringTest {
	private static final String REFACTORING_PATH= "ConvertToRecord/";

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	public ConvertToRecordTests() {
		super(new Java14Setup());
	}

	protected ConvertToRecordTests(RefactoringTestSetup rts) {
		super(rts);
	}

	protected String getSimpleTestFileName(boolean canConvert, boolean input){
		StringBuilder fileName = new StringBuilder("A_").append(getName());
		if (canConvert)
			fileName.append(input ? "_in": "_out");
		return fileName.append(".java").toString();
	}

	protected String getTestFileName(boolean canConvert, boolean input){
		StringBuilder fileName= new StringBuilder(TEST_PATH_PREFIX).append(getRefactoringPath());
		fileName.append(canConvert ? "canConvert/": "cannotConvert/");
		return fileName.append(getSimpleTestFileName(canConvert, input)).toString();
	}

	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, boolean canConvert, boolean input) throws Exception {
		return createCU(pack, getSimpleTestFileName(canConvert, input), getFileContents(getTestFileName(canConvert, input)));
	}

	private ISourceRange getSelection(ICompilationUnit cu) throws Exception{
		String source= cu.getSource();
		int offset= source.indexOf(AbstractJunit4SelectionTestCase.SQUARE_BRACKET_OPEN);
		int end= source.indexOf(AbstractJunit4SelectionTestCase.SQUARE_BRACKET_CLOSE);
		return new SourceRange(offset, end - offset);
	}

	private void helper1(ICompilationUnit cu, ISourceRange selection) throws Exception{
		ConvertToRecordRefactoring ref= new ConvertToRecordRefactoring(cu, selection.getOffset(), selection.getLength());
		RefactoringStatus result= performRefactoring(ref);
		assertNull("precondition was supposed to pass", result);

		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String newCuName= getSimpleTestFileName(true, true);
		ICompilationUnit newcu= pack.getCompilationUnit(newCuName);
		assertTrue(newCuName + " does not exist", newcu.exists());
		assertEqualLines("incorrect conversion", getFileContents(getTestFileName(true, false)), newcu.getSource());
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
		ConvertToRecordRefactoring ref= new ConvertToRecordRefactoring(cu, selection.getOffset(), selection.getLength());
		RefactoringStatus result= performRefactoring(ref);
		assertNotNull("precondition was supposed to fail", result);
	}

	protected void helper2(int startLine, int startColumn, int endLine, int endColumn) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), false, true);
		ISourceRange selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
		helper2(cu, selection);
	}

	private void helper3(ICompilationUnit cu, ISourceRange selection, int status) throws Exception{
		ConvertToRecordRefactoring ref= new ConvertToRecordRefactoring(cu, selection.getOffset(), selection.getLength());
		RefactoringStatus result= ref.checkAllConditions(new NullProgressMonitor());
		assertEquals(status, result.getSeverity());
	}

	protected void helper3(int startLine, int startColumn, int endLine, int endColumn, int severity) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), false, true);
		ISourceRange selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
		helper3(cu, selection, severity);
	}


	//--- tests

	@Test
	public void test0() throws Exception {
		helper1(8, 12, 8, 13);
	}

	@Test
	public void test2() throws Exception {
		helper1(12, 16, 12, 21);
	}

	@Test
	public void test3() throws Exception {
		helper1(12, 16, 12, 21);
	}

	@Test
	public void testFail0() throws Exception {
		helper2(12, 16, 12, 21);
	}

	@Test
	public void testFail2() throws Exception {
		helper2(12, 16, 12, 21);
	}

	@Test
	public void testFail3() throws Exception {
		helper2(12, 16, 12, 21);
	}

	@Test
	public void testFail4() throws Exception {
		helper2(12, 16, 12, 21);
	}

	@Test
	public void testFail5() throws Exception {
		helper2(12, 16, 12, 21);
	}

	@Test
	public void testFail6() throws Exception {
		helper3(12, 16, 12, 21, IStatus.WARNING);
	}

	@Test
	public void testFail7() throws Exception {
		helper2(15, 16, 15, 21);
	}

	@Test
	public void testFail8() throws Exception {
		helper2(8, 18, 8, 23);
	}

}
