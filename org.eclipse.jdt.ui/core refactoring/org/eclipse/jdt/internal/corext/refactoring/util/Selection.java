/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.util;

import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.ast.Block;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.corext.refactoring.Assert;

public class Selection {
	
	/** Flag indicating that the AST node somehow intersects with the selection. */
	public static final int INTERSECTS= 0;
	
	/** Flag that indicates that an AST node appears before the selected nodes. */
	public static final int BEFORE= 1;
	
	/** Flag indicating that an AST node is covered by the selection. */
	public static final int SELECTED= 2;
	
	/** Flag indicating that an AST nodes appears after the selected nodes. */
	public static final int AFTER= 3;
	
	public int start;		// inclusive
	public int end;			// inclusive
	
	protected Selection() {
	}
	
	/* non javadoc
	 * for debugging only
	 */
	public String toString(){
		return "<start == " + start + ", end == " + end + "/>"; 
	}
	
	/**
	 * Creates a new selection from the given start and length.
	 * 
	 * @param s the start offset of the selection (inclusive)
	 * @param length the length of the selection
	 * @return the created selection object
	 */
	public static Selection createFromStartLength(int s, int length) {
		Selection result= new Selection();
		result.start= s;
		result.end= s + length - 1;
		Assert.isTrue(result.start <= result.end);
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
		Selection result= new Selection();
		result.start= s;
		result.end= e;
		Assert.isTrue(result.start <= result.end);
		return result;
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
	public int getVisitSelectionMode(AstNode node) {
		int nodeStart= ASTUtil.getSourceStart(node);
		int nodeEnd= ASTUtil.getSourceEnd(node);
		if (nodeEnd < start)
			return BEFORE;
		else if (covers(nodeStart, nodeEnd))
			return SELECTED;
		else if (end < nodeStart)
			return AFTER;
		return INTERSECTS;
	}
	
	public int getEndVisitSelectionMode(AstNode node) {
		int nodeStart= ASTUtil.getSourceStart(node);
		int nodeEnd= ASTUtil.getSourceEnd(node);
		if (nodeEnd < start)
			return BEFORE;
		else if (covers(nodeStart, nodeEnd))
			return SELECTED;
		else if (nodeEnd >= end)
			return AFTER;
		return INTERSECTS;
	}
	
	// enclosed* methods do an open interval check.
	
	public boolean enclosedBy(AstNode node) {
		return node.sourceStart < start && end < node.sourceEnd;		
	}
	
	public boolean enclosedBy(int sourceStart, int sourceEnd) {
		return sourceStart < start && end < sourceEnd;
	}
	
	// cover* methods do a closed interval check.
	
	public boolean covers(AstNode node) {
		return start <= node.sourceStart && node.sourceEnd <= end;
	}
	
	public boolean covers(int position) {
		return start <= position && position <= end;
	}
	
	public boolean covers(int sourceStart, int sourceEnd) {
		return start <= sourceStart && sourceEnd <= end;
	}
	
	public boolean coveredBy(AstNode node) {
		return coveredBy(node.sourceStart, node.sourceEnd);
	}
	
	public boolean coveredBy(int sourceStart, int sourceEnd) {
		return sourceStart <= start && end <= sourceEnd;
	}
	
	public boolean endsIn(int sourceStart, int sourceEnd) {
		return sourceStart <= end && end < sourceEnd;
	}
	
	public boolean intersects(int sourceStart, int sourceEnd) {
		return sourceStart < start && start <= sourceEnd && sourceEnd <= end ||
		       start <= sourceStart && sourceStart <= end && end < sourceEnd;
	}
	
	public boolean intersects(AstNode node) {
		return intersects(node.sourceStart, node.sourceEnd);
	}
}