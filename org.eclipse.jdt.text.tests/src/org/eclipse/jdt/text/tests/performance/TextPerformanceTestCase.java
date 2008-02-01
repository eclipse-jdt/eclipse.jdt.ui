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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.Platform;

import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;
import org.eclipse.jdt.text.tests.JdtTextTestPlugin;

/**
 * Superclass of Text performance test cases.
 * 
 * @since 3.1
 */
public class TextPerformanceTestCase extends TestCase {
	
	private static final boolean DEBUG= false;


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
	private static final int OVERRIDE_WARM_UP_RUNS= intValueOf(Platform.getDebugOption(PLUGIN_ID + OVERRIDE_WARM_UP_RUNS_OPTION), 2);
	
	/** overridden number of measured runs */
	private static final int OVERRIDE_MEASURED_RUNS= intValueOf(Platform.getDebugOption(PLUGIN_ID + OVERRIDE_MEASURED_RUNS_OPTION), 2);
	
	/** custom number of warm-up runs */
	private int fCustomWarmUpRuns= -1;
	
	/** custom number of measured runs */
	private int fCustomMeasuredRuns= -1;

	/** created performance meters */
	private List fPerformanceMeters;
	
	/** {@link KeyboardProbe} singleton */
	private static KeyboardProbe fgKeyboardProbe;

	/** base scenario id */
	private String fBaseScenarioId;

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
	
	
	/*
	 * @see junit.framework.TestCase#setUp()
	 * @since 3.1
	 */
	protected void setUp() throws Exception {
		super.setUp();
		
		EditorTestHelper.forceFocus();
		
		if (DEBUG)
			System.out.println(getClass().getName() + "." + getName() + ": " + System.currentTimeMillis());
	}
	
	/*
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		if (fPerformanceMeters != null)
			for (Iterator iter= fPerformanceMeters.iterator(); iter.hasNext();)
				((PerformanceMeter) iter.next()).dispose();

		if (DEBUG)
			System.out.println("    torn down: " + System.currentTimeMillis());
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

	/**
	 * @return the default scenario id for this test
	 */
	protected final String getDefaultScenarioId() {
		return Performance.getDefault().getDefaultScenarioId(this);
	}

	/**
	 * Returns the base scenario id for this test which has the default
	 * scenario id as its default.
	 * 
	 * @return the base scenario id
	 */
	protected final String getBaseScenarioId() {
		if (fBaseScenarioId == null)
			fBaseScenarioId= Performance.getDefault().getDefaultScenarioId(this);
		return fBaseScenarioId;
	}

	/**
	 * Sets the base scenario id for this test.
	 * 
	 * @param baseScenarioId the base scenario id
	 */
	protected final void setBaseScenarioId(String baseScenarioId) {
		fBaseScenarioId= baseScenarioId;
	}

	/**
	 * Create a performance meter with the base scenario id. The
	 * performance meter will be disposed on {@link #tearDown()}.
	 * 
	 * @return the created performance meter
	 */
	protected final PerformanceMeter createPerformanceMeter() {
		return createPerformanceMeter("");
	}

	/**
	 * Create a performance meter with the given sub-scenario id. The
	 * performance meter will be disposed on {@link #tearDown()}.
	 * 
	 * @param subScenarioId the sub-scenario id
	 * @return the created performance meter
	 */
	protected final PerformanceMeter createPerformanceMeter(String subScenarioId) {
		return internalCreatePerformanceMeter(getBaseScenarioId() + subScenarioId);
	}

	/**
	 * Create a performance meter with the base scenario id and mark the
	 * scenario to be included into the component performance summary. The
	 * summary shows the given dimension of the scenario and labels the
	 * scenario with the short name. The performance meter will be disposed
	 * on {@link #tearDown()}.
	 * 
	 * @param shortName a short (shorter than 40 characters) descriptive
	 *                name of the scenario
	 * @param dimension the dimension to show in the summary
	 * @return the created performance meter
	 */
	protected final PerformanceMeter createPerformanceMeterForSummary(String shortName, Dimension dimension) {
		return createPerformanceMeterForSummary("", shortName, dimension);
	}

