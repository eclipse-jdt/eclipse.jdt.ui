/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     Nikolay Metchev <nikolaymetchev@gmail.com> - [rename] https://bugs.eclipse.org/99622
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;

import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameMethodProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameVirtualMethodProcessor;

import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

public class RenameVirtualMethodInClassTests extends GenericRefactoringTest {

	private static final String REFACTORING_PATH= "RenameVirtualMethodInClass/";

	public RenameVirtualMethodInClassTests() {
		rts= new RefactoringTestSetup();
	}

	@Override
	protected String getRefactoringPath(){
		return REFACTORING_PATH;
	}

	private void helper1_not_available(String methodName, String[] signatures) throws Exception{
		IType classA= getType(createCUfromTestFile(getPackageP(), "A"), "A");
		RenameMethodProcessor processor= new RenameVirtualMethodProcessor(classA.getMethod(methodName, signatures));
		RenameRefactoring ref= new RenameRefactoring(processor);
		assertFalse(ref.isApplicable());
	}

	private void helper1_0(String methodName, String newMethodName, String[] signatures) throws Exception{
		IType classA= getType(createCUfromTestFile(getPackageP(), "A"), "A");
		RenameMethodProcessor processor= new RenameVirtualMethodProcessor(classA.getMethod(methodName, signatures));
		RenameRefactoring ref= new RenameRefactoring(processor);
		processor.setNewElementName(newMethodName);
		RefactoringStatus result= performRefactoring(ref);
		assertNotNull("precondition was supposed to fail", result);
	}

	private void helper1() throws Exception{
		helper1_0("m", "k", new String[0]);
	}

	private void helper2_0(String methodName, String newMethodName, String[] signatures, boolean shouldPass, boolean updateReferences, boolean createDelegate) throws Exception{
		final ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		final IType classA= getType(cu, "A");
		final IMethod method= classA.getMethod(methodName, signatures);
		final RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_METHOD);
		descriptor.setJavaElement(method);
		descriptor.setNewName(newMethodName);
		descriptor.setUpdateReferences(updateReferences);
		descriptor.setKeepOriginal(createDelegate);
		descriptor.setDeprecateDelegate(createDelegate);
		final RefactoringStatus status= new RefactoringStatus();
		final Refactoring refactoring= descriptor.createRefactoring(status);
		assertNotNull("Refactoring should not be null", refactoring);
		assertTrue("status should be ok", status.isOK());
		assertNull("was supposed to pass", performRefactoring(refactoring));
		if (!shouldPass){
			assertNotEquals("incorrect renaming because of java model", getFileContents(getOutputTestFileName("A")), cu.getSource());
			return;
		}
		String expectedRenaming= getFileContents(getOutputTestFileName("A"));
		String actuaRenaming= cu.getSource();
		assertEqualLines("incorrect renaming", expectedRenaming, actuaRenaming);

		assertTrue("anythingToUndo", RefactoringCore.getUndoManager().anythingToUndo());
		assertFalse("! anythingToRedo", RefactoringCore.getUndoManager().anythingToRedo());
		//assertEquals("1 to undo", 1, Refactoring.getUndoManager().getRefactoringLog().size());

		RefactoringCore.getUndoManager().performUndo(null, new NullProgressMonitor());
		assertEqualLines("invalid undo", getFileContents(getInputTestFileName("A")), cu.getSource());

		assertFalse("! anythingToUndo", RefactoringCore.getUndoManager().anythingToUndo());
		assertTrue("anythingToRedo", RefactoringCore.getUndoManager().anythingToRedo());
		//assertEquals("1 to redo", 1, Refactoring.getUndoManager().getRedoStack().size());

