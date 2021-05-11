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
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;

import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;

import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

public class RenameTempTests extends GenericRefactoringTest{
	private static final String REFACTORING_PATH= "RenameTemp/";

	public RenameTempTests() {
		rts= new RefactoringTestSetup();
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private String getSimpleTestFileName(boolean canRename, boolean input){
		StringBuilder fileName = new StringBuilder("A_").append(getName());
		if (canRename)
			fileName.append(input ? "_in": "_out");
		return fileName.append(".java").toString();
	}

	private String getTestFileName(boolean canRename, boolean input){
		StringBuilder fileName= new StringBuilder(TEST_PATH_PREFIX).append(getRefactoringPath());
		fileName.append(canRename ? "canRename/": "cannotRename/");
		return fileName.append(getSimpleTestFileName(canRename, input)).toString();
	}

	//------------
	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, boolean canRename, boolean input) throws Exception {
		return createCU(pack, getSimpleTestFileName(canRename, input), getFileContents(getTestFileName(canRename, input)));
	}

	private ISourceRange getSelection(ICompilationUnit cu) throws Exception{
		String source= cu.getSource();
		//Warning: this *includes* the SQUARE_BRACKET_OPEN!
		int offset= source.indexOf(AbstractJunit4SelectionTestCase.SQUARE_BRACKET_OPEN);
		int end= source.indexOf(AbstractJunit4SelectionTestCase.SQUARE_BRACKET_CLOSE);
		return new SourceRange(offset, end - offset);
	}

	private void helper1(String newName, boolean updateReferences, ISourceRange selection, ICompilationUnit cu) throws Exception {
		IJavaElement[] elements= cu.codeSelect(selection.getOffset(), selection.getLength());
		assertEquals(1, elements.length);
		assertTrue(elements[0].getClass().toString(), elements[0] instanceof ILocalVariable);

		final RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_LOCAL_VARIABLE);
		descriptor.setJavaElement(elements[0]);
		descriptor.setNewName(newName);
		descriptor.setUpdateReferences(updateReferences);

		final RefactoringStatus status= new RefactoringStatus();
		final Refactoring refactoring= descriptor.createRefactoring(status);
		assertTrue("status should be ok", status.isOK());
		assertNotNull("refactoring should not be null", refactoring);

		RefactoringStatus result= performRefactoring(refactoring);
		assertNull("precondition was supposed to pass", result);

		IPackageFragment pack= (IPackageFragment) cu.getParent();
		String newCuName= getSimpleTestFileName(true, true);
		ICompilationUnit newcu= pack.getCompilationUnit(newCuName);
		assertTrue(newCuName + " does not exist", newcu.exists());
		assertEqualLines("incorrect renaming", getFileContents(getTestFileName(true, false)), newcu.getSource());
	}

	private void helper1(String newName, boolean updateReferences) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		helper1(newName, updateReferences, getSelection(cu), cu);
	}

	private void helper1(String newName, boolean updateReferences, int startLine, int startColumn, int endLine, int endColumn) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		ISourceRange selection= TextRangeUtil.getSelection(cu, startLine, startColumn, endLine, endColumn);
		helper1(newName, updateReferences, selection, cu);
	}

	private void helper1(String newName) throws Exception{
		helper1(newName, true);
	}

	private void failHelperNoElement() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), false, true);
		ISourceRange selection= getSelection(cu);
		IJavaElement[] elements= cu.codeSelect(selection.getOffset(), selection.getLength());
		assertEquals(0, elements.length);
	}

	private void failTestHelper(String newName, boolean updateReferences, ICompilationUnit cu, ISourceRange selection) throws Exception {
		IJavaElement[] elements= cu.codeSelect(selection.getOffset(), selection.getLength());
		assertEquals(1, elements.length);
		assertTrue(elements[0].getClass().toString(), elements[0] instanceof ILocalVariable);

		final RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_LOCAL_VARIABLE);
		descriptor.setJavaElement(elements[0]);
		descriptor.setNewName(newName);
		descriptor.setUpdateReferences(updateReferences);

		final RefactoringStatus status= new RefactoringStatus();
		final Refactoring refactoring= descriptor.createRefactoring(status);
		assertTrue("status should be ok", status.isOK());
		assertNotNull("refactoring should not be null", refactoring);

		RefactoringStatus result= performRefactoring(refactoring);
		assertNotNull("precondition was supposed to fail", result);
	}

	private void helper2(String newName, boolean updateReferences) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), false, true);
		ISourceRange selection= getSelection(cu);
		failTestHelper(newName, updateReferences, cu, selection);
	}

	private void helper2(String newName) throws Exception{
		helper2(newName, true);
	}

	@Test
	public void test0() throws Exception{
		helper1("j");
	}

	@Test
	public void test1() throws Exception{
		helper1("j");
	}

