/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
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

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;

import org.eclipse.test.internal.performance.InternalPerformanceMeter;
import org.eclipse.test.internal.performance.OSPerformanceMeter;
import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dim;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.data.Scalar;


/**
 * An accumulating performance meter that tracks elapsed time between
 * consecutive calls to <code>on</code> and <code>off</code> using
 * <code>System.currentTimeMillis()</code>.
 *
 * @since 3.4 , extracted from {@link DocumentLineDifferModificationTest}
 */
final class DifferenceMeter extends InternalPerformanceMeter {

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
	@Override
	public void start() {
		throw new UnsupportedOperationException();
	}

	/*
	 * @see org.eclipse.test.performance.PerformanceMeter#stop()
	 */
	@Override
	public void stop() {
		throw new UnsupportedOperationException();
	}

	/*
	 * @see org.eclipse.test.performance.PerformanceMeter#dispose()
	 */
	@Override
	public void dispose() {
		fReferenceMeter.dispose();
		fMeasuredMeter.dispose();
		super.dispose();
	}

	@Override
	public Sample getSample() {
		Map<String, String> properties= new HashMap<>();

		Sample reference= fReferenceMeter.getSample();
		DataPoint[] referencePoints= reference.getDataPoints();

		Sample measured= fMeasuredMeter.getSample();
		DataPoint[] measuredPoints= measured.getDataPoints();

		Assert.assertEquals(referencePoints.length, measuredPoints.length);

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
		int step= minuend.getStep();
		Assert.assertEquals(step, subtrahend.getStep());

		Map<Dim, Scalar> scalars= new HashMap<>();
		for (Dim dimension : minuend.getDimensions2()) {
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
