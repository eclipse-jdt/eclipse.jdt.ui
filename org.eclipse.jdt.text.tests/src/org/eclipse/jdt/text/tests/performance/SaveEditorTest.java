/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.internal.ui.util.CoreUtility;


/**
 * @since 3.1
 */
public abstract class SaveEditorTest extends TextPerformanceTestCase {

	private static final Class THIS= SaveEditorTest.class;

	private static final int WARM_UP_RUNS= 10;

	private static final int MEASURED_RUNS= 5;

	private static final String FILE_PREFIX= "StyledText";

	private static final String PREFIX= "/" + PerformanceTestSetup.PROJECT + "/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/" + FILE_PREFIX;

	private static final String FILE_SUFFIX= ".java";

	private AbstractTextEditor[] fEditors;

	private static final int EDITORS= 10;

	private boolean fWasAutobuilding;

	public static Test suite() {
		TestSuite suite= new TestSuite(THIS.getName());
		suite.addTest(SaveTextEditorTest.suite());
		suite.addTest(SaveJavaEditorTest.suite());
		return suite;
	}

	protected void setUp() throws Exception {
		super.setUp();
		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
		ResourceTestHelper.replicate(PREFIX + FILE_SUFFIX, PREFIX, FILE_SUFFIX, getNumberOfEditors(), FILE_PREFIX, FILE_PREFIX, ResourceTestHelper.SKIP_IF_EXISTS);
		fEditors= EditorTestHelper.openInEditor(ResourceTestHelper.findFiles(PREFIX, FILE_SUFFIX, 0, getNumberOfEditors()), getEditorId());
		EditorTestHelper.joinBackgroundActivities();
		fWasAutobuilding= CoreUtility.setAutoBuilding(false);
	}

	protected int getNumberOfEditors() {
		return EDITORS;
	}

	protected abstract String getEditorId();

	protected void tearDown() throws Exception {
		super.tearDown();
		EditorTestHelper.closeAllEditors();
		try {
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=72633
			ResourceTestHelper.delete(PREFIX, FILE_SUFFIX, getNumberOfEditors());
		} finally {
			CoreUtility.setAutoBuilding(fWasAutobuilding);
			EditorTestHelper.joinBackgroundActivities();
		}
	}

	public void test1() throws Exception {
		measureRevert(getNullPerformanceMeter(), getWarmUpRuns());
		measureRevert(createPerformanceMeter(), getMeasuredRuns());
		commitAllMeasurements();
		assertAllPerformance();
	}

	protected void measureRevert(PerformanceMeter performanceMeter, int runs) throws Exception {
		for (int i= 0; i < runs; i++) {
			for (int j= 0; j < getNumberOfEditors(); j++)
				dirtyEditor(fEditors[j]);
			for (int j= 0; j < getNumberOfEditors(); j++)
				EditorTestHelper.joinReconciler(EditorTestHelper.getSourceViewer(fEditors[j]), 100, 10000, 100);
			performanceMeter.start();
			for (int j= 0; j < getNumberOfEditors(); j++)
				EditorTestHelper.getActivePage().saveEditor(fEditors[j], false);
			performanceMeter.stop();
			for (int j= 0; j < getNumberOfEditors(); j++)
				assertFalse(fEditors[j].isDirty());
		}
	}

	private void dirtyEditor(ITextEditor editor) throws BadLocationException {
		IDocument document= EditorTestHelper.getDocument(editor);
		if (document.getLength() > 0 && document.getChar(0) == ' ')
			document.replace(0, 1, "");
		else
			document.replace(0, 0, " ");
		assertTrue(editor.isDirty());
	}
}
