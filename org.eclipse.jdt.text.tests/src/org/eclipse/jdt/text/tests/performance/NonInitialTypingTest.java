/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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

package org.eclipse.jdt.text.tests.performance;

import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;

import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

/**
 * Measures the time to type in one single method into a large file. Abstract
 * implementation.
 *
 * @since 3.1
 */
public abstract class NonInitialTypingTest extends TextPerformanceTestCase {

	private static final String FILE= PerformanceTestSetup.STYLED_TEXT;

	private static final char[] METHOD= ("""
		public int foobar(int iParam, Object oParam) {\r\
		return 42;\r\
		}\r""").toCharArray();

	private static final int WARM_UP_RUNS= 3;

	private static final int MEASURED_RUNS= 100;

	private ITextEditor fEditor;

	protected PerformanceMeter fMeter;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		EditorTestHelper.runEventQueue();
		fEditor= (ITextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(FILE), getEditorId(), true);
		// dirty editor to avoid initial dirtying / validate edit costs
		dirtyEditor();
		Performance performance= Performance.getDefault();
		fMeter= performance.createPerformanceMeter(getScenarioId());


		// FIXME: Currently removed from summary because unstable under Linux
//		String summaryName= getSummaryName();
//		if (summaryName != null)
//			performance.tagAsSummary(fMeter, summaryName, Dimension.ELAPSED_PROCESS);

		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
	}

	protected String getSummaryName() {
		return null;
	}

	protected abstract String getEditorId();

	protected String getScenarioId() {
		return Performance.getDefault().getDefaultScenarioId(this);
	}

	private void dirtyEditor() {
		fEditor.getSelectionProvider().setSelection(new TextSelection(0, 0));
		EditorTestHelper.runEventQueue();
		sleep(1000);

		Display display= EditorTestHelper.getActiveDisplay();
		getKeyboardProbe().pressChar('a', display);
		EditorTestHelper.runEventQueue();
		SWTEventHelper.pressKeyCode(display, SWT.BS);
		sleep(1000);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		sleep(1000);
		fMeter.dispose();
		EditorTestHelper.revertEditor(fEditor, true);
		EditorTestHelper.closeAllEditors();
	}

	/**
	 * Measures the time to type in one single method into a large file.
	 *
	 * @throws BadLocationException if the insert position can't be detected
	 */
	public void testTypeAMethod() throws BadLocationException {
		Display display= EditorTestHelper.getActiveDisplay();
		int offset= getInsertPosition();

		int warmUpRuns= getWarmUpRuns();
		int measuredRuns= getMeasuredRuns();
		for (int i= 0; i < warmUpRuns + measuredRuns; i++) {
			fEditor.getSelectionProvider().setSelection(new TextSelection(offset, 0));
			fEditor.getAction(ITextEditorActionConstants.SMART_ENTER_INVERSE).run();
			EditorTestHelper.runEventQueue(display, 1000);
			KeyboardProbe keyboardProbe= getKeyboardProbe();

			if (i >= warmUpRuns)
				fMeter.start();

			for (char element : METHOD) {
				keyboardProbe.pressChar(element, display);
				EditorTestHelper.runEventQueue();
			}

			if (i >= warmUpRuns)
				fMeter.stop();

			EditorTestHelper.revertEditor(fEditor, true);
			EditorTestHelper.runEventQueue(display, 1000);
		}
		fMeter.commit();
		assertPerformance(fMeter);
	}

	private synchronized void sleep(int time) {
		DisplayHelper.sleep(EditorTestHelper.getActiveDisplay(), time);
	}

	private int getInsertPosition() throws BadLocationException {
		IDocument document= EditorTestHelper.getDocument(fEditor);
		int lines= document.getNumberOfLines();
		int offset= document.getLineOffset(lines - 2);
		return offset;
	}
}
