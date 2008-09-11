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

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.texteditor.AbstractTextEditor;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Measurements of the {@link org.eclipse.jdt.internal.ui.text.javadoc.JavaDocAutoIndentStrategy}.
 *
 * FIXME: The test is currently excluded because it only takes 0..2ms.
 *
 * @since 3.1
 */

public class JavaDocIndentStrategyTest extends TextPerformanceTestCase implements ITextEditorTestCase {

	private static class Setup extends TextEditorTestSetup {

		private static final String FILE= "Test.java";

		private IJavaProject fJavaProject;

		public Setup(Test test) {
			super(test);
		}

		protected void setUp() throws Exception {
			IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
			store.setValue(PreferenceConstants.EDITOR_CLOSE_JAVADOCS, true);
			store.setValue(PreferenceConstants.EDITOR_ADD_JAVADOC_TAGS, true);

			fJavaProject= EditorTestHelper.createJavaProject(PROJECT, LINKED_FOLDER);
			super.setUp();
		}

		protected String getFile() {
			return "/" + PROJECT + "/src/" + FILE;
		}

		protected void tearDown () throws Exception {
			super.tearDown();
			if (fJavaProject != null)
				JavaProjectHelper.delete(fJavaProject);

			IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
			store.setToDefault(PreferenceConstants.EDITOR_CLOSE_JAVADOCS);
			store.setToDefault(PreferenceConstants.EDITOR_ADD_JAVADOC_TAGS);
		}
	}

	public static final String PROJECT= "JavaDocIndentStrategyTest";

	public static final String LINKED_FOLDER= "/testResources/javaDocIndentStrategyTest1";

	private static final Class THIS= JavaDocIndentStrategyTest.class;

	private static final String SHORT_NAME= "JavaDoc Indent Strategy";

	private static final int WARM_UP_RUNS= 5;

	private static final int MEASURED_RUNS= 5;

	private static final int LINE= 1;

	private static final int COLUMN= 4;

	private AbstractTextEditor fEditor;

	public static Test suite() {
		return new PerformanceTestSetup(new Setup(new TestSuite(THIS)));
	}

	public void setEditor(AbstractTextEditor editor) {
		fEditor= editor;
	}

	protected void setUp() throws Exception {
		super.setUp();
		EditorTestHelper.bringToTop();
		EditorTestHelper.joinBackgroundActivities();

		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
	}

	/**
	 * Places the caret behind a Javadoc prefix after which the declaration
	 * of a method with many arguments and declared exceptions follows and
	 * measures the time it takes to auto edit when entering a newline. See
	 * also <code>testResources/javaDocIndentStrategyTest1/Test.java<code>.
	 *
	 * @throws Exception
	 */
	public void testJavaDocIndentStrategy() throws Exception {
		int destOffset= EditorTestHelper.getDocument(fEditor).getLineOffset(LINE) + COLUMN;
		measureJavaDocIndentStrategy(destOffset, getNullPerformanceMeter(), getWarmUpRuns());
		PerformanceMeter performanceMeter= createPerformanceMeterForSummary(SHORT_NAME, Dimension.ELAPSED_PROCESS);
		measureJavaDocIndentStrategy(destOffset, performanceMeter, getMeasuredRuns());
		commitAllMeasurements();
		assertAllPerformance();
	}

	private void measureJavaDocIndentStrategy(int destOffset, PerformanceMeter performanceMeter, int runs) throws Exception {
		IDocument document= EditorTestHelper.getDocument(fEditor);
		Display display= EditorTestHelper.getActiveDisplay();
		for (int i= 0; i < runs; i++) {
			dirty(document);
			fEditor.selectAndReveal(destOffset, 0);
			EditorTestHelper.joinReconciler(EditorTestHelper.getSourceViewer(fEditor), 0, 10000, 100);
			performanceMeter.start();
			SWTEventHelper.pressKeyCode(display, SWT.CR);
			performanceMeter.stop();
			EditorTestHelper.revertEditor(fEditor, true);
		}
	}

	private void dirty(IDocument document) throws BadLocationException {
		document.replace(0, 0, " ");
		document.replace(0, 1, "");
	}
}
