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
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
/**
  */
public interface ICorrectionContext {

	/**
	 * Returns the current compilation unit
	 */
	ICompilationUnit getCompilationUnit();

	/**
	 * Returns the length of the current selection
	 * @return int
	 */
	int getLength();
	
	/**
	 * Returns the offset of the current selection
	 * @return int
	 */
	int getOffset();
	
	/**
	 * Returns the problem is associated with this context. 0 is returned if no problem exists. 
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
	 * Creates an AST on the compilation unit and returns the AST root element. The AST returned is shared and must
	 * not be modified (or modifications must be reverted back again)
	 * @return CompilationUnit
	 */
	CompilationUnit getASTRoot();
	
	/**
	 * Returns the the AST node that covers the current selection or <code>null</code> if no node is found
	 * that completly covers the selection range.
	 * @return ASTNode
	 */
	ASTNode getCoveringNode();

	/**
	 * Returns the the AST node that is covered by the current selection or <code>null</code> if no node is found
	 * that completely covered by the selection range.
	 * @return ASTNode
	 */
	ASTNode getCoveredNode();
}
