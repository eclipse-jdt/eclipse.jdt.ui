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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.action.IAction;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

public class JavaIndenterTest extends TextPerformanceTestCase {

	private static final Class THIS= JavaIndenterTest.class;

	private static final String FILE= PerformanceTestSetup.TEXT_LAYOUT;

	private static final int WARM_UP_RUNS= 2;

	private static final int MEASURED_RUNS= 2;

	private static final int[] CTRL_END= new int[] { SWT.CTRL, SWT.END };

	private ITextEditor fEditor;

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	protected void setUp() throws Exception {
		super.setUp();
		EditorTestHelper.runEventQueue();

		EditorTestHelper.bringToTop();
		fEditor= (ITextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(FILE), true);
		runAction(fEditor.getAction(ITextEditorActionConstants.SELECT_ALL));
		runAction(fEditor.getAction("ToggleComment"));
		SWTEventHelper.pressKeyCodeCombination(EditorTestHelper.getActiveDisplay(), CTRL_END);
		EditorTestHelper.joinJobs(2000, 5000, 100);

		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		EditorTestHelper.closeAllEditors();
	}

	public void testJavaIndenter2() {
		measureJavaIndenter(getNullPerformanceMeter(), getWarmUpRuns());
		measureJavaIndenter(createPerformanceMeter(), getMeasuredRuns());
		commitAllMeasurements();
		assertAllPerformance();
	}

	private void measureJavaIndenter(PerformanceMeter performanceMeter, int runs) {
		final IDocument document= EditorTestHelper.getDocument(fEditor);
		Display display= EditorTestHelper.getActiveDisplay();
		IAction undo= fEditor.getAction(ITextEditorActionConstants.UNDO);
		final int originalNumberOfLines= document.getNumberOfLines();
		for (int i= 0; i < runs; i++) {
			DisplayHelper helper= new DisplayHelper() {
				protected boolean condition() {
					return document.getNumberOfLines() == originalNumberOfLines + 1;
				}
			};
			performanceMeter.start();
			SWTEventHelper.pressKeyCode(display, SWT.CR, false);
			boolean success= helper.waitForCondition(display, 5000);
			performanceMeter.stop();
			assertTrue(success);
			runAction(undo);
			helper= new DisplayHelper() {
				protected boolean condition() {
					return document.getNumberOfLines() == originalNumberOfLines;
				}
			};
			assertTrue(helper.waitForCondition(display, 1000));
		}
	}

	private void runAction(IAction action) {
		action.run();
		EditorTestHelper.runEventQueue();
	}
}
