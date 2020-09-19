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
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.internal.core.refactoring.UndoManager2;

import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

public class UndoManagerTests extends GenericRefactoringTest {

	public UndoManagerTests() {
		rts= new RefactoringTestSetup();
	}

	private void checkState(boolean undo, boolean redo, int undoCount, int redoCount){
		checkState(0, undo, redo, undoCount, redoCount);
	}

	@Override
	protected IUndoManager getUndoManager() {
		return RefactoringCore.getUndoManager();
	}

	@After
	public void after() throws Exception {
		RefactoringCore.getUndoManager().flush();
	}

	private void checkState(int iterationCount, boolean undo, boolean redo, int undoCount, int redoCount){
		assertEquals(iterationCount + " undo", undo, RefactoringCore.getUndoManager().anythingToUndo());
		assertEquals(iterationCount + " redo", redo, RefactoringCore.getUndoManager().anythingToRedo());
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

	@Test
	public void test0() throws Exception{
		checkState(false, false, 0, 0);
	}

	@Test
	public void test1() throws Exception{
		performRefactoring(new NullRefactoring());
		checkState(true, false, 1, 0);
	}

	@Test
	public void test2() throws Exception{
		performRefactoring(new NullRefactoring());
		performUndo();
		checkState(false, true, 0, 1);
	}

	@Test
	public void test3() throws Exception{
		performRefactoring(new NullRefactoring());
		performUndo();
		performRedo();
		checkState(true, false, 1, 0);
	}

	@Test
	public void test4() throws Exception{
		performRefactoring(new NullRefactoring());
		performUndo();
		performRedo();
		performUndo();
		checkState(false, true, 0, 1);
	}

	@Test
	public void test5() throws Exception{
		performRefactoring(new NullRefactoring());
		performUndo();
		performRedo();
		performRefactoring(new NullRefactoring());
		checkState(true, false, 2, 0);
	}

	@Test
	public void test6() throws Exception{
		performRefactoring(new NullRefactoring());
		performUndo();
		performRedo();
		performRefactoring(new NullRefactoring());
		performUndo();
		performUndo();
		checkState(false, true, 0, 2);
	}

	@Test
	public void test7() throws Exception{
		performRefactoring(new NullRefactoring());
		performUndo();
		performRedo();
		performRefactoring(new NullRefactoring());
		performUndo();
		checkState(true, true, 1, 1);
	}

	@Test
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


	@Test
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
