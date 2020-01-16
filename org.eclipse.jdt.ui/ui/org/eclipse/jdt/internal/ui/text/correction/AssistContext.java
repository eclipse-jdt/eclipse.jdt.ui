/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.TextInvocationContext;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;

import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;

import org.eclipse.jdt.ui.text.java.IInvocationContext;


public class AssistContext extends TextInvocationContext implements IInvocationContext {

	private final ICompilationUnit fCompilationUnit;
	private final IEditorPart fEditor;

	private CompilationUnit fASTRoot;
	private final SharedASTProviderCore.WAIT_FLAG fWaitFlag;
	/**
	 * The cached node finder, can be null.
	 * @since 3.6
	 */
	private NodeFinder fNodeFinder;


	/*
	 * @since 3.5
	 */
	private AssistContext(ICompilationUnit cu, ISourceViewer sourceViewer, IEditorPart editor, int offset, int length, SharedASTProviderCore.WAIT_FLAG waitFlag) {
		super(sourceViewer, offset, length);
		Assert.isLegal(cu != null);
		Assert.isLegal(waitFlag != null);
		fCompilationUnit= cu;
		fEditor= editor;
		fWaitFlag= waitFlag;
	}

	/*
	 * @since 3.5
	 */
	public AssistContext(ICompilationUnit cu, ISourceViewer sourceViewer, int offset, int length, SharedASTProviderCore.WAIT_FLAG waitFlag) {
		this(cu, sourceViewer, null, offset, length, waitFlag);
	}

	/*
	 * @since 3.5
	 */
	public AssistContext(ICompilationUnit cu, ISourceViewer sourceViewer, IEditorPart editor, int offset, int length) {
		this(cu, sourceViewer, editor, offset, length, SharedASTProviderCore.WAIT_YES);
	}

	/*
	 * Constructor for CorrectionContext.
	 * @since 3.4
	 */
	public AssistContext(ICompilationUnit cu, ISourceViewer sourceViewer, int offset, int length) {
		this(cu, sourceViewer, null, offset, length);
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
	@Override
	public ICompilationUnit getCompilationUnit() {
		return fCompilationUnit;
	}

	/**
	 * Returns the editor or <code>null</code> if none.
	 * @return an <code>IEditorPart</code> or <code>null</code> if none
	 * @since 3.5
	 */
	public IEditorPart getEditor() {
		return fEditor;
	}

	/**
	 * Returns the length.
	 * @return int
	 */
	@Override
	public int getSelectionLength() {
		return Math.max(getLength(), 0);
	}

	/**
	 * Returns the offset.
	 * @return int
	 */
	@Override
	public int getSelectionOffset() {
		return getOffset();
	}

	@Override
	public CompilationUnit getASTRoot() {
		if (fASTRoot == null) {
			fASTRoot= SharedASTProviderCore.getAST(fCompilationUnit, fWaitFlag, null);
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

	@Override
	public ASTNode getCoveringNode() {
		if (fNodeFinder == null) {
			fNodeFinder= new NodeFinder(getASTRoot(), getOffset(), getLength());
		}
		return fNodeFinder.getCoveringNode();
	}

	@Override
	public ASTNode getCoveredNode() {
		if (fNodeFinder == null) {
			fNodeFinder= new NodeFinder(getASTRoot(), getOffset(), getLength());
		}
		return fNodeFinder.getCoveredNode();
	}

}
