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

package org.eclipse.jdt.text.tests.performance;

import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

import org.eclipse.jface.action.IAction;

import org.eclipse.ui.texteditor.AbstractTextEditor;

/**
 * Measures the time to move a line within a large file.
 *
 * @since 3.1
 */
public abstract class MoveLineTest extends TextPerformanceTestCase {

	private static final String FILE= PerformanceTestSetup.STYLED_TEXT;

	private static final int WARM_UP_RUNS= 5;

	private static final int MEASURED_RUNS= 5;

	private static final int LINE= 10;

	private static final int DISTANCE= 100;

	private AbstractTextEditor fEditor;

	protected void setUp() throws Exception {
		super.setUp();
		fEditor= (AbstractTextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(FILE), getEditorId(), true);
		EditorTestHelper.joinBackgroundActivities(fEditor);
		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
	}

	protected abstract String getEditorId();

	protected void tearDown() throws Exception {
		super.tearDown();
		EditorTestHelper.closeAllEditors();
	}

	/**
	 * Measures the time to move a line within a large file.
	 *
	 * @throws Exception
	 */
	public void test() throws Exception {
		measureMoveLine(getNullPerformanceMeter(), getWarmUpRuns());
		measureMoveLine(createPerformanceMeter(), getMeasuredRuns());
		commitAllMeasurements();
		assertAllPerformance();
	}

	private void measureMoveLine(PerformanceMeter performanceMeter, int runs) throws Exception {
		IAction expandAll= fEditor.getAction("FoldingExpandAll");
		int offset= EditorTestHelper.getDocument(fEditor).getLineOffset(LINE);

		// Use keyboard events to trigger optimization correctly
		Event event= new Event();
		event.keyCode= SWT.ARROW_DOWN;
		event.stateMask= SWT.MOD3;
		Display display= fEditor.getSite().getShell().getDisplay();
		for (int i= 0; i < runs; i++) {
			/*
			 * Avoid bug 68697: [projection] folding + move lines down/up can corrupt source
			 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=68697
			 */
			if (expandAll != null) {
				runAction(expandAll);
				EditorTestHelper.joinBackgroundActivities(fEditor);
			}

			fEditor.selectAndReveal(offset, 0);
			performanceMeter.start();
			SWTEventHelper.keyCodeDown(display, SWT.MOD3, true);
			for (int j= 0; j < DISTANCE; j++) {
				event.type= SWT.KeyDown;
				display.post(event);
				EditorTestHelper.runEventQueue();
				event.type= SWT.KeyUp;
				display.post(event);
				EditorTestHelper.runEventQueue();
			}
			SWTEventHelper.keyCodeUp(display, SWT.MOD3, true);
			performanceMeter.stop();

			/*
			 * In some cases in Eclipse 3.0 revert under Linux
			 * caused an NPE which we cannot yet explain. Setting
			 * the selection to (0, 0) prevents this from happening.
			 */
			fEditor.selectAndReveal(0, 0);
			EditorTestHelper.revertEditor(fEditor, true);

			EditorTestHelper.joinBackgroundActivities(fEditor);
		}
	}

	private void runAction(IAction action) {
		action.run();
		EditorTestHelper.runEventQueue();
	}
}
