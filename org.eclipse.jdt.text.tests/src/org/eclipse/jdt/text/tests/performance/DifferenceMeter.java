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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.Assert;

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
	public void start() {
		throw new UnsupportedOperationException();
	}

	/*
	 * @see org.eclipse.test.performance.PerformanceMeter#stop()
	 */
	public void stop() {
		throw new UnsupportedOperationException();
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
		Collection mDims= minuend.getDimensions2();
		int step= minuend.getStep();
		Assert.assertEquals(step, subtrahend.getStep());

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
