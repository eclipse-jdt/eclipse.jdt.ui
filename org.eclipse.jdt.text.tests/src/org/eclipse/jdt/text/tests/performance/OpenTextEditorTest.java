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

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;

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

	public static class Setup extends TestSetup {
		
		private boolean fTearDown;

		public Setup(Test test) {
			this(test, true);
		}
		
		public Setup(Test test, boolean tearDown) {
			super(test);
			fTearDown= tearDown;
		}
		
		protected void setUp() throws Exception {
			ResourceTestHelper.replicate("/" + PerformanceTestSetup.PROJECT + ORIG_FILE, PREFIX, FILE_SUFFIX, WARM_UP_RUNS + MEASURED_RUNS, ResourceTestHelper.SKIP_IF_EXISTS);
			EditorTestHelper.showPerspective(PERSPECTIVE);
		}
		
		protected void tearDown() throws Exception {
			if (fTearDown) {
				EditorTestHelper.showPerspective(PerformanceTestSetup.PERSPECTIVE);
				ResourceTestHelper.delete(PREFIX, FILE_SUFFIX, WARM_UP_RUNS + MEASURED_RUNS);
			}
		}
	}
	
	private static final Class THIS= OpenTextEditorTest.class;

	private static final String SHORT_NAME_FIRST_RUN= "Open text editor (first in session)";

	private static final String SHORT_NAME_WARM_RUN= "Open text editor (reopen)";

	private static final String PERSPECTIVE= "org.eclipse.ui.resourcePerspective";

	private static final int WARM_UP_RUNS= 10;
	
	private static final int MEASURED_RUNS= 5;
	
	private static final String PATH= "/Eclipse SWT/win32/org/eclipse/swt/graphics/";
	
	private static final String FILE_PREFIX= "TextLayout";
	
	private static final String FILE_SUFFIX= ".txt";
	
	private static final String ORIG_FILE= PATH + FILE_PREFIX + ".java";
	
	private static final String PREFIX= "/" + PerformanceTestSetup.PROJECT + PATH + FILE_PREFIX;

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
		return new CloseWorkbenchDecorator(new PerformanceTestSetup(new Setup(suite), false));
	}
	
	/*
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		EditorTestHelper.runEventQueue();
		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
	}
	
	/*
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		EditorTestHelper.closeAllEditors();
	}
	
	public void testOpenFirstEditor() throws Exception {
		PerformanceMeter performanceMeter= createPerformanceMeterForSummary(SHORT_NAME_FIRST_RUN, Dimension.ELAPSED_PROCESS); 
		measureOpenInEditor(ResourceTestHelper.findFiles(PerformanceTestSetup.PROJECT + PATH + FILE_PREFIX, FILE_SUFFIX, 0, 1), performanceMeter);
	}
	
	public void testOpenTextEditor1() throws Exception {
		measureOpenInEditor(ResourceTestHelper.findFiles(PerformanceTestSetup.PROJECT + PATH + FILE_PREFIX, FILE_SUFFIX, 0, getWarmUpRuns()), Performance.getDefault().getNullPerformanceMeter());
		measureOpenInEditor(ResourceTestHelper.findFiles(PerformanceTestSetup.PROJECT + PATH + FILE_PREFIX, FILE_SUFFIX, getWarmUpRuns(), getMeasuredRuns()), createPerformanceMeter());
	}
	
	public void testOpenTextEditor2() throws Exception {
		measureOpenInEditor(ResourceTestHelper.findFiles(PerformanceTestSetup.PROJECT + PATH + FILE_PREFIX, FILE_SUFFIX, 0, getWarmUpRuns()), Performance.getDefault().getNullPerformanceMeter());
		PerformanceMeter performanceMeter= createPerformanceMeterForGlobalSummary(SHORT_NAME_WARM_RUN, Dimension.ELAPSED_PROCESS); 
		measureOpenInEditor(ResourceTestHelper.findFiles(PerformanceTestSetup.PROJECT + PATH + FILE_PREFIX, FILE_SUFFIX, getWarmUpRuns(), getMeasuredRuns()), performanceMeter);
	}
}