		RefactoringCore.getUndoManager().performRedo(null, new NullProgressMonitor());
		assertEqualLines("invalid redo", getFileContents(getOutputTestFileName("A")), cu.getSource());
	}

	private void helper2_0(String methodName, String newMethodName, String[] signatures, boolean shouldPass) throws Exception{
		helper2_0(methodName, newMethodName, signatures, shouldPass, true, false);
	}

	private void helper2_0(String methodName, String newMethodName, String[] signatures) throws Exception{
		helper2_0(methodName, newMethodName, signatures, true);
	}

	private void helper2(boolean updateReferences) throws Exception{
		helper2_0("m", "k", new String[0], true, updateReferences, false);
	}

	private void helper2() throws Exception{
		helper2(true);
	}

	private void helperDelegate() throws Exception{
		helper2_0("m", "k", new String[0], true, true, true);
	}

	private void helper2_fail() throws Exception{
		helper2_0("m", "k", new String[0], false);
	}

// ----------------------------------------------------------------

	@Test
	public void testEnum1() throws Exception {
		helper2_0("getNameLength", "getNameSize", new String[0]);
	}

	@Test
	public void testEnum2() throws Exception {
		helper2_0("getSquare", "get2ndPower", new String[0]);
	}

	@Test
	public void testEnum3() throws Exception {
		helper2_0("getSquare", "get2ndPower", new String[0]);
	}

	@Ignore("BUG_83217_IMPLICIT_ENUM_METHODS")
	@Test
	public void testEnumFail1() throws Exception {
		helper1_0("value", "valueOf", new String[]{"QString;"});
	}

	@Test
	public void testGenerics1() throws Exception {
		helper2_0("m", "k", new String[]{"QG;"});
	}

	@Test
	public void testGenerics2() throws Exception {
		helper2_0("add", "addIfPositive", new String[]{"QE;"});
	}

	@Test
	public void testGenerics3() throws Exception {
		helper2_0("add", "addIfPositive", new String[]{"QT;"});
	}

	@Test
	public void testGenerics4() throws Exception {
		helper2_0("takeANumber", "doit", new String[]{"QNumber;"});
	}

	@Test
	public void testGenerics5() throws Exception {
		helper2_0("covariant", "variant", new String[0]);
	}

	@Test
	public void testVarargs1() throws Exception {
		helper2_0("runall", "runThese", new String[]{"[QRunnable;"});
	}

	@Test
	public void testVarargs2() throws Exception {
		helper2_0("m", "k", new String[]{"[QString;"});
	}

	@Test
	public void testFail0() throws Exception{
		helper1();
	}

	@Test
	public void testFail1() throws Exception{
		helper1_not_available("toString", new String[0]);
	}

	@Test
	public void testFail2() throws Exception{
		helper1();
	}

	@Test
	public void testFail3() throws Exception{
		helper1();
	}

	@Test
	public void testFail4() throws Exception{
		helper1();
	}

	@Test
	public void testFail5() throws Exception{
		helper1();
	}

	@Test
	public void testFail6() throws Exception{
		helper1();
	}

	@Test
	public void testFail7() throws Exception{
		helper1();
	}

	@Test
	public void testFail8() throws Exception{
		helper1();
	}

	@Test
	public void testFail9() throws Exception{
		helper1_0("m", "k", new String[]{Signature.SIG_INT});
	}

	@Test
	public void testFail10() throws Exception{
		helper1();
	}

	@Test
	public void testFail11() throws Exception{
		helper1();
	}

	@Test
	public void testFail12() throws Exception{
		helper1();
	}

	@Test
	public void testFail13() throws Exception{
		helper1();
	}

	@Test
	public void testFail14() throws Exception{
		helper1_0("m", "k", new String[]{Signature.SIG_INT});
	}

	@Test
	public void testFail15() throws Exception{
		helper1();
	}

	@Test
	public void testFail17() throws Exception{
		helper1();
	}

	@Test
	public void testFail18() throws Exception{
		helper1();
	}

	@Test
	public void testFail19() throws Exception{
		helper1();
	}

	@Test
	public void testFail20() throws Exception{
		helper1();
	}

	@Test
	public void testFail21() throws Exception{
		helper1();
	}

	@Test
	public void testFail22() throws Exception{
		helper1();
	}

	@Test
	public void testFail23() throws Exception{
		helper1();
	}

	@Test
	public void testFail24() throws Exception{
		helper1();
	}

	@Test
	public void testFail25() throws Exception{
		helper1();
	}

	@Test
	public void testFail26() throws Exception{
		helper1();
	}

	@Test
	public void testFail27() throws Exception{
		helper1();
	}

	@Test
	public void testFail28() throws Exception{
		helper1();
	}

	@Test
	public void testFail29() throws Exception{
		helper1();
	}

	@Test
	public void testFail30() throws Exception{
		helper1();
	}

	@Test
	public void testFail31() throws Exception{
		helper1_0("m", "k", new String[]{"QString;"});
	}

	@Test
	public void testFail32() throws Exception{
		helper1_0("m", "k", new String[]{"QObject;"});
	}

	@Test
	public void testFail33() throws Exception{
		helper1_not_available("toString", new String[0]);
	}

	@Test
	public void testFail34() throws Exception{
		helper1_0("m", "k", new String[]{"QString;"});
	}

