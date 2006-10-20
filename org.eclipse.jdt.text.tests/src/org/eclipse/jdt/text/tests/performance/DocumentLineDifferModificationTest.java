/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.performance;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.swt.widgets.Display;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IRegion;

import org.eclipse.test.internal.performance.InternalPerformanceMeter;
import org.eclipse.test.internal.performance.OSPerformanceMeter;
import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.data.Scalar;
import org.eclipse.test.performance.Performance;
import org.eclipse.ui.internal.texteditor.quickdiff.DocumentLineDiffer;

public class DocumentLineDifferModificationTest extends AbstractDocumentLineDifferTest {
	
	/**
	 * An accumulating performance meter that tracks elapsed time
	 * between consecutive calls to <code>on</code> and
	 * <code>off</code> using <code>System.currentTimeMillis()</code>.
	 */
	private static final class DifferenceMeter extends InternalPerformanceMeter {

		private final InternalPerformanceMeter fReferenceMeter;
		private final InternalPerformanceMeter fMeasuredMeter;
		
		
		/**
		 * @param scenarioId the scenario id
		 */
		public DifferenceMeter(String scenarioId) {
			super(scenarioId);
			
			fReferenceMeter= new OSPerformanceMeter(scenarioId);
			fMeasuredMeter= new OSPerformanceMeter(scenarioId);
		}
		
		/*
		 * @see org.eclipse.test.performance.PerformanceMeter#start()
		 */
		public void start() {
			assertTrue(false);
		}
		
		/*
		 * @see org.eclipse.test.performance.PerformanceMeter#stop()
		 */
		public void stop() {
			assertTrue(false);
		}
		
		/*
		 * @see org.eclipse.test.performance.PerformanceMeter#dispose()
		 */
		public void dispose() {
			fReferenceMeter.dispose();
			fMeasuredMeter.dispose();
			super.dispose();
		}

		public Sample getSample() {
		    	Map properties= new HashMap();
		    	
		    	Sample reference= fReferenceMeter.getSample();
		    	DataPoint[] referencePoints= reference.getDataPoints();
		    	
		    	Sample measured= fMeasuredMeter.getSample();
		    	DataPoint[] measuredPoints= measured.getDataPoints();
		    	
		    	assertEquals(referencePoints.length, measuredPoints.length);
		    	
		    	DataPoint[] data= new DataPoint[referencePoints.length];
		    	for (int i= 0; i < measuredPoints.length; i++) {
		    		DataPoint r= referencePoints[i];
					DataPoint m= measuredPoints[i];
					
					data[i]= difference(m, r);
				}
		    	
		    	return new Sample(getScenarioName(), measured.getStartTime(), properties, data);
	    }
		
		/*
		 * @see org.eclipse.test.internal.performance.InternalPerformanceMeter#printSample(java.io.PrintStream, org.eclipse.test.internal.performance.data.Sample)
		 */
		private DataPoint difference(DataPoint minuend, DataPoint subtrahend) {
			Collection mDims= minuend.getDimensions2();
			int step= minuend.getStep();
			assertEquals(step, subtrahend.getStep());

			Map scalars= new HashMap();
			for (Iterator it= mDims.iterator(); it.hasNext();) {
				Dim dimension= (Dim) it.next();
				
				Scalar m= minuend.getScalar(dimension);
				Scalar s= subtrahend.getScalar(dimension);
				
				if (m != null && s != null) {
					long difference= m.getMagnitude() - s.getMagnitude();
					Scalar scalar= new Scalar(dimension, difference);
					scalars.put(dimension, scalar);
				}
			}
			
			return new DataPoint(step, scalars);
		}

		public void startReference() {
			fReferenceMeter.start();
		}

		public void stopReference() {
			fReferenceMeter.stop();
		}

		public void startMeasured() {
			fMeasuredMeter.start();
		}

		public void stopMeasured() {
			fMeasuredMeter.stop();
		}
	}
	
	
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
