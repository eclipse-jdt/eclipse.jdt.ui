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

import org.eclipse.core.resources.IFile;

import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.jface.action.IAction;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

public abstract class UndoEditorTest extends TestCase {

	private PerformanceMeter fPerformanceMeter;

	protected void setUp() throws Exception {
		fPerformanceMeter= Performance.createPerformanceMeterFactory().createPerformanceMeter(this);
		EditorTestHelper.runEventQueue();
	}

	protected void tearDown() throws Exception {
		EditorTestHelper.closeAllEditors();
	}
	
	protected void measureUndo(IFile file, int nOfRuns) throws PartInitException {
		AbstractDecoratedTextEditor editor= (AbstractDecoratedTextEditor) EditorTestHelper.openInEditor(file, true);
		editor.showChangeInformation(false); // TODO: remove when undo does no longer trigger timing issue
		
		IAction selectAll= editor.getAction(ITextEditorActionConstants.SELECT_ALL);
		IAction shiftRight= editor.getAction(ITextEditorActionConstants.SHIFT_RIGHT);
		IAction undo= editor.getAction(ITextEditorActionConstants.UNDO);
		for (int i= 0; i < nOfRuns; i++) {
			runAction(selectAll);
			runAction(shiftRight);
			sleep(5000);
			EditorTestHelper.runEventQueue();
			fPerformanceMeter.start();
			runAction(undo);
			fPerformanceMeter.stop();
			sleep(5000); // NOTE: runnables posted from other threads, while the main thread waits here, are not measured at all
		}
		fPerformanceMeter.commit();
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
