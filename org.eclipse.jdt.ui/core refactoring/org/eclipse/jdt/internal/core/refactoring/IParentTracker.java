/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring;import org.eclipse.jdt.internal.compiler.ast.AstNode;

/**
 * This interface give AST node visitors access to the parent
 * of an </code>AstNode<code>.
 */
public interface IParentTracker {

	/**
	 * Returns the parent of the currently active AST node, Returns
	 * <code>null</code> if the current AST node doesn't have any 
	 * parent.
	 * 
	 * @return the parent of the currently active AST node
	 */
	public AstNode getParent();
}
