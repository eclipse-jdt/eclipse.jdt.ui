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
package org.eclipse.test.internal.performance.data;


/**
 * @since 3.1
 */
public class Scalar {
	private String fDimension;
	private long fMagnitude;
	
	public Scalar(String dimension, long extent) {
		fDimension= dimension;
		fMagnitude= extent;
	}
	
	public String getDimension() {
		return fDimension;
	}
	
	public long getMagnitude() {
		return fMagnitude;
	}
	
	public String toString() {
		// TODO get rid of perfmsr dependency
		Dimension dim= PerfMsrDimensions.getDimension(fDimension);
		if (dim == null)
			return "Scalar [dimension= " + fDimension + ", magnitude= " + fMagnitude + "]";
		else
			return "Scalar [" + dim.getName() + ": " + dim.getDisplayValue(this) + "]";
	}
}
