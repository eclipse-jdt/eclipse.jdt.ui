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

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Random;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.ILineTracker;

import org.eclipse.test.performance.PerformanceMeter;


/**
 * 
 * @since 3.2
 */
public class LineTrackerPerformanceTest extends TextPerformanceTestCase {
	
	protected static final String FAUST1;
	protected static final String FAUST500;
	
	static {
		String faust;
		try {
			faust= FileTool.read(new InputStreamReader(AbstractDocumentLineDifferTest.class.getResourceAsStream("faust1.txt"))).toString();
		} catch (IOException x) {
			faust= "";
			x.printStackTrace();
		}
		FAUST1= faust;
		FAUST500= faust.substring(0, 500);
	}


	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(LineTrackerPerformanceTest.class));
	}

	public static Test setUpTest(Test test) {
		return new PerformanceTestSetup(test);
	}

	protected ILineTracker createLineTracker() {
		return new DefaultLineTracker();
	}

	private ILineTracker fLineTracker;

	protected void setUp() throws Exception {
		super.setUp();
		setWarmUpRuns(20);
		setMeasuredRuns(20);

		fLineTracker= createLineTracker();
	}

	public void testSet() throws Exception {
		PerformanceMeter meter= getNullPerformanceMeter();
		int runs= getWarmUpRuns();
		for (int run= 0; run < runs; run++)
			measureSet(meter);
		
		meter= createPerformanceMeter();
		runs= getMeasuredRuns();
		for (int run= 0; run < runs; run++)
			measureSet(meter);
		
		commitAllMeasurements();
		assertAllPerformance();
		
	}
	
	private void measureSet(PerformanceMeter meter) {
		meter.start();
		for (int accumulated= 0; accumulated < 100; accumulated++) {
			fLineTracker.set(FAUST1);
		}
		meter.stop();
	}
	
	public void testReplace() throws Exception {
		fLineTracker.set(FAUST1);
		
		PerformanceMeter meter= getNullPerformanceMeter();
		int runs= getWarmUpRuns();
		for (int run= 0; run < runs; run++)
			measureReplace(meter);
		
		meter= createPerformanceMeter();
		runs= getMeasuredRuns();
		for (int run= 0; run < runs; run++)
			measureReplace(meter);
		
		commitAllMeasurements();
		assertAllPerformance();

	}

	private void measureReplace(PerformanceMeter meter) throws BadLocationException {
		meter.start();
		for (int times= 0; times < 15000; times++)
			fLineTracker.replace(12, 500, FAUST500);
		meter.stop();
	}
	
	public void testRandomReplace() throws Exception {
		fLineTracker.set(FAUST1);
		
		PerformanceMeter meter= getNullPerformanceMeter();
		int runs= getWarmUpRuns();
		for (int run= 0; run < runs; run++)
			measureRandomReplace(meter);
		
		meter= createPerformanceMeter();
		runs= getMeasuredRuns();
		for (int run= 0; run < runs; run++)
			measureRandomReplace(meter);
		
		commitAllMeasurements();
		assertAllPerformance();
		
	}
	
	private void measureRandomReplace(PerformanceMeter meter) throws BadLocationException {
		Random rand= new Random(132498029834234L);
		int[] indices= new int[1000];
		for (int i= 0; i < indices.length; i++)
			indices[i]= rand.nextInt(FAUST1.length() - 100);
		char[] replacement= new char[100];
		Arrays.fill(replacement, ' ');
		String replace= new String(replacement);
		
		meter.start();
		for (int times= 0; times < 300; times++) {
			for (int i= 0; i < indices.length; i++) {
				fLineTracker.replace(indices[i], 100, null);
				fLineTracker.replace(indices[i], 0, replace);
			}
		}
		meter.stop();
	}
	
	public void testLineByOffset() throws Exception {
		fLineTracker.set(FAUST1);
		fLineTracker.replace(0, 0, ""); // trigger replacement with TreeLineTracker implementation
		
		PerformanceMeter meter= getNullPerformanceMeter();
		int runs= getWarmUpRuns();
		for (int run= 0; run < runs; run++)
			measureLineByOffset(meter);
		
		meter= createPerformanceMeter();
		runs= getMeasuredRuns();
		for (int run= 0; run < runs; run++)
			measureLineByOffset(meter);
		
		commitAllMeasurements();
		assertAllPerformance();
		
	}
	
	private void measureLineByOffset(PerformanceMeter meter) throws BadLocationException {
		int chars= FAUST1.length();
		meter.start();
		for (int times= 0; times < 30; times++)
			for (int offset= 0; offset <= chars; offset++)
				fLineTracker.getLineNumberOfOffset(offset);
		meter.stop();
	}

	public void testLineByOffset2() throws Exception {
		fLineTracker.set(FAUST1);
		
		PerformanceMeter meter= getNullPerformanceMeter();
		int runs= getWarmUpRuns();
		for (int run= 0; run < runs; run++)
			measureLineByOffset2(meter);
		
		meter= createPerformanceMeter();
		runs= getMeasuredRuns();
		for (int run= 0; run < runs; run++)
			measureLineByOffset2(meter);
		
		commitAllMeasurements();
		assertAllPerformance();
		
	}
	
	private void measureLineByOffset2(PerformanceMeter meter) throws BadLocationException {
		int chars= FAUST1.length();
		meter.start();
		for (int times= 0; times < 30; times++)
			for (int offset= 0; offset <= chars; offset++)
				fLineTracker.getLineNumberOfOffset(offset);
		meter.stop();
	}
	
	public void testLineByIndex() throws Exception {
		fLineTracker.set(FAUST1);
		fLineTracker.replace(0, 0, ""); // trigger replacement with TreeLineTracker implementation
		
		PerformanceMeter meter= getNullPerformanceMeter();
		int runs= getWarmUpRuns();
		for (int run= 0; run < runs; run++)
			measureLineByIndex(meter);
		
		meter= createPerformanceMeter();
		runs= getMeasuredRuns();
		for (int run= 0; run < runs; run++)
			measureLineByIndex(meter);
		
		commitAllMeasurements();
		assertAllPerformance();
		
	}
	
	private void measureLineByIndex(PerformanceMeter meter) throws BadLocationException {
		int lines= fLineTracker.getNumberOfLines();
		meter.start();
		for (int times= 0; times < 1000; times++)
			for (int line= 0; line < lines; line++)
				fLineTracker.getLineOffset(line);
		meter.stop();
	}

	public void testLineByIndex2() throws Exception {
		fLineTracker.set(FAUST1);
		
		PerformanceMeter meter= getNullPerformanceMeter();
		int runs= getWarmUpRuns();
		for (int run= 0; run < runs; run++)
			measureLineByIndex2(meter);
		
		meter= createPerformanceMeter();
		runs= getMeasuredRuns();
		for (int run= 0; run < runs; run++)
			measureLineByIndex2(meter);
		
		commitAllMeasurements();
		assertAllPerformance();
		
	}
	
	private void measureLineByIndex2(PerformanceMeter meter) throws BadLocationException {
		int lines= fLineTracker.getNumberOfLines();
		meter.start();
		for (int times= 0; times < 1000; times++)
			for (int line= 0; line < lines; line++)
				fLineTracker.getLineOffset(line);
		meter.stop();
	}
}
