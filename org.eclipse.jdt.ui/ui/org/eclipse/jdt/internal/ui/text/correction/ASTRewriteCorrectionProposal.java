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

import java.util.Map;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.RewriteException;
import org.eclipse.jdt.internal.ui.JavaUIStatus;

/**
  */
public class ASTRewriteCorrectionProposal extends CUCorrectionProposal {

	private ASTRewrite fRewrite;

	public ASTRewriteCorrectionProposal(String name, ICompilationUnit cu, ASTRewrite rewrite, int relevance, Image image) {
		super(name, cu, relevance, image);
		fRewrite= rewrite;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal#addEdits(org.eclipse.jface.text.IDocument)
	 */
	protected void addEdits(IDocument document) throws CoreException {
		super.addEdits(document);
		ASTRewrite rewrite= getRewrite();
		if (rewrite != null) {
			try {
				Map options= getCompilationUnit().getJavaProject().getOptions(true);
				TextEdit edit= rewrite.rewriteAST(document, options);
				getRootTextEdit().addChild(edit);
			} catch (RewriteException e) {
				throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
			}
		}
	}
	
	/**
	 * Returns the rewriter that has been passed in the constructor. Implemententors can override this
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
