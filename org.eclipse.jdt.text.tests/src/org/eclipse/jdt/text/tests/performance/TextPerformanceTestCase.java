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

import junit.framework.TestCase;

/**
 * Superclass of Text performance test cases.
 * 
 * @since 3.1
 */
public class TextPerformanceTestCase extends TestCase {

	/** <code>true</code> iff the default number of runs should be used */
	private static final boolean USE_DEFAULT_RUNS= true;
	
	/** default number of warm-up runs */
	private static final int DEFAULT_WARM_UP_RUNS= 0;
	
	/** default number of measured runs */
	private static final int DEFAULT_MEASURED_RUNS= 1;
	
	/** custom number of warm-up runs */
	private int fCustomWarmUpRuns= -1;
	
	/** custom number of measured runs */
	private int fCustomMeasuredRuns= -1;
	
	/*
	 * @see TestCase#TestCase()
	 */
	public TextPerformanceTestCase() {
		super();
	}

	/*
	 * @see TestCase#TestCase(String)
	 */
	public TextPerformanceTestCase(String name) {
		super(name);
	}
	
	/**
	 * @return number of warm-up runs, must have been set before
	 */
	protected final int getWarmUpRuns() {
		assertTrue(fCustomWarmUpRuns >= 0);
		if (USE_DEFAULT_RUNS)
			return DEFAULT_WARM_UP_RUNS;
		return fCustomWarmUpRuns;
	}
	
	/**
	 * Sets the number of warm-up runs. Can be overruled.
	 * 
	 * @param runs number of warm-up runs
	 */
	protected final void setWarmUpRuns(int runs) {
		fCustomWarmUpRuns= runs;
	}
	
	/**
	 * @return number of measured runs, must have been set before
	 */
	protected final int getMeasuredRuns() {
		assertTrue(fCustomMeasuredRuns >= 0);
		if (USE_DEFAULT_RUNS)
			return DEFAULT_MEASURED_RUNS;
		return fCustomMeasuredRuns;
	}
	
	/**
	 * Sets the number of measured runs. Can be overruled.
	 * 
	 * @param runs number of measured runs
	 */
	protected final void setMeasuredRuns(int runs) {
		fCustomMeasuredRuns= runs;
	}
}
