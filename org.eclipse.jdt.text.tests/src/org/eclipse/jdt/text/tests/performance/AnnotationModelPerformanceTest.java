/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.performance;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.test.performance.PerformanceMeter;


/**
 * @since 3.4
 */
public class AnnotationModelPerformanceTest extends TextPerformanceTestCase {
	
	private static final Class THIS= AnnotationModelPerformanceTest.class;
	
	public static Test suite() {
		return new TestSuite(THIS);
	}
	
	private static final int COUNT= 20000;
	
	private Document fDocument;
	private AnnotationModel fAnnotationModel;
	
	private Annotation[] fAnnotations;
	private Position[] fPositions;
	
	private Random fRandom;
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.text.tests.performance.TextPerformanceTestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		
		setWarmUpRuns(1);
		setMeasuredRuns(5);
		
		String faust= FileTool.read(new InputStreamReader(AbstractDocumentPerformanceTest.class.getResourceAsStream("faust1.txt"))).toString();
		fDocument= new Document(faust);
		
		fAnnotationModel= new AnnotationModel();
		fAnnotationModel.connect(fDocument);
		
		fAnnotations= new Annotation[COUNT];
		fPositions= new Position[COUNT];
		for (int i= 0; i < fAnnotations.length; i++) {
			fAnnotations[i]= new Annotation(false);
			fPositions[i]= new Position(i, 0);
		}
		
		fRandom= new Random(4711);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.text.tests.performance.TextPerformanceTestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		fAnnotationModel.disconnect(fDocument);
		
