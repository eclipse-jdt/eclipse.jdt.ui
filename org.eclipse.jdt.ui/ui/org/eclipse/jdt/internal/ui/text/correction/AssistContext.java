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
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.text.java.*;

/**
  */
public class AssistContext implements IInvocationContext {
	
	private ICompilationUnit fCompilationUnit;
	private int fOffset;
	private int fLength;
		
	private CompilationUnit fASTRoot;
	
	/*
	 * Constructor for CorrectionContext.
	 */
	public AssistContext(ICompilationUnit cu, int offset, int length) {
		fCompilationUnit= cu;
		fOffset= offset;
		fLength= length;

		fASTRoot= null;
	}
		
	/**
	 * Returns the compilation unit.
	 * @return Returns a ICompilationUnit
	 */
	public ICompilationUnit getCompilationUnit() {
		return fCompilationUnit;
	}
	
	/**
	 * Returns the length.
	 * @return int
	 */
	public int getSelectionLength() {
		return fLength;
	}

	/**
	 * Returns the offset.
	 * @return int
	 */
	public int getSelectionOffset() {
		return fOffset;
	}
	
	public CompilationUnit getASTRoot() {
		if (fASTRoot == null) {
			fASTRoot= AST.parsePartialCompilationUnit(fCompilationUnit, fOffset, true, null, null);
		}
		return fASTRoot;
	}

		
	/**
	 * @param root The ASTRoot to set.
	 */
	public void setASTRoot(CompilationUnit root) {
		fASTRoot= root;
	}

}
