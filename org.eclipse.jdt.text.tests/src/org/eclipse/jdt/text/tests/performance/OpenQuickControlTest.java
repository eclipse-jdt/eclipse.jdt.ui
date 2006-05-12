/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

import org.eclipse.jface.action.IAction;

import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.ui.texteditor.AbstractTextEditor;

/**
 * @since 3.1
 */
public abstract class OpenQuickControlTest extends TextPerformanceTestCase {

	private static final Class THIS= OpenQuickControlTest.class;

	private static final int MEASURED_RUNS= 200;
	
	private static final int WARM_UP_RUNS= 10;
	
	private static final String PATH= "/org.eclipse.swt/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/";
	
	private static final String ORIG_NAME= "StyledText";
	
	private static final String ORIG_FILE= PATH + ORIG_NAME + ".java";

	public static Test suite() {
		TestSuite suite= new TestSuite(THIS.getName());
		suite.addTest(OpenQuickOutlineTest.suite());
		suite.addTest(OpenJavaContentAssistTest.suite());
		return suite;
	}
	
	protected void tearDown() throws Exception {
		super.tearDown();
		ResourceTestHelper.delete(PATH + ORIG_NAME, ".java", getWarmUpRuns() + getMeasuredRuns());
	}

	protected void setUp() throws Exception {
		super.setUp();
		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
		ResourceTestHelper.replicate(ORIG_FILE, PATH + ORIG_NAME, ".java", getWarmUpRuns() + getMeasuredRuns(), ORIG_NAME, ORIG_NAME, ResourceTestHelper.OVERWRITE_IF_EXISTS);
		ResourceTestHelper.incrementalBuild();
		EditorTestHelper.bringToTop();
		EditorTestHelper.joinJobs(1000, 10000, 100);
	}

	protected abstract IAction setUpMeasurement(AbstractTextEditor editor) throws Exception;

	protected abstract void tearDownMeasurement(AbstractTextEditor editor) throws Exception;

	protected final void measureOpenQuickControl(PerformanceMeter coldMeter, PerformanceMeter warmMeter) throws Exception {
		measureOpenQuickControl(getNullPerformanceMeter(), getNullPerformanceMeter(), 0, getWarmUpRuns());
		measureOpenQuickControl(coldMeter, warmMeter, getWarmUpRuns(), getMeasuredRuns());
		commitAllMeasurements();
		assertAllPerformance();
	}

	private void measureOpenQuickControl(PerformanceMeter coldMeter, PerformanceMeter warmMeter, int index, int runs) throws Exception {
		for (int i= 0; i < runs; i++) {
			String name= ORIG_NAME + (index + i);
			String file= PATH + name + ".java";
			AbstractTextEditor editor= (AbstractTextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(file), true);
			EditorTestHelper.joinReconciler(EditorTestHelper.getSourceViewer(editor), 100, 10000, 100);
			
			measureOpenQuickControl(editor, coldMeter);
			measureOpenQuickControl(editor, warmMeter);
			
			EditorTestHelper.closeAllEditors();
		}
	}

	private void measureOpenQuickControl(AbstractTextEditor editor, PerformanceMeter performanceMeter) throws Exception {
		IAction openQuickControl= setUpMeasurement(editor);
		performanceMeter.start();
		runAction(openQuickControl);
		EditorTestHelper.runEventQueue();
		performanceMeter.stop();
		tearDownMeasurement(editor);
	}

	private void runAction(IAction action) {
		action.run();
		EditorTestHelper.runEventQueue();
	}
}
