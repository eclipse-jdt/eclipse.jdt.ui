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
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.text.java.IInvocationContext;

import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;

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
			fASTRoot= JavaPlugin.getDefault().getASTProvider().getAST(fCompilationUnit, true, null);
			if (fASTRoot == null) {
				// see bug 63554
				ASTParser parser= ASTParser.newParser(ASTProvider.AST_LEVEL);
				parser.setSource(fCompilationUnit);
				parser.setResolveBindings(true);
				fASTRoot= (CompilationUnit) parser.createAST(null);
			}
		}
		return fASTRoot; 
	}

		
	/**
	 * @param root The ASTRoot to set.
	 */
	public void setASTRoot(CompilationUnit root) {
		fASTRoot= root;
	}
	
	/*(non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IInvocationContext#getCoveringNode()
	 */
	public ASTNode getCoveringNode() {
		NodeFinder finder= new NodeFinder(fOffset, fLength);
		getASTRoot().accept(finder);
		return finder.getCoveringNode();
	}

	/*(non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IInvocationContext#getCoveredNode()
	 */
	public ASTNode getCoveredNode() {
		NodeFinder finder= new NodeFinder(fOffset, fLength);
		getASTRoot().accept(finder);
		return finder.getCoveredNode();
	}

}
