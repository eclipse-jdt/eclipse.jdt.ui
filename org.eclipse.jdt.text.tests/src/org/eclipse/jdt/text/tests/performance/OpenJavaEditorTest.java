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

import org.eclipse.core.resources.IFile;

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
public class OpenJavaEditorTest extends OpenEditorTest {

	private static final Class THIS= OpenJavaEditorTest.class;

	private static final String SHORT_NAME_FIRST_RUN= "Open Java editor (first in session)";

	private static final String SHORT_NAME_WARM_RUN= "Open Java editor (reopen)";

	public static final int WARM_UP_RUNS= 10;
	
	public static final int MEASURED_RUNS= 5;
	
	public static final String PATH= "/Eclipse SWT/win32/org/eclipse/swt/graphics/";
	
	public static final String FILE_PREFIX= "TextLayout";
	
	public static final String FILE_SUFFIX= ".java";
	
	public OpenJavaEditorTest() {
		super();
	}

	public OpenJavaEditorTest(String name) {
		super(name);
	}

	public static Test suite() {
		// ensure sequence
		TestSuite suite= new TestSuite(THIS.getName());
		suite.addTest(new OpenJavaEditorTest("testOpenFirstEditor"));
		suite.addTest(new OpenJavaEditorTest("testOpenJavaEditor1"));
		suite.addTest(new OpenJavaEditorTest("testOpenJavaEditor2"));
		return new CloseWorkbenchDecorator(new PerformanceTestSetup(new OpenJavaEditorTestSetup(suite)));
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
		measureOpenInEditor(new IFile[] { ResourceTestHelper.findFile(PerformanceTestSetup.PROJECT + PATH + FILE_PREFIX + FILE_SUFFIX) }, performanceMeter);
	}
	
	public void testOpenJavaEditor1() throws Exception {
		measureOpenInEditor(ResourceTestHelper.findFiles(PerformanceTestSetup.PROJECT + PATH + FILE_PREFIX, FILE_SUFFIX, 0, getWarmUpRuns()), Performance.getDefault().getNullPerformanceMeter());
		measureOpenInEditor(ResourceTestHelper.findFiles(PerformanceTestSetup.PROJECT + PATH + FILE_PREFIX, FILE_SUFFIX, getWarmUpRuns(), getMeasuredRuns()), createPerformanceMeter());
	}
	
	public void testOpenJavaEditor2() throws Exception {
		measureOpenInEditor(ResourceTestHelper.findFiles(PerformanceTestSetup.PROJECT + PATH + FILE_PREFIX, FILE_SUFFIX, 0, getWarmUpRuns()), Performance.getDefault().getNullPerformanceMeter());
		PerformanceMeter performanceMeter= createPerformanceMeterForGlobalSummary(SHORT_NAME_WARM_RUN, Dimension.ELAPSED_PROCESS); 
		measureOpenInEditor(ResourceTestHelper.findFiles(PerformanceTestSetup.PROJECT + PATH + FILE_PREFIX, FILE_SUFFIX, getWarmUpRuns(), getMeasuredRuns()), performanceMeter);
	}
}
