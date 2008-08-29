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

import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.ui.IWorkbenchPage;

import org.eclipse.ui.texteditor.AbstractTextEditor;


public abstract class ActivateEditorTest extends TextPerformanceTestCase {
	
	private static final Class THIS= ActivateEditorTest.class;
	
	private static final int WARM_UP_RUNS= 10;
	
	private static final int MEASURED_RUNS= 5;
	
	private static final int EDITORS= 30;
	
	private static final String FILE_PREFIX= "TextLayout";
	
	private static final String PREFIX= "/" + PerformanceTestSetup.PROJECT + "/Eclipse SWT/win32/org/eclipse/swt/graphics/" + FILE_PREFIX;
	
	private static final String FILE_SUFFIX= ".java";
	
	private String fShortName;
	
	private AbstractTextEditor[] fEditors;
	
	public ActivateEditorTest() {
		super();
	}
	
	public ActivateEditorTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		TestSuite suite= new TestSuite(THIS.getName());
		suite.addTest(ActivateJavaEditorTest.suite());
		suite.addTest(ActivateTextEditorTest.suite());
		return suite;
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
		ResourceTestHelper.replicate(PREFIX + FILE_SUFFIX, PREFIX, FILE_SUFFIX, getNumberOfEditors(), FILE_PREFIX, FILE_PREFIX, ResourceTestHelper.SKIP_IF_EXISTS);
		fEditors= EditorTestHelper.openInEditor(ResourceTestHelper.findFiles(PREFIX, FILE_SUFFIX, 0, getNumberOfEditors()), getEditorId());
	}
	
	protected void tearDown() throws Exception {
		super.tearDown();
		EditorTestHelper.closeAllEditors();
		ResourceTestHelper.delete(PREFIX, FILE_SUFFIX, getNumberOfEditors());
	}
	
	public void testActivateEditor() {
		measureActivateEditor(fEditors, getWarmUpRuns(), getNullPerformanceMeter());
		PerformanceMeter performanceMeter;
		if (getShortName() != null)
			performanceMeter= createPerformanceMeterForSummary(getShortName(), Dimension.ELAPSED_PROCESS);
		else
			performanceMeter= createPerformanceMeter();
		
		String degradationComment= getDegradationComment();
		if (degradationComment != null)
			explainDegradation(degradationComment, performanceMeter);
		
		measureActivateEditor(fEditors, getMeasuredRuns(), performanceMeter);
		commitAllMeasurements();
		assertAllPerformance();
	}
	
	protected String getDegradationComment() {
		return null;
	}
	
	protected void measureActivateEditor(AbstractTextEditor[] editors, int runs, PerformanceMeter performanceMeter) {
		IWorkbenchPage page= EditorTestHelper.getActivePage();
		for (int i= 0; i < runs; i++) {
			assertTrue(editors.length == 0 || editors[0] != page.getActivePart());
			performanceMeter.start();
			for (int j= 0, n= editors.length; j < n; j++) {
				AbstractTextEditor editor= editors[j];
				page.activate(editor);
				waitUntilReady(editor);
				EditorTestHelper.runEventQueue(editor);
			}
			performanceMeter.stop();
			for (int j= 0, n= editors.length; j < n; j++)
				EditorTestHelper.joinReconciler(EditorTestHelper.getSourceViewer(editors[j]), 100, 10000, 100);
		}
	}
	
	/**
	 * Waits until the editor is ready.
	 * 
	 * @param editor the editor
	 */
	protected void waitUntilReady(AbstractTextEditor editor) {
	}
	
	protected abstract String getEditorId();
	
	protected static int getNumberOfEditors() {
		return EDITORS;
	}
	
	protected final String getShortName() {
		return fShortName;
	}
	
	protected final void setShortName(String shortName) {
		fShortName= shortName;
	}
}
