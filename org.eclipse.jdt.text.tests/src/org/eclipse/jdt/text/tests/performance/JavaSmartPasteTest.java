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

import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * This test is currently disabled because the
 * results vary too much.
 */
public class JavaSmartPasteTest extends TextPerformanceTestCase {

	private static final Class THIS= JavaSmartPasteTest.class;

	private static final String SRC_FILE= "org.eclipse.swt/Eclipse SWT/win32/org/eclipse/swt/graphics/TextLayout.java";

	private static final String DEST_FILE= "org.eclipse.swt/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/StyledText.java";

	private static final int SRC_START_LINE= 168;

	private static final int SRC_END_LINE= 343;

	private static final int DEST_LINE= 7794;

	private static final int WARM_UP_RUNS= 5;

	private static final int MEASURED_RUNS= 5;

	private AbstractTextEditor fEditor;

	private static final String SHORT_NAME= "Smart paste in Java editor";

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	protected void setUp() throws Exception {
		super.setUp();
		fEditor= (AbstractTextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(DEST_FILE), true);
		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.EDITOR_SMART_PASTE, true);
		store.setValue(PreferenceConstants.EDITOR_IMPORTS_ON_PASTE, true);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		EditorTestHelper.closeAllEditors();
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setToDefault(PreferenceConstants.EDITOR_SMART_PASTE);
		store.setToDefault(PreferenceConstants.EDITOR_IMPORTS_ON_PASTE);
	}

	public void testSmartPaste() throws Exception {
		copyToClipboard(SRC_FILE, SRC_START_LINE, SRC_END_LINE);
		measurePaste(DEST_LINE, getNullPerformanceMeter(), getWarmUpRuns());
		PerformanceMeter performanceMeter= createPerformanceMeterForSummary(SHORT_NAME, Dimension.ELAPSED_PROCESS);
		measurePaste(DEST_LINE, performanceMeter, getMeasuredRuns());
		commitAllMeasurements();
		assertAllPerformance();
	}

	private void copyToClipboard(String srcFile, int startLine, int endLine) throws Exception {
		ITextEditor editor= (ITextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(srcFile), true);
		IDocument document= EditorTestHelper.getDocument(editor);
		int offset= document.getLineOffset(startLine);
		editor.selectAndReveal(offset, document.getLineOffset(endLine) - offset);
		runAction(editor.getAction(ITextEditorActionConstants.COPY));
		EditorTestHelper.closeEditor(editor);
	}

	private void measurePaste(int destLine, PerformanceMeter performanceMeter, int runs) throws Exception {
		IDocument document= EditorTestHelper.getDocument(fEditor);
		int destOffset= document.getLineOffset(destLine);
		IAction paste= fEditor.getAction(ITextEditorActionConstants.PASTE);
		for (int i= 0; i < runs; i++) {
			dirty(document);
			fEditor.selectAndReveal(destOffset, 0);
			EditorTestHelper.joinReconciler(EditorTestHelper.getSourceViewer(fEditor), 0, 10000, 100);
			performanceMeter.start();
			runAction(paste);
			performanceMeter.stop();
			EditorTestHelper.revertEditor(fEditor, true);
		}
	}

	private void dirty(IDocument document) throws BadLocationException {
		document.replace(0, 0, " ");
		document.replace(0, 1, "");
	}

	private void runAction(IAction action) {
		action.run();
		EditorTestHelper.runEventQueue();
	}
}