//	public void test2() throws Exception{
//		Map renaming= new HashMap();
//		renaming.put("x", "j");
//		renaming.put("y", "k");
//		helper1(renaming, new String[0]);
//	}

	@Test
	public void test3() throws Exception{
		helper1("j1");
	}

	@Test
	public void test4() throws Exception{
		helper1("k");
	}

	@Test
	public void test5() throws Exception{
		helper1("k");
	}

	@Test
	public void test6() throws Exception{
		helper1("k");
	}

	@Test
	public void test7() throws Exception{
		helper1("k");
	}
//
//	//8, 9, 10 removed
//
//
	@Test
	public void test11() throws Exception{
		helper1("j");
	}

	@Test
	public void test12() throws Exception{
		helper1("j");
	}

	@Test
	public void test13() throws Exception{
		helper1("j");
	}

	@Test
	public void test14() throws Exception{
		helper1("j");
	}

// disabled
//	public void test15() throws Exception{
//		Map renaming= new HashMap();
//		renaming.put("i", "j");
//		renaming.put("j", "i");
//		helper1(renaming, new String[0]);
//	}
//
	@Test
	public void test16() throws Exception{
		helper1("j");
	}

// disabled
//	public void test17() throws Exception{
//		Map renaming= new HashMap();
//		renaming.put("i", "j");
//		renaming.put("j", "i");
//		helper1(renaming, new String[0]);
//	}
//
	@Test
	public void test18() throws Exception{
		helper1("j");
	}

	@Test
	public void test19() throws Exception{
		helper1("j");
	}

	@Test
	public void test20() throws Exception{
		helper1("j");
	}

	@Test
	public void test21() throws Exception{
		helper1("j");
	}

	@Test
	public void test22() throws Exception{
		helper1("j");
	}

//	disabled
//	public void test23() throws Exception{
//		Map renaming= new HashMap();
//		renaming.put("i", "j");
//		renaming.put("j", "i");
//		helper1(renaming, new String[0]);
//	}

	@Test
	public void test24() throws Exception{
		helper1("j");
	}

	@Test
	public void test25() throws Exception{
		helper1("j");
	}

	@Test
	public void test26() throws Exception{
		helper1("j");
	}

//  deleted - incorrect. see testFail26
//	public void test27() throws Exception{
//		helper1("j");
//	}

	@Test
	public void test28() throws Exception{
		helper1("j");
	}

	@Test
	public void test29() throws Exception{
		helper1("b");
	}

	@Test
	public void test30() throws Exception{
		helper1("k");
	}

	@Test
	public void test31() throws Exception{
		helper1("kk");
	}

	@Test
	public void test32() throws Exception{
		helper1("j");
	}

	@Test
	public void test33() throws Exception{
		helper1("b", false);
	}

	@Test
	public void test34() throws Exception{
		helper1("j");
	}

	@Test
	public void test35() throws Exception{
//		printTestDisabledMessage("regression test for bug#9001");
		helper1("test2");
	}

	@Test
	public void test36() throws Exception{
//		printTestDisabledMessage("regression test for bug#7630");
		helper1("j", true, 5, 13, 5, 14);
	}

	@Test
	public void test37() throws Exception{
//		printTestDisabledMessage("regression test for bug#7630");
		helper1("j", true, 5, 16, 5, 17);
	}

	@Test
	public void test38() throws Exception{
//		printTestDisabledMessage("regression test for Bug#11453");
		helper1("i", true, 7, 12, 7, 13);
	}

	@Test
	public void test39() throws Exception{
//		printTestDisabledMessage("regression test for Bug#11440");
		helper1("j", true, 7, 16, 7, 18);
	}

	@Test
	public void test40() throws Exception{
//		printTestDisabledMessage("regression test for Bug#10660");
		helper1("j", true, 4, 16, 4, 17);
	}

	@Test
	public void test41() throws Exception{
//		printTestDisabledMessage("regression test for Bug#10660");
		helper1("j", true, 3, 17, 3, 18);
	}

	@Test
	public void test42() throws Exception{
//		printTestDisabledMessage("regression test for Bug#10660");
		helper1("j", true, 3, 25, 3, 26);
	}

	@Test
	public void test43() throws Exception{
//		printTestDisabledMessage("regression test for Bug#10660");
		helper1("j", true, 4, 23, 4, 24);
	}

	@Test
	public void test44() throws Exception{
//		printTestDisabledMessage("regression test for Bug#12200");
		helper1("j", true, 6, 11, 6, 14);
	}

	@Test
	public void test45() throws Exception{
//		printTestDisabledMessage("regression test for Bug#12210");
		helper1("j", true, 4, 14, 4, 14);
	}

	@Test
	public void test46() throws Exception{
//		printTestDisabledMessage("regression test for Bug#12210");
		helper1("j", true, 5, 18, 5, 18);
	}

	@Test
	public void test47() throws Exception{
//		printTestDisabledMessage("regression test for Bug#17922");
		helper1("newname", true, 7, 13, 7, 17);
	}

	@Test
	public void test48() throws Exception{
//		printTestDisabledMessage("regression test for Bug#22938");
		helper1("newname", true, 4, 16, 4, 20);
	}

	@Test
	public void test49() throws Exception{
//		printTestDisabledMessage("regression test for Bug#30923 ");
		helper1("newname", true, 4, 16, 4, 20);
	}

	@Test
	public void test50() throws Exception{
//		printTestDisabledMessage("regression test for Bug#30923 ");
		helper1("newname", true, 4, 16, 4, 20);
	}

	@Test
	public void test51() throws Exception {
//		printTestDisabledMessage("regression test for Bug#47822");
		helper1("qwerty", true, 5, 19, 5, 20);
	}

	@Test
	public void test52() throws Exception{
		helper1("j");
	}

	@Test
	public void test53() throws Exception{
//		printTestDisabledMessage("bug#19851");
		helper1("locker");
	}

	@Test
	public void test54() throws Exception{
		helper1("obj");
	}

	@Test
	public void test55() throws Exception{
		helper1("t");
	}

	@Test
	public void test56() throws Exception{
		helper1("param");
	}

	@Test
	public void test57() throws Exception{
		helper1("param");
	}

	@Test
	public void test58() throws Exception{
		helper1("param");
	}

	@Test
	public void test59() throws Exception{
		helper1("thing");
	}

	@Test
	public void test60() throws Exception{
		helper1("param");
	}

	@Test
	public void test61() throws Exception{
		helper1("x");
	}

	@Test
	public void test62() throws Exception {
//		printTestDisabledMessage("bug#47822");
		helper1("xxx");
	}

	@Test
	public void test63() throws Exception {
		// regression test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=144426
		helper1("xxx");
	}

	@Test
	public void test64() throws Exception {
		// regression test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=144426
		helper1("xxx");
	}

