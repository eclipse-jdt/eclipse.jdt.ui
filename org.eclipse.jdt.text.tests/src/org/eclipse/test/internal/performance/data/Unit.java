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

import java.text.NumberFormat;


/**
 * @since 3.1
 */
public class Unit {
	private static final int T_DECIMAL= 1000;
	private static final int T_BINARY= 1024;
	
	protected static final String[] PREFIXES= new String[] { "y", "z", "a", "f", "p", "n", "u", "m", "", "k", "M", "G", "T", "P", "E", "Z", "Y" };
	protected static final String[] FULL_PREFIXES= new String[] { "yocto", "zepto", "atto", "femto", "pico", "nano", "micro", "milli", "", "kilo", "mega", "giga", "tera", "peta", "exa", "zetta", "yotta" };
	protected static final String[] BINARY_PREFIXES= new String[] { "", "", "", "", "", "", "", "", "", "ki", "Mi", "Gi", "Ti", "Pi", "Ei", "Zi", "Yi" };
	protected static final String[] BINARY_FULL_PREFIXES= new String[] { "", "", "", "", "", "", "", "", "", "kibi", "mebi", "gibi", "tebi", "pebi", "exbi", "zebi", "yobi" };
	
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
		boolean negative= magnitude < 0;
		double mag= Math.abs(magnitude), ratio= mag / div;
		int divs= PREFIXES.length / 2;
		while (ratio >= 1) {
			mag= ratio;
			divs++;
			ratio= mag / div;
		}
		ratio= mag * div;
		while (ratio > 0.0 && ratio < div) {
			mag= ratio;
			divs--;
			ratio= mag * div;
		}
		
		if (negative)
			mag= -mag;
		
		String[] prefixes= fIsBinary ? BINARY_PREFIXES : PREFIXES;
		NumberFormat format= NumberFormat.getInstance();
		format.setMaximumFractionDigits(fPrecision);
		if (divs > 0 && divs <= prefixes.length)
			return new DisplayValue(prefixes[divs] + getShortName(), format.format(mag));
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
	public static final Unit BYTE= new Unit("byte", "byte", true);  //$NON-NLS-1$
	public static final Unit CARDINAL= new Unit("", "", false);  //$NON-NLS-1$
}
