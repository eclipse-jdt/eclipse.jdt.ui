/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.text.tests.performance;

import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.action.IAction;

import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

/**
 * Measures the time to replaceAll in a large file.
 *
 * @since 3.1
 */
public abstract class AbstractJavaReplaceAllTest extends TextPerformanceTestCase {

	private static final String FILE= PerformanceTestSetup.TEXT_LAYOUT;

	private static final int WARM_UP_RUNS= 3;

	private static final int MEASURED_RUNS= 3;

	private static final char FIND= 'e';

	private static final char REPLACE= 'x';

	private AbstractTextEditor fEditor;

	private String fShortName= null;


	protected void setUp() throws Exception {
		super.setUp();
		fEditor= (AbstractTextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(FILE), true);
		fEditor.showChangeInformation(isQuickDiffEnabled());
		EditorTestHelper.joinBackgroundActivities(fEditor);
		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
	}

	protected abstract boolean isQuickDiffEnabled();

	protected void tearDown() throws Exception {
		super.tearDown();
		EditorTestHelper.closeAllEditors();
	}

	/**
	 * Measures the time to replaceAll in a large file.
	 *
	 * @throws Exception if measure fails
	 */
	public void test() throws Exception {
		measure(getNullPerformanceMeter(), getWarmUpRuns());
		PerformanceMeter performanceMeter;
		if (getShortName() != null)
			performanceMeter= createPerformanceMeterForSummary(getShortName(), Dimension.ELAPSED_PROCESS);
		else
			performanceMeter= createPerformanceMeter();
		measure(performanceMeter, getMeasuredRuns());
		commitAllMeasurements();
		assertAllPerformance();
	}

	private void measure(PerformanceMeter performanceMeter, int runs) throws Exception {
		IAction action= fEditor.getAction(ITextEditorActionConstants.FIND);
		final Display display= EditorTestHelper.getActiveDisplay();
		for (int i= 0; i < runs; i++) {
			StyledText text= (StyledText) fEditor.getAdapter(Control.class);
			text.setSelection(0);
			runAction(action);

			// Fill Find field
			SWTEventHelper.pressKeyChar(display, FIND,false);

			// Switch to Replace field
			SWTEventHelper.pressKeyCode(display, SWT.TAB,false);

			// Fill Replace field
			SWTEventHelper.pressKeyChar(display, REPLACE,false);

			performanceMeter.start();

			// Press Replace All button via mnemonic
			SWTEventHelper.keyCodeDown(display, SWT.MOD3,false);
			SWTEventHelper.pressKeyChar(display, 'a',false);
			SWTEventHelper.keyCodeUp(display, SWT.MOD3,false);


			// Close Find/Replace dialog
			SWTEventHelper.pressKeyCode(display, SWT.ESC,false);

			EditorTestHelper.runEventQueue();
			DisplayHelper helper= new DisplayHelper() {
				public boolean condition() {
					return fEditor.isDirty() && display.getActiveShell() == fEditor.getEditorSite().getShell();
				}
			};
			assertTrue(helper.waitForCondition(display, 1000));

			performanceMeter.stop();

			try {
				EditorTestHelper.revertEditor(fEditor, true);
			} catch (IllegalArgumentException e) {
				// ignore because this can trigger a bug that got fixed in the 3.1 stream
			}
			EditorTestHelper.joinBackgroundActivities(fEditor);
		}
	}

	private void runAction(IAction action) {
		action.run();
	}


	protected final String getShortName() {
		return fShortName;
	}

	protected final void setShortName(String shortName) {
		fShortName= shortName;
	}

}
