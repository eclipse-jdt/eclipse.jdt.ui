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
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.ui.PartInitException;

/**
 * Opens 20 instances of org.eclipse.swt.graphics.TextLayout leaving
 * each of them open and then closes all together them (close all).
 * Then repeats above scenario.
 * <p>
 * This tests a mid-size file.
 * </p>
 * 
 * @since 3.1
 */
public class OpenTextEditorTest extends OpenEditorTest {

	private static final Class THIS= OpenTextEditorTest.class;

	private static final String SHORT_NAME_FIRST_RUN= "Open text editor (first in session)";

	private static final String SHORT_NAME_WARM_RUN= "Open text editor (reopen)";

	public static final int WARM_UP_RUNS= 10;
	
	public static final int MEASURED_RUNS= 5;
	
	public static final String PATH= "/Eclipse SWT/win32/org/eclipse/swt/graphics/";
	
	public static final String FILE_PREFIX= "TextLayout";
	
	public static final String FILE_SUFFIX= ".txt";
	
	public static final String ORIG_FILE= PATH + FILE_PREFIX + ".java";

	public OpenTextEditorTest() {
		super();
	}

	public OpenTextEditorTest(String name) {
		super(name);
	}

	public static Test suite() {
		// ensure sequence
		TestSuite suite= new TestSuite(THIS.getName());
		suite.addTest(new OpenTextEditorTest("testOpenFirstEditor"));
		suite.addTest(new OpenTextEditorTest("testOpenTextEditor1"));
		suite.addTest(new OpenTextEditorTest("testOpenTextEditor2"));
		return new CloseWorkbenchDecorator(new PerformanceTestSetup(new OpenTextEditorTestSetup(suite)));
	}
	
	protected void setUp() {
		EditorTestHelper.runEventQueue();
		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
	}

	public void testOpenFirstEditor() throws PartInitException {
		Performance performance= Performance.getDefault();
		PerformanceMeter performanceMeter= performance.createPerformanceMeter(performance.getDefaultScenarioId(this));
		performance.tagAsSummary(performanceMeter, SHORT_NAME_FIRST_RUN, Dimension.ELAPSED_PROCESS); 
		measureOpenInEditor(ResourceTestHelper.findFiles(PerformanceTestSetup.PROJECT + PATH + FILE_PREFIX, FILE_SUFFIX, 0, 1), performanceMeter, true);
	}
	
	public void testOpenTextEditor1() throws PartInitException {
		measureOpenInEditor(ResourceTestHelper.findFiles(PerformanceTestSetup.PROJECT + PATH + FILE_PREFIX, FILE_SUFFIX, 0, getWarmUpRuns()), null, false);
		Performance performance= Performance.getDefault();
		PerformanceMeter performanceMeter= performance.createPerformanceMeter(performance.getDefaultScenarioId(this));
		measureOpenInEditor(ResourceTestHelper.findFiles(PerformanceTestSetup.PROJECT + PATH + FILE_PREFIX, FILE_SUFFIX, getWarmUpRuns(), getMeasuredRuns()), performanceMeter, true);
	}
	
	public void testOpenTextEditor2() throws PartInitException {
		measureOpenInEditor(ResourceTestHelper.findFiles(PerformanceTestSetup.PROJECT + PATH + FILE_PREFIX, FILE_SUFFIX, 0, getWarmUpRuns()), null, false);
		Performance performance= Performance.getDefault();
		PerformanceMeter performanceMeter= performance.createPerformanceMeter(performance.getDefaultScenarioId(this));
		performance.tagAsGlobalSummary(performanceMeter, SHORT_NAME_WARM_RUN, Dimension.ELAPSED_PROCESS); 
		measureOpenInEditor(ResourceTestHelper.findFiles(PerformanceTestSetup.PROJECT + PATH + FILE_PREFIX, FILE_SUFFIX, getWarmUpRuns(), getMeasuredRuns()), performanceMeter, true);
	}
}
