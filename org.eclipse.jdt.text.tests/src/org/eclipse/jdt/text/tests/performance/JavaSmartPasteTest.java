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
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.jface.action.IAction;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;


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
	
	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	protected void setUp() throws Exception {
		super.setUp();
		fEditor= (AbstractTextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(DEST_FILE), true);
		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
	}
	
	protected void tearDown() throws Exception {
		super.tearDown();
		EditorTestHelper.closeAllEditors();
	}

	public void testSmartPaste() throws Exception {
		copyToClipboard(SRC_FILE, SRC_START_LINE, SRC_END_LINE);
		measurePaste(DEST_LINE, getNullPerformanceMeter(), getWarmUpRuns());
		measurePaste(DEST_LINE, createPerformanceMeter(), getMeasuredRuns());
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
