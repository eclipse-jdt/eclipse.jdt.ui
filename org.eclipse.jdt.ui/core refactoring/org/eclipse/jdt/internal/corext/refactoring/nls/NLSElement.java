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
package org.eclipse.jdt.internal.corext.refactoring.nls;

import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;

public class NLSElement {

	public static final String TAG_PREFIX= "//$NON-NLS-"; //$NON-NLS-1$
	public static final int TAG_PREFIX_LENGTH= TAG_PREFIX.length();
	public static final String TAG_POSTFIX= "$"; //$NON-NLS-1$
	public static final int TAG_POSTFIX_LENGTH= TAG_POSTFIX.length();

	private static class NLSTextRegion extends TextRegion {
		int fStart;
		int fLength;
		public NLSTextRegion(int start, int length) {
			fStart= start;
			Assert.isTrue(fStart >= 0);
			fLength= length;
			Assert.isTrue(fLength >= 0);
		}
		public int getOffset() {
			return fStart;
		}
		public int getLength() {
			return fLength;
		}
		public String toString() {
			return "(" + fStart + "," + fLength + ")"; //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
		}
	}
	
	/** The original string denoted by the position */
	private String fValue;
	/** The position of the original string */
	private TextRegion fPosition;
	
	/** Position of the // $NON_NLS_*$ tag */
	private TextRegion fTagPosition;
    
	/** Index of the Element in an NLSLine */
	private int fIndex;
	
	/**
	 * Creates a new NLS element for the given string and position.
	 */
	public NLSElement(String value, int start, int length, int index) {
		fValue= value;
		fIndex= index;
		Assert.isNotNull(fValue);
		fPosition= new NLSTextRegion(start, length);
	}
	
	/**
	 * Returns the position of the string to be NLSed.
	 * @return Returns the position of the string to be NLSed
	 */
	public TextRegion getPosition() {
		return fPosition;
	}

	/**
	 * Returns the actual string value.
	 * @return the actual string value
	 */
	public String getValue() {
		return fValue;
	}
	
	/**
	 * Sets the actual string value.
	 */
	public void setValue(String value) {
		fValue= value;
	}
	
	/**
	 * Sets the tag position if one is associated with the NLS element.
	 */	
	public void setTagPosition(int start, int length) {
		fTagPosition= new NLSTextRegion(start, length);
	}
	
	/**
	 * Returns the tag position for this element. The method can return <code>null</code>.
	 * In this case no tag has been found for this NLS element.
	 */
	public TextRegion getTagPosition() {
		return fTagPosition;
	}
	
	/**
	 * Returns <code>true</code> if the NLS element has an assicated $NON-NLS-*$ tag. 
	 * Otherwise <code>false</code> is returned.
	 */
	public boolean hasTag() {
		return fTagPosition != null && fTagPosition.getLength() > 0;
	}
	
	public static String createTagText(int index){
		return TAG_PREFIX + index + TAG_POSTFIX;
	}
	
	public String getTagText() {
		return TAG_PREFIX + (fIndex + 1) + TAG_POSTFIX;
	}

	/* (Non-Javadoc)
	 * Method declared in Object.
	 * only for debugging
	 */
	public String toString() {
		return fPosition + ": " + fValue + "    Tag position: " +  //$NON-NLS-2$ //$NON-NLS-1$
			(hasTag() ? fTagPosition.toString() : "no tag found"); //$NON-NLS-1$
	}
}

