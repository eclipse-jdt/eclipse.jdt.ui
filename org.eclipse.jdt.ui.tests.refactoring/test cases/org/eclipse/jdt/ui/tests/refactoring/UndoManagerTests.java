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

import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.internal.core.refactoring.UndoManager2;

public class UndoManagerTests extends RefactoringTest {

	private static final Class clazz= UndoManagerTests.class;
	public UndoManagerTests(String name) {
		super(name);
	}

	public static Test suite() {
		return new RefactoringTestSetup(new TestSuite(clazz));
	}

	public static Test setUpTest(Test test) {
		return new RefactoringTestSetup(test);
	}

	private void checkState(boolean undo, boolean redo, int undoCount, int redoCount){
		checkState(0, undo, redo, undoCount, redoCount);
	}

	protected IUndoManager getUndoManager() {
		return RefactoringCore.getUndoManager();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		RefactoringCore.getUndoManager().flush();
	}

	private void checkState(int iterationCount, boolean undo, boolean redo, int undoCount, int redoCount){
		assertTrue(iterationCount + " undo", undo == RefactoringCore.getUndoManager().anythingToUndo());
		assertTrue(iterationCount + " redo", redo == RefactoringCore.getUndoManager().anythingToRedo());
		testCounts(iterationCount, undoCount, redoCount);
	}

	private void testCounts(int iterationCount, int undoCount, int redoCount) {
		IUndoManager undoManager= RefactoringCore.getUndoManager();
		if (undoManager instanceof UndoManager2) {
			UndoManager2 manager= (UndoManager2)undoManager;
			assertTrue(iterationCount + "undo stack", manager.testHasNumberOfUndos(undoCount));
			assertTrue(iterationCount + "redo stack", manager.testHasNumberOfRedos(redoCount));
		}
	}

	private void performUndo() throws Exception {
		RefactoringCore.getUndoManager().performUndo(null, new NullProgressMonitor());
	}

	private void performRedo() throws Exception {
		RefactoringCore.getUndoManager().performRedo(null, new NullProgressMonitor());
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
		performUndo();
		checkState(false, true, 0, 1);
	}

	public void test3() throws Exception{
		performRefactoring(new NullRefactoring());
		performUndo();
		performRedo();
		checkState(true, false, 1, 0);
	}

	public void test4() throws Exception{
		performRefactoring(new NullRefactoring());
		performUndo();
		performRedo();
		performUndo();
		checkState(false, true, 0, 1);
	}

	public void test5() throws Exception{
		performRefactoring(new NullRefactoring());
		performUndo();
		performRedo();
		performRefactoring(new NullRefactoring());
		checkState(true, false, 2, 0);
	}

	public void test6() throws Exception{
		performRefactoring(new NullRefactoring());
		performUndo();
		performRedo();
		performRefactoring(new NullRefactoring());
		performUndo();
		performUndo();
		checkState(false, true, 0, 2);
	}

	public void test7() throws Exception{
		performRefactoring(new NullRefactoring());
		performUndo();
		performRedo();
		performRefactoring(new NullRefactoring());
		performUndo();
		checkState(true, true, 1, 1);
	}

	public void test8() throws Exception{
		// limit is 5 since the stack is limited to 5 entries
		int limit= 5;
		for (int i= 0; i < limit; i++){
			checkState(i, i != 0, false, i, 0);
			performRefactoring(new NullRefactoring());
		}
		for (int i= 0; i < limit; i++){
			checkState(i, i != limit, i != 0, limit - i, i);
			performUndo();
		}

		for (int i= 0; i < limit; i++){
			checkState(i, i != 0, i != limit, i, limit - i);
			performRedo();
		}
	}


	public void test9() throws Exception{
		performRefactoring(new NullRefactoring());
		performUndo();
		performRedo();
		performRefactoring(new NullRefactoring());
		performUndo();
		RefactoringCore.getUndoManager().flush();
		checkState(false, false, 0, 0);
	}
}
