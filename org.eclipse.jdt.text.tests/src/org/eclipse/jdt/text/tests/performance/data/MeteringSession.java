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

import java.util.List;
import java.util.Map;


/**
 * @since 3.1
 */
public class MeteringSession {
	Map fProperties;
	List fDataPoints;
	String fId;
	MeteringSession(Map properties, List dataPoints) {
		fProperties= properties;
		fDataPoints= dataPoints;
	}
	public String getProperty(String name) {
		return (String) fProperties.get(name);
	}
	public DataPoint[] getDataPoints() {
		return (DataPoint[]) fDataPoints.toArray(new DataPoint[fDataPoints.size()]);
	}
	public String getId() {
		return fId;
	}
	public String toString() {
		return "MeteringSession [id= " + fId + ", #datapoints: " + fDataPoints.size() + "]";
	}
}