	/**
	 * Create a performance meter with the given sub-scenario id and mark
	 * the scenario to be included into the component performance summary.
	 * The summary shows the given dimension of the scenario and labels the
	 * scenario with the short name. The performance meter will be disposed
	 * on {@link #tearDown()}.
	 * 
	 * @param subScenarioId the sub-scenario id
	 * @param shortName a short (shorter than 40 characters) descriptive
	 *                name of the scenario
	 * @param dimension the dimension to show in the summary
	 * @return the created performance meter
	 */
	protected final PerformanceMeter createPerformanceMeterForSummary(String subScenarioId, String shortName, Dimension dimension) {
		PerformanceMeter performanceMeter= createPerformanceMeter(subScenarioId);
		Performance.getDefault().tagAsSummary(performanceMeter, shortName, dimension);
		return performanceMeter;
	}

	/**
	 * Create a performance meter with the base scenario id and mark the
	 * scenario to be included into the global performance summary. The
	 * summary shows the given dimension of the scenario and labels the
	 * scenario with the short name. The performance meter will be disposed
	 * on {@link #tearDown()}.
	 * 
	 * @param shortName a short (shorter than 40 characters) descriptive
	 *                name of the scenario
	 * @param dimension the dimension to show in the summary
	 * @return the created performance meter
	 */
	protected final PerformanceMeter createPerformanceMeterForGlobalSummary(String shortName, Dimension dimension) {
		return createPerformanceMeterForGlobalSummary("", shortName, dimension);
	}

	/**
	 * Create a performance meter with the given sub-scenario id and mark
	 * the scenario to be included into the global performance summary. The
	 * summary shows the given dimension of the scenario and labels the
	 * scenario with the short name. The performance meter will be disposed
	 * on {@link #tearDown()}.
	 * 
	 * @param subScenarioId the sub-scenario id
	 * @param shortName a short (shorter than 40 characters) descriptive
	 *                name of the scenario
	 * @param dimension the dimension to show in the summary
	 * @return the created performance meter
	 */
	protected final PerformanceMeter createPerformanceMeterForGlobalSummary(String subScenarioId, String shortName, Dimension dimension) {
		PerformanceMeter performanceMeter= createPerformanceMeter(subScenarioId);
		Performance.getDefault().tagAsGlobalSummary(performanceMeter, shortName, dimension);
		return performanceMeter;
	}

	/**
	 * Create an invocation counting performance meter that will count the
	 * number of invocations of the given methods. The performance meter
	 * will be disposed on {@link #tearDown()}.
	 * 
	 * @param methods the methods whose invocations will be counted
	 * @return the created performance meter
	 */
	protected final InvocationCountPerformanceMeter createInvocationCountPerformanceMeter(Method[] methods) {
		return createInvocationCountPerformanceMeter("", methods);
	}

	/**
	 * Create an invocation counting performance meter with the given
	 * sub-scenario id. The performance meter will count the number of
	 * invocations of the given methods. The performance meter will be
	 * disposed on {@link #tearDown()}.
	 * 
	 * @param subScenarioId the sub-scenario id
	 * @param methods the methods whose invocations will be counted
	 * @return the created performance meter
	 */
	protected final InvocationCountPerformanceMeter createInvocationCountPerformanceMeter(String subScenarioId, Method[] methods) {
		InvocationCountPerformanceMeter performanceMeter= new InvocationCountPerformanceMeter(getBaseScenarioId() + subScenarioId, methods);
		addPerformanceMeter(performanceMeter);
		return performanceMeter;
	}

