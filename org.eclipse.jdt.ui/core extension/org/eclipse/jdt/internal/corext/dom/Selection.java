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
package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jdt.core.dom.ASTNode;

import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.internal.corext.Assert;

public class Selection {
	
	/** Flag indicating that the AST node somehow intersects with the selection. */
	public static final int INTERSECTS= 0;
	
	/** Flag that indicates that an AST node appears before the selected nodes. */
	public static final int BEFORE= 1;
	
	/** Flag indicating that an AST node is covered by the selection. */
	public static final int SELECTED= 2;
	
	/** Flag indicating that an AST nodes appears after the selected nodes. */
	public static final int AFTER= 3;
	
	private int fStart;
	private int fLength;
	private int fExclusiveEnd;	
	
	protected Selection() {
	}
	
	/**
	 * Creates a new selection from the given start and length.
	 * 
	 * @param s the start offset of the selection (inclusive)
	 * @param l the length of the selection
	 * @return the created selection object
	 */
	public static Selection createFromStartLength(int s, int l) {
		Assert.isTrue(s >= 0 && l >= 0);
		Selection result= new Selection();
		result.fStart= s;
		result.fLength= l;
		result.fExclusiveEnd= s + l;
		return result;
	}
	
	/**
	 * Creates a new selection from the given start and end.
	 * 
	 * @param s the start offset of the selection (inclusive)
	 * @param e the end offset of the selection (inclusive)
	 * @return the created selection object
	 */
	public static Selection createFromStartEnd(int s, int e) {
		Assert.isTrue(s >= 0 && e >= s);
		Selection result= new Selection();
		result.fStart= s;
		result.fLength= e - s + 1;
		result.fExclusiveEnd= result.fStart + result.fLength;
		return result;
	}
	
	public int getOffset() {
		return fStart;
	}
	
	public int getLength() {
		return fLength;
	}
	
	public int getInclusiveEnd() {
		return fExclusiveEnd - 1;
	}
	
	public int getExclusiveEnd() {
		return fExclusiveEnd;
	}
	
	/**
	 * Returns the selection mode of the given AST node regarding this selection. Possible
	 * values are <code>INTERSECTS</code>, <code>BEFORE</code>, <code>SELECTED</code>, and
	 * <code>AFTER</code>.
	 * 
	 * @return the selection mode of the given AST node regarding this selection
	 * @see #INTERSECTS
	 * @see #BEFORE
	 * @see #SELECTED
	 * @see #AFTER
	 */
	public int getVisitSelectionMode(ASTNode node) {
		int nodeStart= node.getStartPosition();
		int nodeEnd= nodeStart + node.getLength();
		if (nodeEnd <= fStart)
			return BEFORE;
		else if (covers(node))
			return SELECTED;
		else if (fExclusiveEnd <= nodeStart)
			return AFTER;
		return INTERSECTS;
	}
	
	public int getEndVisitSelectionMode(ASTNode node) {
		int nodeStart= node.getStartPosition();
		int nodeEnd= nodeStart + node.getLength();
		if (nodeEnd <= fStart)
			return BEFORE;
		else if (covers(node))
			return SELECTED;
		else if (nodeEnd >= fExclusiveEnd)
			return AFTER;
		return INTERSECTS;
	}
	
	// enclosed* methods do an open interval check.
	
	public boolean enclosedBy(ASTNode node) {
		int nodeStart= node.getStartPosition();
		return nodeStart < fStart && fExclusiveEnd < nodeStart + node.getLength();
	}
	
//	public boolean enclosedBy(int nodeStart, int nodeLength) {
//		return nodeStart < fStart && fStart + fLength < nodeStart + nodeLength;
//	}
	
	// cover* methods do a closed interval check.
	
	public boolean covers(int position) {
		return fStart <= position && position < fStart + fLength;
	}
	
	public boolean covers(ASTNode node) {
		int nodeStart= node.getStartPosition();
		return fStart <= nodeStart && nodeStart + node.getLength() <= fExclusiveEnd;
	}
	
//	public boolean covers(int nodeStart, int nodeLength) {
//		return fStart <= nodeStart && nodeStart + nodeLength <= fExclusiveEnd;
//	}
	
	public boolean coveredBy(ASTNode node) {
		int nodeStart= node.getStartPosition();
		return nodeStart <= fStart && fExclusiveEnd <= nodeStart + node.getLength();
	}
	
	public boolean coveredBy(IRegion region) {
		int rangeStart= region.getOffset();
		return rangeStart <= fStart && fExclusiveEnd <= rangeStart + region.getLength();
	}
	
//	public boolean coveredBy(int sourceStart, int sourceEnd) {
//		return sourceStart <= start && end <= sourceEnd;
//	}
//	
	public boolean endsIn(ASTNode node) {
		int nodeStart= node.getStartPosition();
		return nodeStart < fExclusiveEnd && fExclusiveEnd < nodeStart + node.getLength();
	}
	
	public boolean liesOutside(ASTNode node) {
		int nodeStart= node.getStartPosition();
		return nodeStart + node.getLength() <= fStart || fExclusiveEnd <= nodeStart;
	}
	
//	public boolean intersects(int sourceStart, int sourceEnd) {
//		return sourceStart < start && start <= sourceEnd && sourceEnd <= end ||
//		       start <= sourceStart && sourceStart <= end && end < sourceEnd;
//	}
//	
//	public boolean intersects(AstNode node) {
//		return intersects(node.sourceStart, node.sourceEnd);
//	}
	
	/* non javadoc
	 * for debugging only
	 */
	public String toString() {
		return "<start == " + fStart + ", length == " + fLength + "/>";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}	
}
