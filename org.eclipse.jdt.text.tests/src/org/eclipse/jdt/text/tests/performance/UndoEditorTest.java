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

import org.eclipse.core.resources.IFile;

import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.jface.action.IAction;

import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

public abstract class UndoEditorTest extends TextPerformanceTestCase {

	private PerformanceMeter fPerformanceMeter;

	protected void setUp() throws Exception {
		Performance performance= Performance.getDefault();
		fPerformanceMeter= performance.createPerformanceMeter(performance.getDefaultScenarioId(this));
		EditorTestHelper.runEventQueue();
	}

	protected void tearDown() throws Exception {
		EditorTestHelper.closeAllEditors();
		fPerformanceMeter.dispose();
	}
	
	protected void measureUndo(IFile file) throws PartInitException {
		AbstractDecoratedTextEditor editor= (AbstractDecoratedTextEditor) EditorTestHelper.openInEditor(file, true);
		editor.showChangeInformation(false); // TODO: remove when undo does no longer trigger timing issue
		
		IAction selectAll= editor.getAction(ITextEditorActionConstants.SELECT_ALL);
		IAction shiftRight= editor.getAction(ITextEditorActionConstants.SHIFT_RIGHT);
		IAction undo= editor.getAction(ITextEditorActionConstants.UNDO);
		int warmUpRuns= getWarmUpRuns();
		int measuredRuns= getMeasuredRuns();
		for (int i= 0; i < warmUpRuns + measuredRuns; i++) {
			runAction(selectAll);
			runAction(shiftRight);
			sleep(5000);
			EditorTestHelper.runEventQueue();
			if (i >= warmUpRuns)
				fPerformanceMeter.start();
			runAction(undo);
			if (i >= warmUpRuns)
				fPerformanceMeter.stop();
			sleep(5000); // NOTE: runnables posted from other threads, while the main thread waits here, are not measured at all
		}
		fPerformanceMeter.commit();
		Performance.getDefault().assertPerformance(fPerformanceMeter);
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
