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
package org.eclipse.jdt.text.tests.performance.eval;

import org.eclipse.jdt.text.tests.performance.data.Dimension;
import org.eclipse.jdt.text.tests.performance.data.Scalar;


/**
 * @since 3.1
 */
public class AbsoluteBandChecker extends AssertChecker {

	private long fLowerBand;
	private long fUpperBand;

	public AbsoluteBandChecker(Dimension dimension, long lowerBand, long upperBand) {
		super(dimension);
		fLowerBand= lowerBand;
		fUpperBand= upperBand;
	}
	
	public boolean test(StatisticsSession reference, StatisticsSession measured, StringBuffer message) {
		Dimension dimension= getDimension();
		double actual= measured.getAverage(dimension);
		double test= reference.getAverage(dimension);
		
		if (actual > fUpperBand + test || actual < test - fLowerBand) {
			message.append("\n " + dimension.getName() + ": " + dimension.getDisplayValue(actual) + " is not within [-" + dimension.getDisplayValue(new Scalar(null, fLowerBand)) + ", +" + dimension.getDisplayValue(new Scalar(null, fUpperBand)) + "] of " + dimension.getDisplayValue(test));
			return false;
		}
		
		return true;
	}	
}
