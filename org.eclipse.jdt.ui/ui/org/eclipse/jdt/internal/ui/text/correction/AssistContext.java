/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.TextInvocationContext;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.dom.NodeFinder;

import org.eclipse.jdt.ui.SharedASTProvider;
import org.eclipse.jdt.ui.text.java.IInvocationContext;


/**
  */
public class AssistContext extends TextInvocationContext implements IInvocationContext {

	private ICompilationUnit fCompilationUnit;

	private CompilationUnit fASTRoot;

	/*
	 * Constructor for CorrectionContext.
	 * @since 3.4
	 */
	public AssistContext(ICompilationUnit cu, ISourceViewer sourceViewer, int offset, int length) {
		super(sourceViewer, offset, length);
		fCompilationUnit= cu;
		fASTRoot= null;
	}

	/*
	 * Constructor for CorrectionContext.
	 */
	public AssistContext(ICompilationUnit cu, int offset, int length) {
		this(cu, null, offset, length);
	}

	/**
	 * Returns the compilation unit.
	 * @return an <code>ICompilationUnit</code>
	 */
	public ICompilationUnit getCompilationUnit() {
		return fCompilationUnit;
	}

	/**
	 * Returns the length.
	 * @return int
	 */
	public int getSelectionLength() {
		return Math.max(getLength(), 0);
	}

	/**
	 * Returns the offset.
	 * @return int
	 */
	public int getSelectionOffset() {
		return getOffset();
	}

	public CompilationUnit getASTRoot() {
		if (fASTRoot == null) {
			fASTRoot= SharedASTProvider.getAST(fCompilationUnit, SharedASTProvider.WAIT_YES, null);
			if (fASTRoot == null) {
				// see bug 63554
				fASTRoot= ASTResolving.createQuickFixAST(fCompilationUnit, null);
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
		NodeFinder finder= new NodeFinder(getOffset(), getLength());
		getASTRoot().accept(finder);
		return finder.getCoveringNode();
	}

	/*(non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IInvocationContext#getCoveredNode()
	 */
	public ASTNode getCoveredNode() {
		NodeFinder finder= new NodeFinder(getOffset(), getLength());
		getASTRoot().accept(finder);
		return finder.getCoveredNode();
	}

}
