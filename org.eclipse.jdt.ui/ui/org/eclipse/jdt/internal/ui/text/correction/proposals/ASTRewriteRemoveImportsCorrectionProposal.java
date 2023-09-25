/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.refactoring.structure.ImportRemover;

import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;

public class ASTRewriteRemoveImportsCorrectionProposal extends ASTRewriteCorrectionProposal{

	public ASTRewriteRemoveImportsCorrectionProposal(String name, ICompilationUnit cu, ASTRewrite rewrite, int relevance) {
		super(name, cu, rewrite, relevance);
		setDelegate(new ASTRewriteRemoveImportsCorrectionProposalCore(name, cu, rewrite, relevance));
	}

	public ASTRewriteRemoveImportsCorrectionProposal(String name, ICompilationUnit cu, ASTRewrite rewrite, int relevance, Image image) {
		super(name, cu, rewrite, relevance, image);
		setDelegate(new ASTRewriteRemoveImportsCorrectionProposalCore(name, cu, rewrite, relevance));
	}

	public void setImportRemover(ImportRemover remover) {
		((ASTRewriteRemoveImportsCorrectionProposalCore)getDelegate()).setImportRemover(remover);
	}

}
