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

import java.util.Map;


/**
 * @since 3.1
 */
public class DataPoint {
	private String fKind; // TODO this is perfmsr dependent
	private Map fScalars;
	
	public DataPoint(String kind, Map values) {
		fKind= kind;
		fScalars= values;
	}
	
	public String getKind() {
		return fKind;
	}
	
	public Scalar[] getScalars() {
		return (Scalar[]) fScalars.values().toArray(new Scalar[fScalars.size()]);
	}
	
	public Scalar getScalar(Dimension dimension) {
		// TODO get rid of perfmsr dependency
		return (Scalar) fScalars.get(PerfMsrDimensions.getPerfMsrId(dimension));
	}
	
	public String toString() {
		return "DataPoint [kind= " + fKind + ", #dimensions: " + fScalars.size() + "]";
	}
}
