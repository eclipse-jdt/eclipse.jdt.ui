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

import org.eclipse.core.resources.IFile;

import org.eclipse.ui.PartInitException;

/**
 * @since 3.1
 */
public class OpenJavaEditorTest extends OpenEditorTest {

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
			ResourceTestHelper.replicate(PREFIX + FILE_SUFFIX, PREFIX, FILE_SUFFIX, WARM_UP_RUNS + MEASURED_RUNS, FILE_PREFIX, FILE_PREFIX, ResourceTestHelper.SKIP_IF_EXISTS);
		}

		protected void tearDown() throws Exception {
			if (fTearDown)
				ResourceTestHelper.delete(PREFIX, FILE_SUFFIX, WARM_UP_RUNS + MEASURED_RUNS);
		}
	}

	private static final Class THIS= OpenJavaEditorTest.class;

	private static final String SHORT_NAME_FIRST_RUN= "Open Java editor (first in session)";

	private static final int WARM_UP_RUNS= 10;

	private static final int MEASURED_RUNS= 50;

	private static final String PATH= "/Eclipse SWT/win32/org/eclipse/swt/graphics/";

	private static final String FILE_PREFIX= "TextLayout";

	private static final String PREFIX= "/" + PerformanceTestSetup.PROJECT + PATH + FILE_PREFIX;

	private static final String FILE_SUFFIX= ".java";

	private static final String LARGE_FILE= "/org.eclipse.swt/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/StyledText.java";

	public OpenJavaEditorTest() {
		super();
	}

	public OpenJavaEditorTest(String name) {
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

		suite.addTest(new OpenJavaEditorTest("testOpenJavaEditor1"));
		suite.addTest(new OpenJavaEditorTest("testOpenJavaEditor2"));
		suite.addTest(new OpenJavaEditorTest("testOpenEditor3"));
		suite.addTest(new OpenJavaEditorTest("testOpenEditor4"));
		suite.addTest(new OpenJavaEditorTest("testOpenEditor5"));
		suite.addTest(new OpenJavaEditorTest("testOpenEditor6"));
		return new PerformanceTestSetup(new Setup(suite));
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
		measureOpenInEditor(new IFile[] { ResourceTestHelper.findFile(PREFIX + FILE_SUFFIX) }, performanceMeter, false);
	}

	public void testOpenJavaEditor1() throws Exception {
		measureOpenInEditor(ResourceTestHelper.findFiles(PREFIX, FILE_SUFFIX, 0, getWarmUpRuns()), Performance.getDefault().getNullPerformanceMeter(), false);
		measureOpenInEditor(ResourceTestHelper.findFiles(PREFIX, FILE_SUFFIX, getWarmUpRuns(), getMeasuredRuns()), createPerformanceMeter(), false);
	}

	public void testOpenJavaEditor2() throws Exception {
		measureOpenInEditor(ResourceTestHelper.findFiles(PREFIX, FILE_SUFFIX, 0, getWarmUpRuns()), Performance.getDefault().getNullPerformanceMeter(), false);
		PerformanceMeter performanceMeter= createPerformanceMeter();
		measureOpenInEditor(ResourceTestHelper.findFiles(PREFIX, FILE_SUFFIX, getWarmUpRuns(), getMeasuredRuns()), performanceMeter, false);
	}

	public void testOpenEditor3() throws Exception {
		PerformanceMeter performanceMeter= createPerformanceMeter();
		measureOpenInEditor(LARGE_FILE, true, true, performanceMeter);
	}

	public void testOpenEditor4() throws Exception {
		measureOpenInEditor(LARGE_FILE, false, true, createPerformanceMeter());
	}

	public void testOpenEditor5() throws Exception {
		measureOpenInEditor(LARGE_FILE, true, false, createPerformanceMeter());
	}

	public void testOpenEditor6() throws Exception {
		measureOpenInEditor(LARGE_FILE, false, false, createPerformanceMeter());
	}

	protected void measureOpenInEditor(String file, boolean enableFolding, boolean showOutline, PerformanceMeter performanceMeter) throws PartInitException {
		boolean shown= EditorTestHelper.isViewShown(EditorTestHelper.OUTLINE_VIEW_ID);
		try {
			EditorTestHelper.enableFolding(enableFolding);
			showOutline(showOutline);
			measureOpenInEditor(file, performanceMeter);
		} finally {
			EditorTestHelper.resetFolding();
			showOutline(shown);
		}
	}

	private boolean showOutline(boolean show) throws PartInitException {
		return EditorTestHelper.showView(EditorTestHelper.OUTLINE_VIEW_ID, show);
	}
}
