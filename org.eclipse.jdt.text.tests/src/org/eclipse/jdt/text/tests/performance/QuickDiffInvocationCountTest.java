/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.text.tests.performance;


import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.ui.internal.texteditor.quickdiff.QuickDiffRangeDifference;

import org.eclipse.ui.texteditor.AbstractTextEditor;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * Measures the number of created {@link QuickDiffRangeDifference}
 * while QuickDiff is initializing with lots of changes in a large file.
 *
 * @since 3.1
 * @deprecated since INVOCATION_COUNT dimension is no longer supported.
 */
@Deprecated
public class QuickDiffInvocationCountTest extends TextPerformanceTestCase {

	private static final Class<QuickDiffInvocationCountTest> THIS= QuickDiffInvocationCountTest.class;

	private static final String FILE= "/" + PerformanceTestSetup.PROJECT + "/Eclipse SWT Custom Widgets/common/org/eclipse/swt/custom/TableTree.java";

	private AbstractTextEditor fEditor;

	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	/*
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		fEditor= (AbstractTextEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile(FILE), EditorTestHelper.TEXT_EDITOR_ID, true);
		fEditor.showChangeInformation(false);
		SourceViewer viewer= EditorTestHelper.getSourceViewer(fEditor);
		IDocument document= EditorTestHelper.getDocument(fEditor);
		DocumentRewriteSession rewriteSession= null;
		try {
			if (viewer != null)
				((ITextViewerExtension) viewer).getRewriteTarget().beginCompoundChange();
			if (document instanceof IDocumentExtension4)
				rewriteSession= ((IDocumentExtension4) document).startRewriteSession(DocumentRewriteSessionType.STRICTLY_SEQUENTIAL);
			for (int i= 0; i < document.getNumberOfLines(); i += 2) {
				document.replace(document.getLineOffset(i), 0, " ");
			}
		} finally {
			if (document instanceof IDocumentExtension4)
				((IDocumentExtension4) document).stopRewriteSession(rewriteSession);
			if (viewer != null)
				((ITextViewerExtension) viewer).getRewriteTarget().endCompoundChange();
		}
		EditorTestHelper.joinBackgroundActivities(fEditor);
	}

	/**
	 * Measures the number of created {@link QuickDiffRangeDifference}
	 * while QuickDiff is initializing with lots of changes in a large file.
	 *
	 * @throws Exception in case of problems
	 */
	public void test() throws Exception {
		PerformanceMeter performanceMeter= createInvocationCountPerformanceMeter(QuickDiffRangeDifference.class.getConstructors());
		performanceMeter.start();
		fEditor.showChangeInformation(true);
		EditorTestHelper.runEventQueue(5000); // ensure QuickDiff job started
		EditorTestHelper.joinBackgroundActivities(fEditor);
		performanceMeter.stop();
		commitAllMeasurements();
		assertAllPerformance();
	}

	/*
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		EditorTestHelper.closeAllEditors();

	}
}
