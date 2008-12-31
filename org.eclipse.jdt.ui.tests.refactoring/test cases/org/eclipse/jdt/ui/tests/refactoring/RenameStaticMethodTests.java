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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;

import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;

public class RenameStaticMethodTests extends RefactoringTest {
	private static final Class clazz= RenameStaticMethodTests.class;
	private static final String REFACTORING_PATH= "RenameStaticMethod/";

	private static final boolean BUG_83332_SPLIT_SINGLE_IMPORT= true;

	public RenameStaticMethodTests(String name) {
		super(name);
	}

	public static Test suite() {
		return new RefactoringTestSetup(new TestSuite(clazz));
	}

	public static Test setUpTest(Test test) {
		return new RefactoringTestSetup(test);
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private void helper1_0(String methodName, String newMethodName, String[] signatures) throws Exception{
			IType classA= getType(createCUfromTestFile(getPackageP(), "A"), "A");
		try{
			IMethod method= classA.getMethod(methodName, signatures);
			RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_METHOD);
			descriptor.setJavaElement(method);
			descriptor.setNewName(newMethodName);
			descriptor.setUpdateReferences(true);
			RefactoringStatus result= performRefactoring(descriptor);
			assertNotNull("precondition was supposed to fail", result);
		} finally{
			performDummySearch();
			classA.getCompilationUnit().delete(true, null);
		}
	}

	private void helper1() throws Exception{
		helper1_0("m", "k", new String[0]);
	}

	private void helper2_0(String methodName, String newMethodName, String[] signatures, boolean updateReferences, boolean createDelegate) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		try{
			IType classA= getType(cu, "A");
			IMethod method= classA.getMethod(methodName, signatures);
			RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_METHOD);
			descriptor.setUpdateReferences(updateReferences);
			descriptor.setJavaElement(method);
			descriptor.setNewName(newMethodName);
			descriptor.setKeepOriginal(createDelegate);
			descriptor.setDeprecateDelegate(true);

			assertEquals("was supposed to pass", null, performRefactoring(descriptor));
			assertEqualLines("invalid renaming", getFileContents(getOutputTestFileName("A")), cu.getSource());

			assertTrue("anythingToUndo", RefactoringCore.getUndoManager().anythingToUndo());
			assertTrue("! anythingToRedo", !RefactoringCore.getUndoManager().anythingToRedo());
			//assertEquals("1 to undo", 1, Refactoring.getUndoManager().getRefactoringLog().size());

			RefactoringCore.getUndoManager().performUndo(null, new NullProgressMonitor());
			assertEqualLines("invalid undo", getFileContents(getInputTestFileName("A")), cu.getSource());

			assertTrue("! anythingToUndo", !RefactoringCore.getUndoManager().anythingToUndo());
			assertTrue("anythingToRedo", RefactoringCore.getUndoManager().anythingToRedo());
			//assertEquals("1 to redo", 1, Refactoring.getUndoManager().getRedoStack().size());

