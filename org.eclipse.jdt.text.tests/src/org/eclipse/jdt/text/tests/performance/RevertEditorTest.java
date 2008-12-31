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

import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.PartInitException;

import org.eclipse.ui.texteditor.ITextEditor;

public abstract class RevertEditorTest extends TextPerformanceTestCase {

	private static final int WARM_UP_RUNS= 10;

	private static final int MEASURED_RUNS= 5;

	private static final String REPLACE_TEXT= "XXX"; //$NON-NLS-1$

	private PerformanceMeter fPerformanceMeter;

	protected void setUp() throws Exception {
		super.setUp();
		Performance performance= Performance.getDefault();
		fPerformanceMeter= performance.createPerformanceMeter(performance.getDefaultScenarioId(this));
		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
	}

	protected void measureRevert(IFile file) throws PartInitException, BadLocationException {
		int warmUpRuns= getWarmUpRuns();
		int measuredRuns= getMeasuredRuns();
		ITextEditor part= (ITextEditor) EditorTestHelper.openInEditor(file, true);
		for (int i= 0; i < warmUpRuns + measuredRuns; i++) {
			dirtyEditor(part);
			if (i >= warmUpRuns)
				fPerformanceMeter.start();
			EditorTestHelper.revertEditor(part, true);
			if (i >= warmUpRuns)
				fPerformanceMeter.stop();
			EditorTestHelper.runEventQueue(2000);
		}

		fPerformanceMeter.commit();
		assertPerformance(fPerformanceMeter);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		fPerformanceMeter.dispose();
		EditorTestHelper.closeAllEditors();
	}

	protected void dirtyEditor(ITextEditor part) throws BadLocationException {
		IDocument document= EditorTestHelper.getDocument(part);
		int line= document.getNumberOfLines() / 2; // dirty middle line
		int offset= document.getLineOffset(line);
		document.replace(offset, 0, REPLACE_TEXT);
		EditorTestHelper.runEventQueue(part);
	}
}