		super.tearDown();
	}
	
	private void removeAll() {
		fAnnotationModel.removeAllAnnotations();
	}
	
	private void addForward(int count) {
		for (int i= 0; i < count; i++) {
			fAnnotationModel.addAnnotation(fAnnotations[i], fPositions[i]);
		}
	}
	
	private void addBackwards(int count) {
		for (int i= 0; i < count; i++) {
			fAnnotationModel.addAnnotation(fAnnotations[i], fPositions[count - i - 1]);
		}
	}
	
	private void addRandom(int count) {
		for (int i= 0; i < count; i++) {
			fAnnotationModel.addAnnotation(fAnnotations[i], fPositions[fRandom.nextInt(COUNT)]);
		}
	}
	
	private Collection getInRegion(boolean lookAhead, boolean lookBehind) {
		Iterator iterator= fAnnotationModel.getAnnotationIterator(2000, COUNT - 2000, lookAhead, lookBehind);
		
		ArrayList result= new ArrayList();
		while (iterator.hasNext()) {
			result.add(iterator.next());
		}
		
		return result;
	}
	
	private Collection getInRegionOld() {
		Iterator iterator= fAnnotationModel.getAnnotationIterator();
		
		ArrayList result= new ArrayList();
		while (iterator.hasNext()) {
			Annotation annotation= (Annotation) iterator.next();
			Position position= fAnnotationModel.getPosition(annotation);
			if (position.overlapsWith(2000, COUNT - 2000)) {
				result.add(annotation);
			}
		}
		
		return result;
	}
	
	public void testAddForward() throws Exception {
		PerformanceMeter meter= createPerformanceMeter();
		int warmUpRuns= getWarmUpRuns();
		for (int i= 0; i < warmUpRuns; i++) {
			addForward(COUNT);
			removeAll();
		}
		
		int measuredRuns= getMeasuredRuns();
		for (int i= 0; i < measuredRuns; i++) {
			meter.start();
			addForward(COUNT);
			meter.stop();
			removeAll();
		}
		meter.commit();
	}
	
	public void testAddBackwards() throws Exception {
		PerformanceMeter meter= createPerformanceMeter();
		int warmUpRuns= getWarmUpRuns();
		for (int i= 0; i < warmUpRuns; i++) {
			addBackwards(COUNT);
			removeAll();
		}
		
		int measuredRuns= getMeasuredRuns();
		for (int i= 0; i < measuredRuns; i++) {
			meter.start();
			addBackwards(COUNT);
			meter.stop();
			removeAll();
		}
		meter.commit();
	}
	
	public void testAddRandom() throws Exception {
		PerformanceMeter meter= createPerformanceMeter();
		int warmUpRuns= getWarmUpRuns();
		for (int i= 0; i < warmUpRuns; i++) {
			addRandom(COUNT);
			removeAll();
		}
		
		int measuredRuns= getMeasuredRuns();
		for (int i= 0; i < measuredRuns; i++) {
			meter.start();
			addRandom(COUNT);
			meter.stop();
			removeAll();
		}
		meter.commit();
	}
	
	public void testRemoveRandom() throws Exception {
		PerformanceMeter meter= createPerformanceMeter();
		int warmUpRuns= getWarmUpRuns();
		for (int i= 0; i < warmUpRuns; i++) {
			addRandom(COUNT);
			removeAll();
		}
		
		int measuredRuns= getMeasuredRuns();
		for (int i= 0; i < measuredRuns; i++) {
			addRandom(COUNT);
			ArrayList annotations= new ArrayList(Arrays.asList(fAnnotations));
			Collections.shuffle(annotations);
			meter.start();
			for (int j= 0; j < COUNT; j++) {
				fAnnotationModel.removeAnnotation((Annotation) annotations.get(j));
			}
			meter.stop();
			removeAll();
		}
		meter.commit();
	}

	public void testRegionInside() throws Exception {
		PerformanceMeter meter= createPerformanceMeter();
		int warmUpRuns= getWarmUpRuns();
		addRandom(COUNT);
		for (int i= 0; i < warmUpRuns; i++) {
			getInRegion(false, false);
		}
		removeAll();
		
		int measuredRuns= getMeasuredRuns();
		addRandom(COUNT);
		for (int i= 0; i < measuredRuns; i++) {
			meter.start();
			for (int j= 0; j < 20; j++) {
				getInRegion(false, false);
			}
			meter.stop();
		}
		removeAll();
		meter.commit();
	}
	
	public void testRegionBeforeAfter() throws Exception {
		PerformanceMeter meter= createPerformanceMeter();
		int warmUpRuns= getWarmUpRuns();
		addRandom(COUNT);
		for (int i= 0; i < warmUpRuns; i++) {
			getInRegion(true, true);
		}
		removeAll();
		
		int measuredRuns= getMeasuredRuns();
		addRandom(COUNT);
		for (int i= 0; i < measuredRuns; i++) {
			meter.start();
			for (int j= 0; j < 20; j++) {
				getInRegion(true, true);
			}
			meter.stop();
		}
		removeAll();
		meter.commit();
	}

	public void testRegionBeforeAfterOld() throws Exception {
		PerformanceMeter meter= createPerformanceMeter();
		int warmUpRuns= getWarmUpRuns();
		addRandom(COUNT);
		for (int i= 0; i < warmUpRuns; i++) {
			getInRegionOld();
		}
		removeAll();
		
		int measuredRuns= getMeasuredRuns();
		addRandom(COUNT);
		for (int i= 0; i < measuredRuns; i++) {
			meter.start();
			for (int j= 0; j < 20; j++) {
				getInRegionOld();
			}
			meter.stop();
		}
		removeAll();
		meter.commit();
	}
	
	public void testRegionBeforeAfterCompare() throws Exception {
		DifferenceMeter meter= new DifferenceMeter(getBaseScenarioId());
		int warmUpRuns= getWarmUpRuns();
		addRandom(COUNT);
		for (int i= 0; i < warmUpRuns; i++) {
			getInRegion(true, true);
		}
		removeAll();
		
		int measuredRuns= getMeasuredRuns();
		addRandom(COUNT);
		for (int i= 0; i < measuredRuns; i++) {
			meter.startReference();
			for (int j= 0; j < 20; j++) {
				getInRegionOld();
			}
			meter.stopReference();
			
			meter.startMeasured();
			for (int j= 0; j < 20; j++) {
				getInRegion(true, true);
			}
			meter.stopMeasured();
		}
		removeAll();
		meter.commit();
	}
	
	
}
