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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.test.performance.PerformanceMeter;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

/**
 * Measures the time to replaceAll in a large file.
 * 
 * @since 3.1
 */
public class JavaReplaceAllTest extends TextPerformanceTestCase {
	
	private static final Class THIS= JavaReplaceAllTest.class;
	
	private static final String FILE= PerformanceTestSetup.TEXT_LAYOUT;
	
	private static final int WARM_UP_RUNS= 3;

	private static final int MEASURED_RUNS= 3;
	
	private static final char FIND= 'e';
	
	private static final char REPLACE= 'x';
	
	private AbstractTextEditor fEditor;

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		fEditor= (AbstractTextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(FILE), true);
		
		// FIXME: temporarily added for debugging purposes
		System.err.println("JavaReplaceAllTest - quick diff enabled: " + fEditor.isChangeInformationShowing());
		
		EditorTestHelper.joinBackgroundActivities(fEditor);
		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
	}
	
	protected void tearDown() throws Exception {
		super.tearDown();
		EditorTestHelper.closeAllEditors();
	}

	/**
	 * Measures the time to replaceAll in a large file.
	 * 
	 * @throws Exception
	 */
	public void test() throws Exception {
		measure(getNullPerformanceMeter(), getWarmUpRuns());
		measure(createPerformanceMeter(), getMeasuredRuns());
		commitAllMeasurements();
		assertAllPerformance();
	}

	private void measure(PerformanceMeter performanceMeter, int runs) throws Exception {
		IAction action= fEditor.getAction(ITextEditorActionConstants.FIND);
		Display display= EditorTestHelper.getActiveDisplay();
		for (int i= 0; i < runs; i++) {
			fEditor.selectAndReveal(0, 0);
			runAction(action);
			
			// Fill Find field
			SWTEventHelper.pressKeyChar(display, FIND, true);
			
			// Switch to Replace field			
			SWTEventHelper.keyCodeDown(display, SWT.MOD3, true);
			SWTEventHelper.pressKeyChar(display, 'e');
			SWTEventHelper.keyCodeUp(display, SWT.MOD3, true);

			// Fill Replace field			
			SWTEventHelper.pressKeyChar(display, REPLACE, true);
			
			performanceMeter.start();
			SWTEventHelper.keyCodeDown(display, SWT.MOD3, true);
			SWTEventHelper.pressKeyChar(display, 'a');
			SWTEventHelper.keyCodeUp(display, SWT.MOD3, true);
			performanceMeter.stop();
			SWTEventHelper.pressKeyChar(display, SWT.ESC);
			DisplayHelper helper= new DisplayHelper() {
				public boolean condition() {
					return fEditor.isDirty();
				}
			};
			assertTrue(helper.waitForCondition(display, 1000));
			EditorTestHelper.revertEditor(fEditor, true);
			EditorTestHelper.joinBackgroundActivities(fEditor);
		}
	}

	private void runAction(IAction action) {
		action.run();
		EditorTestHelper.runEventQueue();
	}
}
