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
package org.eclipse.test.internal.performance.eval;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.test.internal.performance.PerfMsrConstants;
import org.eclipse.test.internal.performance.data.DataPoint;
import org.eclipse.test.internal.performance.data.Dimension;
import org.eclipse.test.internal.performance.data.Sample;
import org.eclipse.test.internal.performance.data.Scalar;

import junit.framework.Assert;



/**
 * @since 3.1
 */
public class StatisticsSession {

	static final class Statistics {
		public long count;
		public long sum;
		public double average;
		public double stddev;
	}
	
	private final Sample fSession;
	private final Map fStatistics= new HashMap();

	public StatisticsSession(Sample session) {
		fSession= session;
	}
	
	public Sample getSession() {
		return fSession;
	}

	public double getAverage(Dimension dimension) {
		return getStats(dimension).average;
	}
	
	public long getSum(Dimension dimension) {
		return getStats(dimension).sum;
	}
	
	public long getCount(Dimension dimension) {
		return getStats(dimension).count;
	}
	
	public double getStddev(Dimension dimension) {
		return getStats(dimension).stddev;
	}
	
	private Statistics getStats(Dimension dimension) {
		Statistics stats= (Statistics) fStatistics.get(dimension);
		if (stats == null) {
			stats= computeStats(dimension);
			fStatistics.put(dimension, stats);
		}
		return stats;
	}

	private Statistics computeStats(Dimension dimension) {
		
		DataPoint[] datapoints= fSession.getDataPoints();
		Statistics stats= new Statistics();
		
		for (int i= 0; i < datapoints.length - 1; i += 2) {
			DataPoint before= datapoints[i];
			Assert.assertTrue("order of datapoints makes no sense", before.getKind().equals(PerfMsrConstants.BEFORE)); //$NON-NLS-1$
			DataPoint after= datapoints[i + 1];
			Assert.assertTrue("order of datapoints makes no sense", after.getKind().equals(PerfMsrConstants.AFTER)); //$NON-NLS-1$
			
			Scalar delta= getDelta(before, after, dimension);
			long magnitude= delta.getMagnitude();
			stats.sum += magnitude;
			stats.count++;
			stats.average= stats.sum / stats.count;
			stats.stddev+= (stats.average - magnitude) * (stats.average - magnitude);
		}
		
		stats.stddev= Math.sqrt(stats.stddev / stats.count - 1);

		return stats;
	}

	private Scalar getDelta(DataPoint before, DataPoint after, Dimension dimension) {
		Scalar one= before.getScalar(dimension);
		Assert.assertTrue("reference has no value for dimension " + dimension, one != null); //$NON-NLS-1$

		Scalar two= after.getScalar(dimension);
		Assert.assertTrue("reference has no value for dimension " + dimension, two != null); //$NON-NLS-1$
		
		Scalar delta= new Scalar(one.getDimension(), two.getMagnitude() - one.getMagnitude());
		return delta;
	}
}
