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
package org.eclipse.jdt.ui.text.java;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 * This API is work in progress and may change before the final 3.0 release.
 * This interface is not intended to be implemented.
 * @since 3.0
 */
public interface IProblemLocation {
	
	/**
	 * Returns the id of problem that is associated with this context. See {@link org.eclipse.jdt.core.compiler.IProblem} for
	 * id definitions
	 * @return int The id of the problem.
	 */
	int getProblemId();
	
	/**
	 * Returns the problem argument is associated with this context. <code>null</code> is returned
	 * if no problem exists. 
	 * @return String[] The problem arguments.
	 */
	String[] getProblemArguments();
	
	/**
	 * @return Returns if the problem has error severity.
	 */
	boolean isError();
	
	/**
	 * @return Returns the length of the problem.
	 */
	int getLength();
	
	/**
	 * @return Returns the offset of the problem.
	 */
	int getOffset();
	
	/**
	 * @param astRoot The root node of the current AST
	 * @return Returns the node that covers the location of the problem
	 */
	ASTNode getCoveringNode(CompilationUnit astRoot);

	/**
	 * @param astRoot The root node of the current AST
	 * @return Returns the node that is covered by the location of the problem
	 */	
	ASTNode getCoveredNode(CompilationUnit astRoot);
	

}
