/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.ui.tests.refactoring.infra.TestExceptionHandler;

import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameMethodRefactoring;

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
			RenameMethodRefactoring ref= RenameMethodRefactoring.createInstance(classA.getMethod(methodName, signatures));
			ref.setNewName(newMethodName);
			RefactoringStatus result= performRefactoring(ref);
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
			RenameMethodRefactoring ref= RenameMethodRefactoring.createInstance(classA.getMethod(methodName, signatures));
			ref.setUpdateReferences(updateReferences);
			ref.setNewName(newMethodName);
			assertEquals("was supposed to pass", null, performRefactoring(ref));
			assertEquals("invalid renaming", getFileContents(getOutputTestFileName("A")), cu.getSource());
			
			assertTrue("anythingToUndo", Refactoring.getUndoManager().anythingToUndo());
			assertTrue("! anythingToRedo", !Refactoring.getUndoManager().anythingToRedo());
			//assertEquals("1 to undo", 1, Refactoring.getUndoManager().getRefactoringLog().size());
			
			Refactoring.getUndoManager().performUndo(new ChangeContext(new TestExceptionHandler()), new NullProgressMonitor());
			assertEquals("invalid undo", getFileContents(getInputTestFileName("A")), cu.getSource());
	
			assertTrue("! anythingToUndo", !Refactoring.getUndoManager().anythingToUndo());
			assertTrue("anythingToRedo", Refactoring.getUndoManager().anythingToRedo());
			//assertEquals("1 to redo", 1, Refactoring.getUndoManager().getRedoStack().size());
			
			Refactoring.getUndoManager().performRedo(new ChangeContext(new TestExceptionHandler()), new NullProgressMonitor());
			assertEquals("invalid redo", getFileContents(getOutputTestFileName("A")), cu.getSource());
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
}