	/**
	 * Create an invocation counting performance meter that will count the
	 * number of invocations of the given constructors. The performance meter
	 * will be disposed on {@link #tearDown()}.
	 * 
	 * @param constructors the constructors whose invocations will be counted
	 * @return the created performance meter
	 */
	protected final InvocationCountPerformanceMeter createInvocationCountPerformanceMeter(Constructor[] constructors) {
		return createInvocationCountPerformanceMeter("", constructors);
	}

	/**
	 * Create an invocation counting performance meter with the given
	 * sub-scenario id. The performance meter will count the number of
	 * invocations of the given constructors. The performance meter will be
	 * disposed on {@link #tearDown()}.
	 * 
	 * @param subScenarioId the sub-scenario id
	 * @param constructors the constructors whose invocations will be counted
	 * @return the created performance meter
	 */
	protected final InvocationCountPerformanceMeter createInvocationCountPerformanceMeter(String subScenarioId, Constructor[] constructors) {
		InvocationCountPerformanceMeter performanceMeter= new InvocationCountPerformanceMeter(getBaseScenarioId() + subScenarioId, constructors);
		addPerformanceMeter(performanceMeter);
		return performanceMeter;
	}

	/**
	 * Commits the measurements captured by all performance meters created
	 * through one of this class' factory methods.
	 */
	protected final void commitAllMeasurements() {
		if (fPerformanceMeters != null)
			for (Iterator iter= fPerformanceMeters.iterator(); iter.hasNext();)
				((PerformanceMeter) iter.next()).commit();
	}
	
	/**
	 * Asserts default properties of the measurements captured by the given
	 * performance meter.
	 * 
	 * @param performanceMeter the performance meter
	 * @throws RuntimeException if the properties do not hold
	 */
	protected final void assertPerformance(PerformanceMeter performanceMeter) {
		Performance.getDefault().assertPerformance(performanceMeter);
	}

	/**
	 * Asserts default properties of the measurements captured by all
	 * performance meters created through one of this class' factory
	 * methods.
	 * 
	 * @throws RuntimeException if the properties do not hold
	 */
	protected final void assertAllPerformance() {
		if (fPerformanceMeters != null)
			for (Iterator iter= fPerformanceMeters.iterator(); iter.hasNext();)
				assertPerformance((PerformanceMeter) iter.next());
	}

	/**
	 * Returns the null performance meter singleton.
	 * 
	 * @return the null performance meter singleton
	 */
	protected static final PerformanceMeter getNullPerformanceMeter() {
		return Performance.getDefault().getNullPerformanceMeter();
	}
	
	/**
	 * Returns the keyboard probe singleton.
	 * 
	 * @return the keyboard probe singleton.
	 */
	protected static final KeyboardProbe getKeyboardProbe() {
		if (fgKeyboardProbe == null) {
			fgKeyboardProbe= new KeyboardProbe();
			fgKeyboardProbe.initialize();
		}
		return fgKeyboardProbe;
	}
	
	/*
	 * @see PerformanceTestCase#setComment(int, String)
	 * @since 3.1
	 */
	protected final void explainDegradation(String explanation, PerformanceMeter performanceMeter) {
		Performance performance= Performance.getDefault();
		performance.setComment(performanceMeter, Performance.EXPLAINS_DEGRADATION_COMMENT, explanation);
	}

	/**
	 * Create a performance meter with the given scenario id. The
	 * performance meter will be disposed on {@link #tearDown()}.
	 * 
	 * @param scenarioId the scenario id
	 * @return the created performance meter
	 */
	private PerformanceMeter internalCreatePerformanceMeter(String scenarioId) {
		PerformanceMeter performanceMeter= Performance.getDefault().createPerformanceMeter(scenarioId);
		addPerformanceMeter(performanceMeter);
		return performanceMeter;
	}

	/**
	 * Add the given performance meter to the managed performance meters.
	 * 
	 * @param performanceMeter the performance meter
	 */
	private void addPerformanceMeter(PerformanceMeter performanceMeter) {
		if (fPerformanceMeters == null)
			fPerformanceMeters= new ArrayList();
		fPerformanceMeters.add(performanceMeter);
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
}
