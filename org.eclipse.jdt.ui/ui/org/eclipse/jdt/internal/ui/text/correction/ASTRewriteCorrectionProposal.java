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

import java.util.Map;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.ui.JavaUIStatus;

/**
 * A proposal for quick fixes and quick assists that works on a AST rewriter.
 * Either a rewriter is directly passed in the constructor or method {@link #getRewrite()} is overridden
 * to provide the AST rewriter that is evaluated to the document when the proposal is
 * applied.
 * @since 3.0
 */
public class ASTRewriteCorrectionProposal extends CUCorrectionProposal {

	private ASTRewrite fRewrite;

	/**
	 * Constructs a AST rewrite correction proposal.
	 * @param name The display name of the proposal.
	 * @param cu The compilation unit that is modified.
	 * @param rewrite The AST rewrite that is invoked when the proposal is applied
	 *  <code>null</code> can be passed if {@link #getRewrite()} is overridden.
	 * @param relevance The relevance of this proposal.
	 * @param image The image that is displayed for this proposal or <code>null</code> if no
	 * image is desired.
	 */
	public ASTRewriteCorrectionProposal(String name, ICompilationUnit cu, ASTRewrite rewrite, int relevance, Image image) {
		super(name, cu, relevance, image);
		fRewrite= rewrite;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal#addEdits(org.eclipse.jface.text.IDocument)
	 */
	protected void addEdits(IDocument document, TextEdit editRoot) throws CoreException {
		super.addEdits(document, editRoot);
		ASTRewrite rewrite= getRewrite();
		if (rewrite != null) {
			try {
				Map options= getCompilationUnit().getJavaProject().getOptions(true);
				TextEdit edit= rewrite.rewriteAST(document, options);
				editRoot.addChild(edit);
			} catch (IllegalArgumentException e) {
				throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
			}
		}
	}
	
	/**
	 * Returns the rewriter that has been passed in the constructor. Implementors can override this
	 * method to create the rewriter lazy. 
	 * @return Returns the rewriter to be used.
	 * @throws CoreException A core exception is thrown when the could not be created.
	 */
	protected ASTRewrite getRewrite() throws CoreException {
		if (fRewrite == null) {
			IStatus status= JavaUIStatus.createError(IStatus.ERROR, "Rewriter not initialized", null); //$NON-NLS-1$
			throw new CoreException(status);
		}
		return fRewrite;
	}
}
