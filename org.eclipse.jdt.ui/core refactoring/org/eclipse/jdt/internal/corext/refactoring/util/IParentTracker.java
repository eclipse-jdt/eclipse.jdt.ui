/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.List;

import org.eclipse.jdt.internal.compiler.ast.AstNode;

/**
 * This interface gives AST node visitors access to the parent
 * of an </code>AstNode<code>.
 */
public interface IParentTracker {

	/**
	 * Returns the parent of the currently active AST node. Returns
	 * <code>null</code> if the current AST node doesn't have any 
	 * parent.
	 * 
	 * @return the parent of the currently active AST node
	 */
	public AstNode getParent();
	
	/**
	 * Returns the list of parents of the currently active AST node.
	 * 
	 * @return the list of parents of the currently active AST node.
	 */
	public List getParents();
}
