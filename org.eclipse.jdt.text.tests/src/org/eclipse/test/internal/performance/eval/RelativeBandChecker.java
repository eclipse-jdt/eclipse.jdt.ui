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

import org.eclipse.test.internal.performance.data.Dimension;


/**
 * @since 3.1
 */
public class RelativeBandChecker extends AssertChecker {

	private final float fLowerBand;
	private final float fUpperBand;

	public RelativeBandChecker(Dimension dimension, float lowerBand, float upperBand) {
		super(dimension);
		fLowerBand= lowerBand;
		fUpperBand= upperBand;
	}

	public boolean test(StatisticsSession reference, StatisticsSession measured, StringBuffer message) {
		Dimension dimension= getDimension();
		double actual= measured.getAverage(dimension);
		double test= reference.getAverage(dimension);
		
		if (actual > fUpperBand * test || actual < fLowerBand * test) {
			message.append("\n " + dimension.getName() + ": " + dimension.getDisplayValue(actual) + " is not within [" + Math.round(fLowerBand * 100)+ "%, " + Math.round(fUpperBand * 100) + "%] of " + dimension.getDisplayValue(test));
			return false;
		}
		
		return true;
	}	
}