// -----
	@Ignore("BUG_checkDeclInNestedClass fails - must revisit")
	@Test
	public void testFail0() throws Exception{
		helper2("j");
	}

	@Test
	public void testFail1() throws Exception{
		failHelperNoElement();
	}

	@Test
	public void testFail2() throws Exception{
		helper2("i");
	}

	@Test
	public void testFail3() throws Exception{
		helper2("9");
	}

	@Test
	public void testFail4() throws Exception{
		helper2("j");
	}

	@Test
	public void testFail5() throws Exception{
		helper2("j");
	}

	@Ignore("BUG_checkDeclInNestedClass fails - must revisit")
	@Test
	public void testFail6() throws Exception{
		helper2("j");
	}

	@Test
	public void testFail7() throws Exception{
		helper2("j");
	}

	@Test
	public void testFail8() throws Exception{
		helper2("j");
	}

	@Test
	public void testFail9() throws Exception{
		helper2("j");
	}

	@Test
	public void testFail10() throws Exception{
		failHelperNoElement();
	}

// disabled - it's allowed now
//	public void testFail11() throws Exception{
//		helper2("uu");
//	}

	@Test
	public void testFail12() throws Exception{
//		printTestDisabledMessage("http://dev.eclipse.org/bugs/show_bug.cgi?id=11638");
		helper2("j");
	}

	@Test
	public void testFail13() throws Exception{
		helper2("j");
	}

	@Test
	public void testFail14() throws Exception{
		helper2("j");
	}

	@Test
	public void testFail15() throws Exception{
		helper2("j");
	}

	@Test
	public void testFail16() throws Exception{
		helper2("j");
	}

	@Test
	public void testFail17() throws Exception{
		helper2("j");
	}

	@Test
	public void testFail18() throws Exception{
		helper2("j");
	}

	@Test
	public void testFail19() throws Exception{
		helper2("j");
	}

	@Test
	public void testFail20() throws Exception{
		helper2("j");
	}

// disabled - it's allowed now
//	public void testFail21() throws Exception{
//		helper2("j");
//	}

	@Test
	public void testFail22() throws Exception{
		failHelperNoElement();
	}

// disabled - it's allowed now
//	public void testFail23() throws Exception{
//		helper2("j");
//	}

	@Test
	public void testFail24() throws Exception{
		//printTestDisabledMessage("compile errors are ok now");
		helper2("t"); //name collision
	}

	@Test
	public void testFail25() throws Exception{
		helper2("j");
	}

	@Ignore("BUG_checkShadowing Test disabled until it is clear how 1.4 treats this")
	@Test
	public void testFail26() throws Exception{
		helper2("j");
	}

	@Test
	public void testFail27() throws Exception{
		helper2("j");
	}

	@Test
	public void testFail28() throws Exception{
		helper2("j");
	}

}
