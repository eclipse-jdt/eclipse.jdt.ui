/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.template;

/**
 * 
 */
public class TemplatePosition {

	/** The name of the template position */
	private final String fName;
	/** The default value of the template position */
	private final String fDefaultValue;

	/** The length of the template positions. */
	private int fLength;
	/** The offsets of the template positions. */
	private int[] fOffsets;
	/** A flag indicating if the template position has been resolved. */
	private boolean fResolved;
	
	/**
	 * Creates a template position.
	 * 
	 * @param name the name of the template position.
	 * @param defaultValue the default value of the position.
	 * @param offsets the array of offsets of the position.
	 * @param the length of the position.
	 */
	public TemplatePosition(String name, String defaultValue, int[] offsets, int length) {
		fName= name;
		fDefaultValue= defaultValue;
		fOffsets= offsets;
		fLength= length;
		fResolved= false;
	}

	/**
	 * Returns the name of the position.
	 */
	public String getName() {
	    return fName;
	}	

	/**
	 * Returns the default value of the position.
	 */
	public String getDefaultValue() {
	 	return fDefaultValue;
	}
	
	/**
	 * Sets the length of the position.
	 */
	public void setLength(int length) {
	    fLength= length;
	}
	
	/**
	 * Returns the length of the position.
	 */
	public int getLength() {
	 	return fLength;   
	}
	
	/**
	 * Sets the offsets of the position.
	 */
	public void setOffsets(int[] offsets) {
	 	fOffsets= offsets; 
	}
	
	/**
	 * Returns the offsets of the position.
	 */
	public int[] getOffsets() {
	 	return fOffsets;   
	}
	
	/**
	 * Sets the resolved flag of the position.
	 */
	public void setResolved(boolean resolved) {
	    fResolved= resolved;
	}	

	/**
	 * Returns <code>true</code> if the position is resolved, <code>false</code> otherwise.
	 */	
	public boolean isResolved() {
	 	return fResolved;   
	}

}
