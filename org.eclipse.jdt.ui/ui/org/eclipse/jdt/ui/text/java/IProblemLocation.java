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
 *
 */
public interface IProblemLocation {
	
	/**
	 * Returns the id of problem that is associated with this context. See {@link org.eclipse.jdt.core.compiler.IProblem} for
	 * id definitions
	 * @return int
	 */
	int getProblemId();
	
	/**
	 * Returns the problem argument is associated with this context. <code>null</code> is returned
	 * if no problem exists. 
	 * @return String[]
	 */
	String[] getProblemArguments();
	
	/**
	 * Returns the length of the problem
	 * @return int
	 */
	int getLength();
	
	/**
	 * Returns the offset of the problem
	 * @return int
	 */
	int getOffset();
	
	/**
	 * Returns the node that covers the location of the problem
	 */
	public ASTNode getCoveringNode(CompilationUnit astRoot);

	/**
	 * Returns the node that is covered by the location of the problem
	 */	
	public ASTNode getCoveredNode(CompilationUnit astRoot);
	

}
