/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.refactoring.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.refactoring.Refactoring;

import org.eclipse.jdt.testplugin.JavaTestSetup;
import org.eclipse.jdt.testplugin.TestPluginLauncher;
import org.eclipse.jdt.testplugin.ui.*;

public class UndoManagerTests extends RefactoringTest {
	
	public UndoManagerTests(String name) {
		super(name);
	}
	
	public static void main(String[] args) {
		TestPluginUILauncher.run(TestPluginLauncher.getLocationFromProperties(), UndoManagerTests.class, args);
	}
	
	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(noSetupSuite());
		return new JavaTestSetup(suite);
	}
	
	public static Test noSetupSuite() {
		return new TestSuite(UndoManagerTests.class);
	}
	
	private void checkState(boolean undo, boolean redo, int undoCount, int redoCount){
		checkState(0, undo, redo, undoCount, redoCount);
	}
	
	private void checkState(int iterationCount, boolean undo, boolean redo, int undoCount, int redoCount){
		assert(iterationCount + " undo", undo == Refactoring.getUndoManager().anythingToUndo());
		assert(iterationCount + " redo", redo == Refactoring.getUndoManager().anythingToRedo());
		//assertEquals(iterationCount + "undo stack", undoCount, Refactoring.getUndoManager().getRefactoringLog().size());
		//assertEquals(iterationCount + "redo stack", redoCount, Refactoring.getUndoManager().getRedoStack().size());
	}
	
	public void test0() throws Exception{
		checkState(false, false, 0, 0);
	}
	
	public void test1() throws Exception{	
		performRefactoring(new NullRefactoring());
		checkState(true, false, 1, 0);
	}
	
	public void test2() throws Exception{	
		performRefactoring(new NullRefactoring());
		Refactoring.getUndoManager().performUndo(new NullProgressMonitor());
		checkState(false, true, 0, 1);
	}
	
	public void test3() throws Exception{	
		performRefactoring(new NullRefactoring());
		Refactoring.getUndoManager().performUndo(new NullProgressMonitor());
		Refactoring.getUndoManager().performRedo(new NullProgressMonitor());
		checkState(true, false, 1, 0);
	}
	
	public void test4() throws Exception{	
		performRefactoring(new NullRefactoring());
		Refactoring.getUndoManager().performUndo(new NullProgressMonitor());
		Refactoring.getUndoManager().performRedo(new NullProgressMonitor());
		Refactoring.getUndoManager().performUndo(new NullProgressMonitor());
		checkState(false, true, 0, 1);
	}

	public void test5() throws Exception{	
		performRefactoring(new NullRefactoring());
		Refactoring.getUndoManager().performUndo(new NullProgressMonitor());
		Refactoring.getUndoManager().performRedo(new NullProgressMonitor());
		performRefactoring(new NullRefactoring());
		checkState(true, false, 2, 0);
	}
	
	public void test6() throws Exception{	
		performRefactoring(new NullRefactoring());
		Refactoring.getUndoManager().performUndo(new NullProgressMonitor());
		Refactoring.getUndoManager().performRedo(new NullProgressMonitor());
		performRefactoring(new NullRefactoring());
		Refactoring.getUndoManager().performUndo(new NullProgressMonitor());
		Refactoring.getUndoManager().performUndo(new NullProgressMonitor());
		checkState(false, true, 0, 2);
	}	
	
	public void test7() throws Exception{	
		performRefactoring(new NullRefactoring());
		Refactoring.getUndoManager().performUndo(new NullProgressMonitor());
		Refactoring.getUndoManager().performRedo(new NullProgressMonitor());
		performRefactoring(new NullRefactoring());
		Refactoring.getUndoManager().performUndo(new NullProgressMonitor());
		checkState(true, true, 1, 1);
	}	
	
	public void test8() throws Exception{	
		int limit= 10;
		for (int i= 0; i < limit; i++){
			checkState(i, i != 0, false, i, 0);			
			performRefactoring(new NullRefactoring());
		}
		for (int i= 0; i < limit; i++){
			checkState(i, i != limit, i != 0, limit - i, i);			
			Refactoring.getUndoManager().performUndo(new NullProgressMonitor());
		}
		
		for (int i= 0; i < limit; i++){
			checkState(i, i != 0, i != limit, i, limit - i);			
			Refactoring.getUndoManager().performRedo(new NullProgressMonitor());
		}
	}
	

	public void test9() throws Exception{	
		performRefactoring(new NullRefactoring());
		Refactoring.getUndoManager().performUndo(new NullProgressMonitor());
		Refactoring.getUndoManager().performRedo(new NullProgressMonitor());
		performRefactoring(new NullRefactoring());
		Refactoring.getUndoManager().performUndo(new NullProgressMonitor());
		Refactoring.getUndoManager().flush();
		checkState(false, false, 0, 0);
	}		
}