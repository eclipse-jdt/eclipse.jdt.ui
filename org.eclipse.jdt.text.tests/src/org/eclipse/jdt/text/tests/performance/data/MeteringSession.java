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
package org.eclipse.jdt.text.tests.performance.data;

import java.util.Map;


/**
 * @since 3.1
 */
public class MeteringSession {
	Map fProperties;
	DataPoint[] fDataPoints;
	String fId;
	public MeteringSession(Map properties, DataPoint[] dataPoints) {
		fProperties= properties;
		fDataPoints= dataPoints;
	}
	public String getProperty(String name) {
		return (String) fProperties.get(name);
	}
	public DataPoint[] getDataPoints() {
		DataPoint[] dataPoints= new DataPoint[fDataPoints.length];
		System.arraycopy(fDataPoints, 0, dataPoints, 0, fDataPoints.length);
		return dataPoints;
	}
	public String getId() {
		return fId;
	}
	public String toString() {
		return "MeteringSession [id= " + fId + ", #datapoints: " + fDataPoints.length + "]";
	}
}
