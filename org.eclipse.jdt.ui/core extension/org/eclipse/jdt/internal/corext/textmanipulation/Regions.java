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
package org.eclipse.jdt.internal.corext.textmanipulation;


public class Regions {
	
	// no instance
	private Regions() {
	}

	/**
	 * Tests if <code>thisRegion</code> covers <code>otherRange</code>. If <code>
	 * thisRegion<code> has length 0, <code>false<code> is returned (a insertion
	 * point can't cover anything else). If <code>thisRegion</code> has a length
	 * greater than zero the following expression is returned:
	 * <pre>
	 *   thisRegion.getOffset() <= otherRegion.getOffset &&
	 *   otherRegion.getOffset + otherRegion.getLength() <= thisRegion.getOffset() + thisRegion.getLength()
	 * </pre>
	 * 
	 * @param thisRegion the region that may cover another region
	 * @param otherRegion the region to be covered
	 * 
	 * @return <code>true</code> if <code>thisRegion</code> covers <code>
	 *  otherRegion</code>; otherwise <code>false</code> is returned.
	 */
	public static boolean covers(TextRange thisRegion, TextRange otherRegion) {
		if (thisRegion.getLength() == 0) {	// an insertion point can't cover anything
			return false;
		} else {
			return thisRegion.getOffset() <= otherRegion.getOffset() && otherRegion.getExclusiveEnd() <= thisRegion.getExclusiveEnd();
		}		
	}
	
	public static TextRange intersect(TextRange op1, TextRange op2) {
		int offset1= op1.getOffset();
		int length1= op1.getLength();
		int end1= offset1 + length1 - 1;
		int offset2= op2.getOffset();
		if (end1 < offset2)
			return null;
		int length2= op2.getLength();
		int end2= offset2 + length2 - 1;
		if (end2 < offset1)
			return null;
		if (offset1 < offset2) {
			int end= Math.max(end1, end2);
			return new TextRange(offset2, end - offset2 + 1);
		} else {
			int end= Math.max(end1, end2);
			return new TextRange(offset1, end - offset1 + 1); 
		}
	}
}
