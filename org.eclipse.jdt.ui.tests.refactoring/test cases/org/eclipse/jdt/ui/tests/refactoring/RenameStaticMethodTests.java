/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameMethodProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameNonVirtualMethodProcessor;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

public class RenameStaticMethodTests extends RefactoringTest {
	private static final Class clazz= RenameStaticMethodTests.class;
	private static final String REFACTORING_PATH= "RenameStaticMethod/";

	public RenameStaticMethodTests(String name) {
		super(name);
	}

	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private void helper1_0(String methodName, String newMethodName, String[] signatures) throws Exception{
			IType classA= getType(createCUfromTestFile(getPackageP(), "A"), "A");
		try{
			RenameMethodProcessor processor= new RenameNonVirtualMethodProcessor(classA.getMethod(methodName, signatures));
			RenameRefactoring refactoring= new RenameRefactoring(processor);
			processor.setNewElementName(newMethodName);
			RefactoringStatus result= performRefactoring(refactoring);
			assertNotNull("precondition was supposed to fail", result);
		} finally{
			performDummySearch();
			classA.getCompilationUnit().delete(true, null);
		}	
	}
	
	private void helper1() throws Exception{
		helper1_0("m", "k", new String[0]);
	}
	
	private void helper2_0(String methodName, String newMethodName, String[] signatures, boolean updateReferences) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		try{
			IType classA= getType(cu, "A");
			RenameMethodProcessor processor= new RenameNonVirtualMethodProcessor(classA.getMethod(methodName, signatures));
			RenameRefactoring refactoring= new RenameRefactoring(processor);
			processor.setUpdateReferences(updateReferences);
			processor.setNewElementName(newMethodName);
			assertEquals("was supposed to pass", null, performRefactoring(refactoring));
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
		helper2_0(methodName, newMethodName, signatures, true);
	}
	
	private void helper2(boolean updateReferences) throws Exception{
		helper2_0("m", "k", new String[0], updateReferences);
	}
	
	private void helper2() throws Exception{
		helper2(true);
	}

	/********** tests *********/
	public void testFail0() throws Exception{
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
		helper2_0("m", "k", new String[]{Signature.SIG_INT}, false);
	}
	
	public void test10() throws Exception{
//		printTestDisabledMessage("bug 40628");
//		if (true)	return;
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");

		IType classB= getType(cuB, "B");
		RenameMethodProcessor processor= new RenameNonVirtualMethodProcessor(classB.getMethod("method", new String[0]));
		RenameRefactoring refactoring= new RenameRefactoring(processor);
		processor.setUpdateReferences(true);
		processor.setNewElementName("newmethod");
		assertEquals("was supposed to pass", null, performRefactoring(refactoring));
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
			RenameMethodProcessor processor= new RenameNonVirtualMethodProcessor(classA.getMethod("method2", new String[0]));
			RenameRefactoring refactoring= new RenameRefactoring(processor);
			processor.setUpdateReferences(true);
			processor.setNewElementName("fred");
			assertEquals("was supposed to pass", null, performRefactoring(refactoring));
			assertEqualLines("invalid renaming in A", getFileContents(getOutputTestFileName("A")), cuA.getSource());
			assertEqualLines("invalid renaming in B", getFileContents(getOutputTestFileName("B")), cuB.getSource());
		} finally{
			packageA.delete(true, new NullProgressMonitor());
			packageB.delete(true, new NullProgressMonitor());
		}
	}
}
