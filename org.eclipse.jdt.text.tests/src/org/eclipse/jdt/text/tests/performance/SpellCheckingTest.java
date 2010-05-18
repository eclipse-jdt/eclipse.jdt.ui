/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentTypeManager;

import org.eclipse.core.resources.IFile;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector;
import org.eclipse.ui.texteditor.spelling.SpellingContext;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;
import org.eclipse.ui.texteditor.spelling.SpellingService;

import org.eclipse.ui.editors.text.EditorsUI;

import org.eclipse.jdt.ui.PreferenceConstants;


/**
 * Measures the time to spell check a large compilation unit.
 * 
 * @since 3.6
 */
public class SpellCheckingTest extends TextPerformanceTestCase {

	private static final Class THIS= SpellCheckingTest.class;

	private static final String FILE= PerformanceTestSetup.STYLED_TEXT;

	private static final int WARM_UP_RUNS= 3;

	private static final int MEASURED_RUNS= 50;

	private IDocument fDocument;

	private SpellingContext fSpellingContext;


	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	protected void setUp() throws Exception {
		super.setUp();
		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);

		PreferenceConstants.getPreferenceStore().setValue(PreferenceConstants.SPELLING_PROBLEMS_THRESHOLD, Integer.MAX_VALUE);
		EditorsUI.getPreferenceStore().putValue(SpellingService.PREFERENCE_SPELLING_ENABLED, IPreferenceStore.TRUE);

		fSpellingContext= new SpellingContext();
		fSpellingContext.setContentType(Platform.getContentTypeManager().getContentType(IContentTypeManager.CT_TEXT));

		IFile file= ResourceTestHelper.findFile(FILE);
		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		try {
			manager.connect(file.getFullPath(), LocationKind.IFILE, null);
			ITextFileBuffer fileBuffer= manager.getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
			fDocument= fileBuffer.getDocument();
		} catch (CoreException e) {
			throw e;
		} finally {
			manager.disconnect(file.getFullPath(), LocationKind.IFILE, null);
		}
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		PreferenceConstants.getPreferenceStore().setToDefault(PreferenceConstants.SPELLING_PROBLEMS_THRESHOLD);
		EditorsUI.getPreferenceStore().setToDefault(SpellingService.PREFERENCE_SPELLING_ENABLED);
	}

	public void test() throws Exception {
		measure(getNullPerformanceMeter(), getWarmUpRuns(), true);
		PerformanceMeter performanceMeter= createPerformanceMeter("Java Editor: Spell checking");
		measure(performanceMeter, getMeasuredRuns(), false);
		commitAllMeasurements();
		assertAllPerformance();
	}

	private void measure(PerformanceMeter performanceMeter, int runs, boolean printDebugInfo) {
		SpellingService spellingService= EditorsUI.getSpellingService();
		for (int i= 0; i < runs; i++) {
			performanceMeter.start();
			spellingService.check(fDocument, fSpellingContext, new SpellingProblemCollector(printDebugInfo && i == 0), null);
			performanceMeter.stop();
		}
	}

	/**
	 * Spelling problem collector.
	 */
	private static class SpellingProblemCollector implements ISpellingProblemCollector {

		private int fProblemCount;
		private boolean fPrintCount;

		
		public SpellingProblemCollector(boolean printCount) {
			fPrintCount= printCount;
		}

		/*
		 * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#accept(org.eclipse.ui.texteditor.spelling.SpellingProblem)
		 */
		public void accept(SpellingProblem problem) {
			fProblemCount++;
		}

		/*
		 * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#beginCollecting()
		 */
		public void beginCollecting() {
			fProblemCount= 0;
		}

		/*
		 * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#endCollecting()
		 */
		public void endCollecting() {
			if (fPrintCount)
				System.out.println("No of spelling problems : " + fProblemCount);
		}
	}

}
