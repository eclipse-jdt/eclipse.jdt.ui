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
		
	protected void addEdits(IDocument document) throws CoreException {
		super.addEdits(document);
		ASTRewrite rewrite= getRewrite();
		if (rewrite != null) {
			try {
				TextEdit edit= rewrite.rewriteAST(document, null);
				getRootTextEdit().addChild(edit);
				rewrite.removeModifications();
			} catch (RewriteException e) {
				throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
			}
		}
	}
	
	protected ASTRewrite getRewrite() throws CoreException {
		return fRewrite;
	}
	
	public void ensureNoModifications() throws CoreException {
		if (fRewrite != null && fRewrite.hasASTModifications()) {
			getChange(); // force the rewriting
		}
	}


}
