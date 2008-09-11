/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

import org.eclipse.jface.action.IAction;

import org.eclipse.ui.texteditor.AbstractTextEditor;

import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectionAction;

/**
 * Measures the time to semantically expand the selection in a large file in the
 * Java editor.
 *
 * @since 3.1
 */
public class JavaExpandSelectionTest extends TextPerformanceTestCase {

	private static final Class THIS= JavaExpandSelectionTest.class;

	private static final String FILE= PerformanceTestSetup.STYLED_TEXT;

	private static final int WARM_UP_RUNS= 5;

	private static final int MEASURED_RUNS= 5;

	private static final int LINE= 3347;

	private static final int COLUMN= 38;

	private static final int REPEAT= 10;

	private AbstractTextEditor fEditor;

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	protected void setUp() throws Exception {
		super.setUp();
		fEditor= (AbstractTextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(FILE), true);
		EditorTestHelper.joinBackgroundActivities(fEditor);
		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		EditorTestHelper.closeAllEditors();
	}

	/**
	 * Measures the time to semantically expand the selection in a large
	 * file in the Java editor.
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
		IAction action= fEditor.getAction(StructureSelectionAction.ENCLOSING);
		int offset= EditorTestHelper.getDocument(fEditor).getLineOffset(LINE) + COLUMN;
		for (int i= 0; i < runs; i++) {
			fEditor.selectAndReveal(offset, 0);
			performanceMeter.start();
			for (int j= 0; j < REPEAT; j++)
				runAction(action);
			performanceMeter.stop();
		}
	}

	private void runAction(IAction action) {
		action.run();
		EditorTestHelper.runEventQueue();
	}
}
