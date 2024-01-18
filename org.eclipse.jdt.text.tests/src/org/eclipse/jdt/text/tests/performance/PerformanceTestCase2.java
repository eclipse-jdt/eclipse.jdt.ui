/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * @since 3.3
 */
public class PerformanceTestCase2 extends TestCase {
	private String fBaseScenarioId;
	private List<PerformanceMeter> fPerformanceMeters;

	public PerformanceTestCase2() {
		super();
	}

	public PerformanceTestCase2(String name) {
		super(name);
	}

	protected int getWarmUpRuns() {
		return 20;
	}

	protected int getMeasuredRuns() {
		return 20;
	}

	@Override
	protected void runTest() throws Throwable {
		assertNotNull(getName());
		Method runMethod= null;
		try {
			// use getMethod to get all public inherited
			// methods. getDeclaredMethods returns all
			// methods of this class but excludes the
			// inherited ones.
			runMethod= getClass().getMethod(getName(), PerformanceMeter.class);
		} catch (NoSuchMethodException e) {
			throw new AssertionFailedError("Method \""+getName()+"\" not found");  //$NON-NLS-1$//$NON-NLS-2$
		}
		assertTrue("Method \"" + getName() + "\" should be public", Modifier.isPublic(runMethod.getModifiers())); //$NON-NLS-1$ //$NON-NLS-2$

		try {
			int warmUpRuns= getWarmUpRuns();
			PerformanceMeter nullMeter= Performance.getDefault().getNullPerformanceMeter();
			Object[] args= { nullMeter };
			for (int i= 0; i < warmUpRuns; i++)
				runMethod.invoke(this, args);

			int measuredRuns= getMeasuredRuns();
			PerformanceMeter meter= createPerformanceMeter();
			args[0]= meter;
			for (int i= 0; i < measuredRuns; i++)
				runMethod.invoke(this, args);

			commitAllMeasurements();
			assertAllPerformance();
		}
		catch (InvocationTargetException e) {
			e.fillInStackTrace();
			throw e.getTargetException();
		}
		catch (IllegalAccessException e) {
			e.fillInStackTrace();
			throw e;
		}
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
			fPerformanceMeters= new ArrayList<>();
		fPerformanceMeters.add(performanceMeter);
	}

	/**
	 * Commits the measurements captured by all performance meters created
	 * through one of this class' factory methods.
	 */
	protected final void commitAllMeasurements() {
		if (fPerformanceMeters != null)
			for (PerformanceMeter performanceMeter : fPerformanceMeters)
				performanceMeter.commit();
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
			for (PerformanceMeter performanceMeter : fPerformanceMeters)
				assertPerformance(performanceMeter);
	}

	/**
	 * Create a performance meter with the base scenario id. The
	 * performance meter will be disposed on {@link #tearDown()}.
	 *
	 * @return the created performance meter
	 */
	protected final PerformanceMeter createPerformanceMeter() {
		PerformanceMeter perfMeter= internalCreatePerformanceMeter(getBaseScenarioId());
		String localFingerprintName= getLocalFingerprints().get(getName());
		if (localFingerprintName != null)
			Performance.getDefault().tagAsSummary(perfMeter, localFingerprintName, Dimension.ELAPSED_PROCESS);

		String comment= getDegradationComments().get(getName());
		if (comment != null)
			Performance.getDefault().setComment(perfMeter, Performance.EXPLAINS_DEGRADATION_COMMENT, comment);

		return perfMeter;
	}

	/**
	 * Returns a map with local fingerprints.
	 *
	 * @return the map with local fingerprints ( test name -> short name)
	 */
	protected Map<String, String> getLocalFingerprints() {
		return Collections.EMPTY_MAP;
	}

	/**
	 * Returns a map with degradation comments.
	 *
	 * @return the map with degradation comments ( test name -> comment)
	 */
	protected Map<String, String> getDegradationComments() {
		return Collections.EMPTY_MAP;
	}

}
