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

import org.eclipse.core.runtime.Platform;

import org.eclipse.jdt.text.tests.JdtTextTestPlugin;

import junit.framework.TestCase;

/**
 * Superclass of Text performance test cases.
 * 
 * @since 3.1
 */
public class TextPerformanceTestCase extends TestCase {

	/** containing plug-in id */
	private static final String PLUGIN_ID= JdtTextTestPlugin.PLUGIN_ID;
	
	/** boolean option, a value of <code>"true"</code> enables overriding of the number of runs */
	private static final String OVERRIDE_RUNS_OPTION= "/debug/performance/OverrideRuns";

	/** integer option, its value overrides the number of warm-up runs */
	private static final String OVERRIDE_WARM_UP_RUNS_OPTION= "/debug/performance/OverrideWarmUpRuns";

	/** integer option, its value overrides the number of measured runs */
	private static final String OVERRIDE_MEASURED_RUNS_OPTION= "/debug/performance/OverrideMeasuredRuns";

	/** <code>true</code> iff the number of runs should be overridden */
	private static final boolean OVERRIDE_RUNS= Boolean.toString(true).equals(Platform.getDebugOption(PLUGIN_ID + OVERRIDE_RUNS_OPTION));
	
	/** overridden number of warm-up runs */
	private static final int OVERRIDE_WARM_UP_RUNS= intValueOf(Platform.getDebugOption(PLUGIN_ID + OVERRIDE_WARM_UP_RUNS_OPTION), 1);
	
	/** overridden number of measured runs */
	private static final int OVERRIDE_MEASURED_RUNS= intValueOf(Platform.getDebugOption(PLUGIN_ID + OVERRIDE_MEASURED_RUNS_OPTION), 2);
	
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
	 * Returns the integer value of the given string unless the string
	 * cannot be interpreted as such, in this case the given default is
	 * returned.
	 * 
	 * @param stringValue the string to be interpreted as integer
	 * @param defaultValue the default integer value
	 * @return the integer value
	 */
	private static int intValueOf(String stringValue, int defaultValue) {
		try {
			if (stringValue != null)
				return Integer.valueOf(stringValue).intValue();
		} catch (NumberFormatException e) {
			// use default
		}
		return defaultValue;
	}

	/**
	 * @return number of warm-up runs, must have been set before
	 */
	protected final int getWarmUpRuns() {
		assertTrue(fCustomWarmUpRuns >= 0);
		if (OVERRIDE_RUNS)
			return OVERRIDE_WARM_UP_RUNS;
		return fCustomWarmUpRuns;
	}
	
	/**
	 * Sets the number of warm-up runs. Can be overridden.
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
		if (OVERRIDE_RUNS)
			return OVERRIDE_MEASURED_RUNS;
		return fCustomMeasuredRuns;
	}
	
	/**
	 * Sets the number of measured runs. Can be overridden.
	 * 
	 * @param runs number of measured runs
	 */
	protected final void setMeasuredRuns(int runs) {
		fCustomMeasuredRuns= runs;
	}
}
