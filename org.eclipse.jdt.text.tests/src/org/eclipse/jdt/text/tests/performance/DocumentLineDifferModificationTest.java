/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.performance;


import org.eclipse.swt.widgets.Display;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IRegion;

import org.eclipse.test.performance.Performance;
import org.eclipse.ui.internal.texteditor.quickdiff.DocumentLineDiffer;

public class DocumentLineDifferModificationTest extends AbstractDocumentLineDifferTest {
	
	private static final Class THIS= DocumentLineDifferModificationTest.class;
	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	private DifferenceMeter fMeter;
	private Document fDocument;
	private FindReplaceDocumentAdapter fFindReplaceAdapter;
	private boolean fInitialized;
	
	protected void setUp() throws Exception {
		super.setUp();
		
		fDocument= new Document();
		fFindReplaceAdapter= new FindReplaceDocumentAdapter(fDocument);
	}

	protected void tearDown() throws Exception {
		if (fMeter != null) {
			fMeter.commit();
			Performance.getDefault().assertPerformance(fMeter);
			fMeter.dispose();
			fMeter= null;
		}
		
		super.tearDown();
	}
	
	public void testEditingUnchanged() throws Exception {
		setUpFast();
		
		runReplaceAllMeasurements(FAUST1, "MARGARETE", "MARGARINE");
	}
	
	public void testEditingChanged() throws Exception {
		setUpFast();
		
		runReplaceAllMeasurements(FAUST_FEW_CHANGES, "FAUST", "HEINRICH");
	}
	
	private void runReplaceAllMeasurements(String originalText, String searchExpression, String replacementString) throws Exception {
		DifferenceMeter meter= new DifferenceMeter("warm up");
		int runs= getWarmUpRuns();
		for (int run= 0; run < runs; run++)
			measureReplaceAll(meter, originalText, searchExpression, replacementString);
		
		fMeter= new DifferenceMeter(getDefaultScenarioId());
		runs= getMeasuredRuns();
		for (int run= 0; run < runs; run++)
			measureReplaceAll(fMeter, originalText, searchExpression, replacementString);
	}
	
	private void measureReplaceAll(DifferenceMeter meter, String contents, String searchExpression, String replacementString) throws Exception {
		DocumentLineDiffer differ= null;
		try {
			// reference measurement
			fDocument.set(contents);
			meter.startReference();
			replaceAll(searchExpression, replacementString);
			meter.stopReference();
			
			// difference measurement
			fDocument.set(contents);
			differ= ensureInitialized(fDocument);
			fInitialized= false;
			meter.startMeasured();
			replaceAll(searchExpression, replacementString);
			meter.stopMeasured();
			
			assertFalse("QuickDiff reinitialization makes performance results unusable", fInitialized);
		} finally {
			if (differ != null)
				differ.disconnect(fDocument);
		}
	}

	private void replaceAll(String searchExpression, String replacementString) throws BadLocationException {
		IRegion match= fFindReplaceAdapter.find(0, searchExpression, true, true, false, false);
		while (match != null) {
			IRegion replace= fFindReplaceAdapter.replace(replacementString, false);
			match= fFindReplaceAdapter.find(replace.getOffset() + replace.getLength(), searchExpression, true, true, false, false);
		}
	}
	
	private DocumentLineDiffer ensureInitialized(Document document) throws InterruptedException {
		final DocumentLineDiffer differ= new DocumentLineDiffer() {
			/*
			 * @see org.eclipse.ui.internal.texteditor.quickdiff.DocumentLineDiffer#initialize()
			 */
			protected synchronized void initialize() {
				fInitialized= true;
				super.initialize();
			}
		};
		setUpDiffer(differ);
		DisplayHelper helper= new DisplayHelper() {
			public boolean condition() {
				return differ.isSynchronized();
			}
		};
		differ.connect(document);
		
		assertTrue(helper.waitForCondition(Display.getDefault(), MAX_WAIT));

		return differ;
	}
}
