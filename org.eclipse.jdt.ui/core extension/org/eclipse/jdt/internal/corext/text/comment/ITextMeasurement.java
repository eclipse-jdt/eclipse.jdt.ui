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
package org.eclipse.jdt.internal.corext.text.comment;

/**
 * The measurements can be used to compute how many times a first string has to
 * be concatenated to reach the same width as a second string. This is usually
 * with respect to a given font.
 * <p>
 * E.g., given a <code>firstString</code> and a <code>secondString</code>,
 * with the following code:
 * </p>
 * 
 * <pre>
 * int numberOfConcat= computeWidth(secondString) / computeWidth(firstString);
 * String thirdString= &quot;&quot;;
 * for (int i= 0; i &lt; numberOfConcat; i++)
 * 	thirdString += firstString;
 * </pre>
 * 
 * <p>
 * <code>computeWidth(thirdString) == computeWidth(secondString)</code> will
 * be <code>true</code> (disregarding rounding errors due to integer
 * arithmetic).
 * </p>
 * 
 * @since 3.0
 */
public interface ITextMeasurement {

	/**
	 * Width of the given string.
	 * 
	 * @param string the considered string
	 * @return the measured width
	 */
	public int computeWidth(String string);
}
