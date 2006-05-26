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
			ResourceTestHelper.copy(ORIG_LARGE_FILE, LARGE_FILE, ResourceTestHelper.SKIP_IF_EXISTS);
			EditorTestHelper.showPerspective(EditorTestHelper.RESOURCE_PERSPECTIVE_ID);
		}
		
		protected void tearDown() throws Exception {
			if (fTearDown) {
				EditorTestHelper.showPerspective(EditorTestHelper.JAVA_PERSPECTIVE_ID);
				ResourceTestHelper.delete(PREFIX, FILE_SUFFIX, WARM_UP_RUNS + MEASURED_RUNS);
				ResourceTestHelper.delete(LARGE_FILE);
			}
		}
	}
	
	private static final Class THIS= OpenTextEditorTest.class;

	private static final String SHORT_NAME_FIRST_RUN= "Open text editor (first in session)";

	private static final String SHORT_NAME_WARM_RUN= "Open text editor (reopen)";

	private static final int WARM_UP_RUNS= 10;
	
	private static final int MEASURED_RUNS= 200;
	
	private static final String PATH= "/Eclipse SWT/win32/org/eclipse/swt/graphics/";
	
	private static final String FILE_PREFIX= "TextLayout";
	
	private static final String FILE_SUFFIX= ".txt";
	
	private static final String ORIG_FILE= PATH + FILE_PREFIX + ".java";
	
	private static final String PREFIX= "/" + PerformanceTestSetup.PROJECT + PATH + FILE_PREFIX;

	private static final String LARGE_FILE_PREFIX= "/org.eclipse.swt/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/StyledText";
	
	private static final String ORIG_LARGE_FILE= LARGE_FILE_PREFIX + ".java";

	private static final String LARGE_FILE= LARGE_FILE_PREFIX + ".txt";

	public OpenTextEditorTest() {
		super();
	}

	public OpenTextEditorTest(String name) {
		super(name);
	}

	public static Test suite() {
		// ensure sequence
		TestSuite suite= new TestSuite(THIS.getName());
		
		/*
		 * Petty useless since it only does one run and hence the
		 * measured values have a very big dispersion.
		 */
//		suite.addTest(new OpenTextEditorTest("testOpenFirstEditor"));
		
		suite.addTest(new OpenTextEditorTest("testOpenTextEditor1"));
		suite.addTest(new OpenTextEditorTest("testOpenTextEditor2"));
		suite.addTest(new OpenTextEditorTest("testOpenEditor3"));
		return new PerformanceTestSetup(new Setup(suite), false);
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
		measureOpenInEditor(ResourceTestHelper.findFiles(PerformanceTestSetup.PROJECT + PATH + FILE_PREFIX, FILE_SUFFIX, 0, 1), performanceMeter, false);
	}
	
	public void testOpenTextEditor1() throws Exception {
		measureOpenInEditor(ResourceTestHelper.findFiles(PerformanceTestSetup.PROJECT + PATH + FILE_PREFIX, FILE_SUFFIX, 0, getWarmUpRuns()), Performance.getDefault().getNullPerformanceMeter(), false);
		measureOpenInEditor(ResourceTestHelper.findFiles(PerformanceTestSetup.PROJECT + PATH + FILE_PREFIX, FILE_SUFFIX, getWarmUpRuns(), getMeasuredRuns()), createPerformanceMeter(), false);
	}
	
	public void testOpenTextEditor2() throws Exception {
		measureOpenInEditor(ResourceTestHelper.findFiles(PerformanceTestSetup.PROJECT + PATH + FILE_PREFIX, FILE_SUFFIX, 0, getWarmUpRuns()), Performance.getDefault().getNullPerformanceMeter(), false);
		PerformanceMeter performanceMeter= createPerformanceMeterForSummary(SHORT_NAME_WARM_RUN, Dimension.ELAPSED_PROCESS); 
		measureOpenInEditor(ResourceTestHelper.findFiles(PerformanceTestSetup.PROJECT + PATH + FILE_PREFIX, FILE_SUFFIX, getWarmUpRuns(), getMeasuredRuns()), performanceMeter, false);
	}
	
	public void testOpenEditor3() throws Exception {
		PerformanceMeter performanceMeter= createPerformanceMeter(); 
		measureOpenInEditor(LARGE_FILE, performanceMeter);
	}
}
