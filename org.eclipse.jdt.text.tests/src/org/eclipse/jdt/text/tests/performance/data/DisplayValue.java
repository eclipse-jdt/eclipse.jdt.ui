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


/**
 * @since 3.1
 */
public class DisplayValue {
	private final String fScaledUnit;
	private final String fScaledScalar;
	
	
	public DisplayValue(String unit, String value) {
		fScaledUnit= unit;
		fScaledScalar= value;
	}
	
	public String getScaledUnit() {
		return fScaledUnit;
	}
	
	public String getScaledValue() {
		return fScaledScalar;
	}
	
	public String getDisplayString() {
		return getScaledValue() + getScaledUnit();
	}
	
	
	public String toString() {
		return getDisplayString();
	}
}
