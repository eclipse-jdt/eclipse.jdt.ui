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

import java.text.NumberFormat;


/**
 * @since 3.1
 */
public class Unit {
	private static final int T_DECIMAL= 1000;
	private static final int T_BINARY= 1024;
	
	protected static final String[] PREFIXES= new String[] { "a", "f", "p", "n", "u", "m", "\0", "k", "M", "G", "T", "P", "E" };
	protected static final String[] FULL_PREFIXES= new String[] { "atto", "femto", "pico", "nano", "micro", "milli", "\0", "kilo", "mega", "giga", "tera", "peta", "exa" };
	
	private final String fShortName;
	private final String fFullName;
	private final boolean fIsBinary;
	private final int fPrecision= 2;
	
	public String getShortName() {
		return fShortName;
	}
	
	public String getFullName() {
		return fFullName;
	}
	
	public DisplayValue getDisplayValue(double magnitude) {
		int div= fIsBinary ? T_BINARY : T_DECIMAL;
		double mag= magnitude, ratio= mag / div;
		int divs= 6;
		while (ratio >= 1) {
			mag= ratio;
			divs++;
			ratio= mag / div;
		}
		ratio= mag * div;
		while (ratio != 0.0 && ratio < div) {
			mag= ratio;
			divs--;
			ratio= mag * div;
		}
		
		NumberFormat format= NumberFormat.getInstance();
		format.setMaximumFractionDigits(fPrecision);
		if (divs > 0 && divs <= PREFIXES.length)
			return new DisplayValue(PREFIXES[divs] + getShortName(), format.format(mag));
		else
			return new DisplayValue(getShortName(), "" + magnitude);
	}
	
	public DisplayValue getDisplayValue(Scalar scalar) {
		return getDisplayValue(scalar.getMagnitude());
	}
	
	public Unit(String shortName, String fullName, boolean binary) {
		fShortName= shortName;
		fFullName= fullName;
		fIsBinary= binary;
	}
	
	public String toString() {
		return "Unit [" + getShortName() + "]";
	}
	
	public static final Unit SECOND= new Unit("s", "second", false);  //$NON-NLS-1$
	public static final Unit BYTE= new Unit("Byte", "byte", true);  //$NON-NLS-1$
	public static final Unit CARDINAL= new Unit("", "", false);  //$NON-NLS-1$
}
