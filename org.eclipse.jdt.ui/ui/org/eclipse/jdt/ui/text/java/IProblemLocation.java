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
package org.eclipse.jdt.ui.text.java;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;


/**
 * Problem information for quick fix and quick assist processors.
 * <p>
 * Note: this interface is not intended to be implemented.
 * </p>
 *
 * @since 3.0
 */
public interface IProblemLocation {
	
	/**
	 * Returns the start offset of the problem.
	 * 
	 * @return the start offset of the problem 
	 */
	int getOffset();
	
	/**
	 * Returns the length of the problem.
	 * 
	 * @return the length of the problem
	 */
	int getLength();
	
	/**
	 * Returns the id of problem. See {@link org.eclipse.jdt.core.compiler.IProblem} for
	 * id definitions.
	 * 
	 * @return The id of the problem.
	 */
	int getProblemId();
	
	/**
	 * Returns the original arguments recorded into the problem.
	 * 
	 * @return String[] Returns the problem arguments.
	 */
	String[] getProblemArguments();
	
	/**
	 * Returns if the problem has error severity.
	 * 
	 * @return <code>true</code> if the problem has error severity 
	 */
	boolean isError();
	
	/**
	 * Convenience method to evaluate the AST node covering this problem.
	 * 
	 * @param astRoot The root node of the current AST
	 * @return Returns the node that covers the location of the problem
	 */
	ASTNode getCoveringNode(CompilationUnit astRoot);

	/**
	 * Convenience method to evaluate the AST node covered by this problem.
	 * 
	 * @param astRoot The root node of the current AST
	 * @return Returns the node that is covered by the location of the problem
	 */	
	ASTNode getCoveredNode(CompilationUnit astRoot);

}
