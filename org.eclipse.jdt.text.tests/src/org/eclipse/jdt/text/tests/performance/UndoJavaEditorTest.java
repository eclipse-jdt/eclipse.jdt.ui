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

package org.eclipse.jdt.text.tests.performance;

import junit.framework.TestCase;

import org.eclipse.jface.action.IAction;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

public class UndoJavaEditorTest extends TestCase {
	
	private static final String FILE= "org.eclipse.swt/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/StyledText.java";

	private static final int N_OF_RUNS= 2;

	private PerformanceMeterFactory fPerformanceMeterFactory= Performance.createPerformanceMeterFactory();

	protected void setUp() {
		EditorTestHelper.runEventQueue();
	}

	public void testUndoJavaEditor1() throws PartInitException {
		// cold run
		measureUndo();
	}

	public void testUndoJavaEditor2() throws PartInitException {
		// warm run
		measureUndo();
	}

	private void measureUndo() throws PartInitException {
		PerformanceMeter performanceMeter= fPerformanceMeterFactory.createPerformanceMeter(this);
		try {
			AbstractDecoratedTextEditor editor= (AbstractDecoratedTextEditor) EditorTestHelper.openInEditor(EditorTestHelper.findFile(FILE), true);
			editor.showChangeInformation(false); // TODO: remove when undo does no longer trigger timing issue

			IAction selectAll= editor.getAction(ITextEditorActionConstants.SELECT_ALL);
			IAction shiftRight= editor.getAction(ITextEditorActionConstants.SHIFT_RIGHT);
			IAction undo= editor.getAction(ITextEditorActionConstants.UNDO);
			for (int i= 0; i < N_OF_RUNS; i++) {
				runAction(selectAll);
				runAction(shiftRight);
				sleep(5000);
				EditorTestHelper.runEventQueue();
				performanceMeter.start();
				runAction(undo);
				performanceMeter.stop();
				sleep(5000); // NOTE: runnables posted from other threads, while the main thread waits here, are not measured at all
			}
		} finally {
			EditorTestHelper.closeAllEditors();
			performanceMeter.commit();
		}
	}

	private void runAction(IAction action) {
		action.run();
		EditorTestHelper.runEventQueue();
	}

	private synchronized void sleep(int time) {
		try {
			wait(time);
		} catch (InterruptedException e) {
		}
	}
}