			RefactoringCore.getUndoManager().performRedo(null, new NullProgressMonitor());
			assertEqualLines("invalid redo", getFileContents(getOutputTestFileName("A")), cu.getSource());
		} finally{
			performDummySearch();
			cu.delete(true, null);
		}
	}
	private void helper2_0(String methodName, String newMethodName, String[] signatures) throws Exception{
		helper2_0(methodName, newMethodName, signatures, true, false);
	}

	private void helper2(boolean updateReferences) throws Exception{
		helper2_0("m", "k", new String[0], updateReferences, false);
	}

	private void helper2() throws Exception{
		helper2(true);
	}

	private void helperDelegate() throws Exception{
		helper2_0("m", "k", new String[0], true, true);
	}

	public void testFail0() throws Exception {
		helper1();
	}

	public void testFail1() throws Exception{
		helper1();
	}

	public void testFail2() throws Exception{
		helper1();
	}

	//testFail3 deleted

	public void testFail4() throws Exception{
		helper1();
	}

	public void testFail5() throws Exception{
		helper1();
	}

	public void testFail6() throws Exception{
		helper1();
	}

	public void testFail7() throws Exception{
		helper1();
	}

	public void testFail8() throws Exception{
		helper1();
	}

	public void test0() throws Exception{
		helper2();
	}

	public void test1() throws Exception{
		helper2();
	}

	public void test2() throws Exception{
		helper2();
	}

	public void test3() throws Exception{
		helper2();
	}

	public void test4() throws Exception{
		helper2();
	}

	public void test5() throws Exception{
		helper2();
	}

	public void test6() throws Exception{
		helper2();
	}

	public void test7() throws Exception{
		helper2_0("m", "k", new String[]{Signature.SIG_INT});
	}

	public void test8() throws Exception{
		helper2_0("m", "k", new String[]{Signature.SIG_INT});
	}

	public void test9() throws Exception{
		helper2_0("m", "k", new String[]{Signature.SIG_INT}, false, false);
	}

	public void test10() throws Exception{
//		printTestDisabledMessage("bug 40628");
//		if (true)	return;
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");

		IType classB= getType(cuB, "B");
		IMethod method= classB.getMethod("method", new String[0]);
		RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_METHOD);
		descriptor.setUpdateReferences(true);
		descriptor.setJavaElement(method);
		descriptor.setNewName("newmethod");

		assertEquals("was supposed to pass", null, performRefactoring(descriptor));
		assertEqualLines("invalid renaming in A", getFileContents(getOutputTestFileName("A")), cuA.getSource());
		assertEqualLines("invalid renaming in B", getFileContents(getOutputTestFileName("B")), cuB.getSource());
	}

	public void test11() throws Exception{
//		printTestDisabledMessage("bug 40452");
//		if (true)	return;
		IPackageFragment packageA= getRoot().createPackageFragment("a", false, new NullProgressMonitor());
		IPackageFragment packageB= getRoot().createPackageFragment("b", false, new NullProgressMonitor());
		try {
			ICompilationUnit cuA= createCUfromTestFile(packageA, "A");
			ICompilationUnit cuB= createCUfromTestFile(packageB, "B");

			IType classA= getType(cuA, "A");
			IMethod method= classA.getMethod("method2", new String[0]);
			RenameJavaElementDescriptor descriptor= RefactoringSignatureDescriptorFactory.createRenameJavaElementDescriptor(IJavaRefactorings.RENAME_METHOD);
			descriptor.setUpdateReferences(true);
			descriptor.setJavaElement(method);
			descriptor.setNewName("fred");

			assertEquals("was supposed to pass", null, performRefactoring(descriptor));
			assertEqualLines("invalid renaming in A", getFileContents(getOutputTestFileName("A")), cuA.getSource());
			assertEqualLines("invalid renaming in B", getFileContents(getOutputTestFileName("B")), cuB.getSource());
		} finally{
			packageA.delete(true, new NullProgressMonitor());
			packageB.delete(true, new NullProgressMonitor());
		}
	}

	public void testUnicode01() throws Exception{
		helper2_0("e", "f", new String[]{});
	}

	public void testStaticImportFail0() throws Exception {
		helper1();
	}

	public void testStaticImport1() throws Exception {
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "C");
		helper2();
		assertEqualLines("invalid renaming in C", getFileContents(getOutputTestFileName("C")), cuA.getSource());
	}

	public void testStaticImport2() throws Exception {
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "C");
		helper2();
		assertEqualLines("invalid renaming in C", getFileContents(getOutputTestFileName("C")), cuA.getSource());
	}

	public void testStaticImport3() throws Exception {
		if (BUG_83332_SPLIT_SINGLE_IMPORT) {
			printTestDisabledMessage("BUG_83332_SPLIT_SINGLE_IMPORT");
			return;
		}
		helper2();
	}

	public void testStaticImport4() throws Exception {
		helper2();
	}

	public void testStaticImport5() throws Exception {
		if (BUG_83332_SPLIT_SINGLE_IMPORT) {
			printTestDisabledMessage("BUG_83332_SPLIT_SINGLE_IMPORT");
			return;
		}
		helper2();
	}

	public void testDelegate01() throws Exception  {
		// simple static delegate
		helperDelegate();
	}

}
