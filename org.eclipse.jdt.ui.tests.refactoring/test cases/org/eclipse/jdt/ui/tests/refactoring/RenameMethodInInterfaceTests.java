/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameMethodProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameVirtualMethodProcessor;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;

public class RenameMethodInInterfaceTests extends RefactoringTest {
	
	private static final Class clazz= RenameMethodInInterfaceTests.class;
	private static final String REFACTORING_PATH= "RenameMethodInInterface/";

	public RenameMethodInInterfaceTests(String name) {
		super(name);
	}
	
	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private void helper1_not_available(String methodName, String[] signatures) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType interfaceI= getType(cu, "I");
		
		RenameProcessor processor= new RenameVirtualMethodProcessor(interfaceI.getMethod(methodName, signatures));
		RenameRefactoring ref= new RenameRefactoring(processor);
		assertTrue(! ref.isApplicable());
	}
	private void helper1_0(String methodName, String newMethodName, String[] signatures) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType interfaceI= getType(cu, "I");
		RenameMethodProcessor processor= new RenameVirtualMethodProcessor(interfaceI.getMethod(methodName, signatures));
		RenameRefactoring ref= new RenameRefactoring(processor);
		processor.setNewElementName(newMethodName);
		RefactoringStatus result= performRefactoring(ref);
		assertNotNull("precondition was supposed to fail", result);
	}
	
	private void helper1() throws Exception{
		helper1_0("m", "k", new String[0]);
	}
	
	private void helper2_0(String methodName, String newMethodName, String[] signatures, boolean shouldPass, boolean updateReferences) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		IType interfaceI= getType(cu, "I");
		RenameMethodProcessor processor= new RenameVirtualMethodProcessor(interfaceI.getMethod(methodName, signatures));
		RenameRefactoring ref= new RenameRefactoring(processor);
		processor.setUpdateReferences(updateReferences);
		processor.setNewElementName(newMethodName);
		assertEquals("was supposed to pass", null, performRefactoring(ref));
		if (!shouldPass){
			assertTrue("incorrect renaming because of a java model bug", ! getFileContents(getOutputTestFileName("A")).equals(cu.getSource()));
			return;
		}
		assertEqualLines("incorrect renaming", getFileContents(getOutputTestFileName("A")), cu.getSource());
		
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
	}
	
	private void helper2(boolean updateReferences) throws Exception{
		helper2_0("m", "k", new String[0], true, updateReferences);
	}
	
	private void helper2() throws Exception{
		helper2(true);
	}
	
	/********tests************/
	public void testFail0() throws Exception{
		helper1();
	}
	public void testFail1() throws Exception{
		helper1();
	}
	public void testFail3() throws Exception{
		helper1();
	}
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
		helper1_0("m", "k", new String[]{Signature.SIG_INT});
	}
	public void testFail9() throws Exception{
		helper1();
	}
	public void testFail10() throws Exception{
		helper1();
	}
	public void testFail11() throws Exception{
		helper1();
	}
	public void testFail12() throws Exception{
		helper1();
	}
	public void testFail13() throws Exception{
		helper1();
	}
	public void testFail14() throws Exception{
		helper1();
	}
	public void testFail15() throws Exception{
		helper1();
	}
	public void testFail16() throws Exception{
		helper1();
	}
	public void testFail17() throws Exception{
		helper1();
	}
	public void testFail18() throws Exception{
		helper1();
	}
	public void testFail19() throws Exception{
		helper1();
	}
	public void testFail20() throws Exception{
		helper1();
	}
	public void testFail21() throws Exception{
		helper1_0("m", "k", new String[]{"QString;"});
	}
	public void testFail22() throws Exception{
		helper1_0("m", "k", new String[]{"QObject;"});
	}
	public void testFail23() throws Exception{
		helper1_not_available("toString", new String[0]);
	}
	public void testFail24() throws Exception{
		helper1();
	}
	public void testFail25() throws Exception{
		helper1();
	}
	public void testFail26() throws Exception{
		helper1();
	}
	public void testFail27() throws Exception{
		helper1();
	}
	public void testFail28() throws Exception{
		helper1();
	}
	public void testFail29() throws Exception{
		helper1();
	}
	
	public void testFail30() throws Exception{
		helper1_not_available("toString", new String[0]);
	}
	
	public void testFail31() throws Exception{
		helper1_not_available("toString", new String[0]);
	}
	
	public void testFail32() throws Exception{
		helper1_0("m", "toString", new String[0]);
	}
	
	public void testFail33() throws Exception{
		helper1_0("m", "toString", new String[0]);
	}
	
	public void testFail34() throws Exception{
		helper1_0("m", "equals", new String[]{"QObject;"});
	}
	
	public void testFail35() throws Exception{
		helper1_0("m", "equals", new String[]{"Qjava.lang.Object;"});
	}
	
	public void testFail36() throws Exception{
		helper1_0("m", "getClass", new String[0]);
	}
	
	public void testFail37() throws Exception{
		helper1_0("m", "hashCode", new String[0]);
	}

	public void testFail38() throws Exception{
		helper1_0("m", "notify", new String[0]);
	}	

	public void testFail39() throws Exception{
		helper1_0("m", "notifyAll", new String[0]);
	}	
	
	public void testFail40() throws Exception{
		helper1_0("m", "wait", new String[]{Signature.SIG_LONG, Signature.SIG_INT});
	}	

	public void testFail41() throws Exception{
		helper1_0("m", "wait", new String[]{Signature.SIG_LONG});
	}	
	
	public void testFail42() throws Exception{
		helper1_0("m", "wait", new String[0]);
	}	
	
	public void testFail43() throws Exception{
		helper1_0("m", "wait", new String[0]);
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
		helper2();
	}
	public void test10() throws Exception{
		helper2();
	}
	public void test11() throws Exception{
		helper2();
	}	
	public void test12() throws Exception{
		helper2();
	}
	
	//test13 became testFail45
	//public void test13() throws Exception{
	//	helper2();
	//}
	public void test14() throws Exception{
		helper2();
	}
	public void test15() throws Exception{
		helper2();
	}
	public void test16() throws Exception{
		helper2();
	}
	public void test17() throws Exception{
		helper2();
	}
	public void test18() throws Exception{
		helper2();
	}
	public void test19() throws Exception{
		helper2();
	}
	public void test20() throws Exception{
		helper2();
	}
	//anonymous inner class
	public void test21() throws Exception{
		printTestDisabledMessage("must fix - incorrect warnings");
		//helper2_fail();
	}
	public void test22() throws Exception{
		helper2();
	}
	
	//test23 became testFail45
	//public void test23() throws Exception{
	//	helper2();
	//}
	
	public void test24() throws Exception{
		helper2();
	}
	public void test25() throws Exception{
		helper2();
	}
	public void test26() throws Exception{
		helper2();
	}
	public void test27() throws Exception{
		helper2();
	}
	public void test28() throws Exception{
		helper2();
	}
	public void test29() throws Exception{
		helper2();
	}
	public void test30() throws Exception{
		helper2();
	}
	//anonymous inner class
	public void test31() throws Exception{
		helper2();
	}
	//anonymous inner class
	public void test32() throws Exception{
		helper2();
	}
	public void test33() throws Exception{
		helper2();
	}
	public void test34() throws Exception{
		helper2();
	}
	public void test35() throws Exception{
		helper2();
	}
	public void test36() throws Exception{
		helper2();
	}
	public void test37() throws Exception{
		helper2();
	}
	public void test38() throws Exception{
		helper2();
	}
	public void test39() throws Exception{
		helper2();
	}
	public void test40() throws Exception{
		helper2();
	}
	public void test41() throws Exception{
		helper2();
	}
	public void test42() throws Exception{
		helper2();
	}
	public void test43() throws Exception{
		helper2();
	}
	public void test44() throws Exception{
		helper2();
	}
	public void test45() throws Exception{
		helper2();
	}
	public void test46() throws Exception{
		helper2(false);
	}
	public void test47() throws Exception{
		helper2();
	}
}