//	//test removed - was invalid
//	public void testFail35() throws Exception{
//	}

	@Test
	public void testFail36() throws Exception{
		helper1();
	}

	@Test
	public void testFail37() throws Exception{
		helper1();
	}

	@Test
	public void testFail38() throws Exception{
		//printTestDisabledMessage("must fix - nested type");
		helper1();
	}

	@Test
	public void testFail39() throws Exception{
		helper1();
	}

	@Test
	public void testFail40() throws Exception{
		//Autoboxing -> calls to methods can be redirected due to overloading
		helper1_0("m", "k", new String[]{Signature.SIG_INT});
	}

	@Test
	public void testFail41() throws Exception{
		helper1();
	}

	@Test
	public void test1() throws Exception{
		ParticipantTesting.reset();
		helper2();
	}

	@Test
	public void test10() throws Exception{
		helper2();
	}

	@Test
	public void test11() throws Exception{
		helper2();
	}

	@Test
	public void test12() throws Exception{
		helper2();
	}

	@Test
	public void test13() throws Exception{
		helper2();
	}

	@Test
	public void test14() throws Exception{
		helper2();
	}

	@Test
	public void test15() throws Exception{
		helper2_0("m", "k", new String[]{Signature.SIG_INT});
	}

	@Test
	public void test16() throws Exception{
		helper2_0("m", "fred", new String[]{Signature.SIG_INT});
	}

	@Test
	public void test17() throws Exception{
		//printTestDisabledMessage("overloading");
		helper2_0("m", "kk", new String[]{Signature.SIG_INT});
	}

	@Test
	public void test18() throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuC= createCUfromTestFile(getPackageP(), "C");

		IType classB= getType(cu, "B");
		IMethod method= classB.getMethod("m", new String[]{"I"});

		final RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_METHOD);
		descriptor.setJavaElement(method);
		descriptor.setNewName("kk");
		descriptor.setUpdateReferences(true);
		final RefactoringStatus status= new RefactoringStatus();
		final Refactoring refactoring= descriptor.createRefactoring(status);
		assertNotNull("Refactoring should not be null", refactoring);
		assertTrue("status should be ok", status.isOK());

		assertNull("was supposed to pass", performRefactoring(refactoring));
		assertEqualLines("invalid renaming A", getFileContents(getOutputTestFileName("A")), cu.getSource());
		assertEqualLines("invalid renaming C", getFileContents(getOutputTestFileName("C")), cuC.getSource());
	}

	@Test
	public void test19() throws Exception{
		helper2_0("m", "fred", new String[0]);
	}

	@Test
	public void test2() throws Exception{
		helper2_0("m", "fred", new String[0]);
	}

	@Test
	public void test20() throws Exception{
		helper2_0("m", "fred", new String[]{Signature.SIG_INT});
	}

	@Test
	public void test21() throws Exception{
		helper2_0("m", "fred", new String[]{Signature.SIG_INT});
	}

	@Test
	public void test22() throws Exception{
		helper2();
	}

	@Test
	public void test41() throws Exception {
		helper2_0("m", "k", new String[] { "QI;" });
	}

	@Test
	public void test42() throws Exception {
		helper2_0("m", "k", new String[] { "QT;" });
	}

	@Test
	public void test43() throws Exception {
		helper2_0("m", "k", new String[] { "QObject;" });
	}

	@Test
	public void test44() throws Exception {
		helper2_0("m", "k", new String[] { "QE;" });
	}

	@Test
	public void test45() throws Exception {
		helper2_0("m", "k", new String[] { "QT;" });
	}

	@Test
	public void test46() throws Exception {
		helper2_0("m", "k", new String[] { "[QE;" });
	}

	@Test
	public void test47() throws Exception {
		helper2_0("m", "k", new String[] { "[QString;" });
	}

	@Test
	public void test48() throws Exception {
		helper2_0("m", "k", new String[] { "[QString;" });
	}

	@Test
	public void test49() throws Exception {
		helper2();
	}

	//anonymous inner class
	@Test
	public void test23() throws Exception{
		helper2_fail();
	}

	@Test
	public void test24() throws Exception{
		helper2_0("m", "k", new String[]{"QString;"});
	}

	@Test
	public void test25() throws Exception{
		//printTestDisabledMessage("waiting for 1GIIBC3: ITPJCORE:WINNT - search for method references - missing matches");
		helper2();
	}

	@Test
	public void test26() throws Exception{
		helper2();
	}

	@Test
	public void test27() throws Exception{
		helper2();
	}

	@Test
	public void test28() throws Exception{
		helper2();
	}

	@Test
	public void test29() throws Exception{
		helper2();
	}

	@Test
	public void test30() throws Exception{
		helper2();
	}

	@Test
	public void test31() throws Exception{
		helper2();
	}

	@Test
	public void test32() throws Exception{
		helper2(false);
	}

	@Test
	public void test33() throws Exception{
		helper2();
	}

	@Ignore("test for bug#18553")
	@Test
	public void test34() throws Exception{
//		helper2_0("A", "foo", new String[0], true, true);
	}

	@Test
	public void test35() throws Exception{
		helper2_0("foo", "bar", new String[] {"QObject;"}, true);
	}

	@Test
	public void test36() throws Exception{
		helper2_0("foo", "bar", new String[] {"QString;"}, true);
	}

	@Test
	public void test37() throws Exception{
		helper2_0("foo", "bar", new String[] {"QA;"}, true);
	}

	@Ignore("difficult to set up test in current testing framework")
	@Test
	public void test38() throws Exception {
		helper2();
	}

	@Test
	public void test39() throws Exception {
		helper2();
	}

	@Test
	public void test40() throws Exception { // test for bug 68592
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType localClass= cu.getType("A").getMethod("doit", new String[0]).getType("LocalClass", 1);
		IMethod method= localClass.getMethod("method", new String[]{"I"});

		final RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_METHOD);
		descriptor.setJavaElement(method);
		descriptor.setNewName("method2");
		descriptor.setUpdateReferences(true);
		final RefactoringStatus status= new RefactoringStatus();
		final Refactoring refactoring= descriptor.createRefactoring(status);
		assertNotNull("Refactoring should not be null", refactoring);
		assertTrue("status should be ok", status.isOK());

		assertNull("was supposed to pass", performRefactoring(refactoring));
		assertEqualLines("invalid renaming A", getFileContents(getOutputTestFileName("A")), cu.getSource());
	}

	//anonymous inner class
	@Test
	public void testAnon0() throws Exception{
		helper2();
	}

	@Test
	public void testLocal0() throws Exception{
		helper2();
	}

	@Test
	public void testDelegate01() throws Exception {
		// simple delegate
		helperDelegate();
	}

	@Test
	public void testDelegate02() throws Exception {
		// overridden delegates with abstract mix-in
		helperDelegate();
	}

	@Test
	public void testDelegate03() throws Exception {
		// overridden delegates in local type
		helperDelegate();
	}
}
