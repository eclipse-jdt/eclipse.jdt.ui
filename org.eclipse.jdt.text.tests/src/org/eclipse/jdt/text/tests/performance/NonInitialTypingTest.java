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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;

import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Measures the time to type in one single method into a large file. Abstract implementation.
 * @since 3.1
 */
public abstract class NonInitialTypingTest extends TextPerformanceTestCase {
	
	private static final String FILE= "org.eclipse.swt/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/StyledText.java";

	private static final char[] METHOD= ("public int foobar(int iParam, Object oParam) {\r" +
			"return 42;\r" +
			"}\r").toCharArray();
	
	private static final int WARM_UP_RUNS= 3;
	
	private static final int MEASURED_RUNS= 3;

	private ITextEditor fEditor;
	
	protected PerformanceMeter fMeter;

	private KeyboardProbe fKeyboardProbe;

	protected void setUp() throws Exception {
		EditorTestHelper.runEventQueue();
		fEditor= (ITextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(FILE), getEditorId(), true);
		// dirty editor to avoid initial dirtying / validate edit costs
		fKeyboardProbe= new KeyboardProbe();
		dirtyEditor();
		Performance performance= Performance.getDefault();
		fMeter= performance.createPerformanceMeter(getScenarioId());
		String summaryName= getSummaryName();
		if (summaryName != null)
			performance.tagAsSummary(fMeter, summaryName, Dimension.ELAPSED_PROCESS);
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
		fKeyboardProbe.pressChar('{', display);
		EditorTestHelper.runEventQueue();
		SWTEventHelper.pressKeyCode(display, SWT.BS);
		sleep(1000);
	}

	protected void tearDown() throws Exception {
		sleep(1000);
		fMeter.dispose();
		EditorTestHelper.revertEditor(fEditor, true);
		EditorTestHelper.closeAllEditors();
	}

	public void testTypeAMethod() throws BadLocationException {
		Display display= EditorTestHelper.getActiveDisplay();
		int offset= getInsertPosition();
		
		int warmUpRuns= getWarmUpRuns();
		int measuredRuns= getMeasuredRuns();
		for (int i= 0; i < warmUpRuns + measuredRuns; i++) {
			fEditor.getSelectionProvider().setSelection(new TextSelection(offset, 0));
			EditorTestHelper.runEventQueue(1000);
			
			if (i >= warmUpRuns)
				fMeter.start();
			for (int j= 0; j < METHOD.length; j++) {
				fKeyboardProbe.pressChar(METHOD[j], display);
				EditorTestHelper.runEventQueue();
			}
			if (i >= warmUpRuns)
				fMeter.stop();
			EditorTestHelper.revertEditor(fEditor, true);
		}
		fMeter.commit();
		Performance.getDefault().assertPerformance(fMeter);
	}

	private synchronized void sleep(int time) {
		try {
			wait(time);
		} catch (InterruptedException e) {
		}
	}
	
	private int getInsertPosition() throws BadLocationException {
		IDocument document= EditorTestHelper.getDocument(fEditor);
		int lines= document.getNumberOfLines();
		int offset= document.getLineOffset(lines - 2);
		return offset;
	}
}
