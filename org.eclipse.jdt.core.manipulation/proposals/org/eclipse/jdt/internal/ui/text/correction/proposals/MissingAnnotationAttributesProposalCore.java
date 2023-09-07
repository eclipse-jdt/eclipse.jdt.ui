/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
 *     Red Hat Inc. - Body moved from org.eclipse.jdt.internal.ui.text.correction.proposals.MissingAnnotationAttributesProposal
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.MissingAnnotationAttributesProposalOperation;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public class MissingAnnotationAttributesProposalCore extends LinkedCorrectionProposalCore {

	private CompilationUnitRewrite fCompilationUnitRewrite;
	private CompilationUnitRewriteOperation fASTRewriteProposalCore;

	public MissingAnnotationAttributesProposalCore(ICompilationUnit cu, Annotation annotation, int relevance) {
		super(CorrectionMessages.MissingAnnotationAttributesProposal_add_missing_attributes_label, cu, null, relevance);
		fASTRewriteProposalCore= new MissingAnnotationAttributesProposalOperation(annotation);
		fCompilationUnitRewrite= new CompilationUnitRewrite(cu, (CompilationUnit)annotation.getRoot());
	}

	@Override
	protected ASTRewrite getRewrite() throws CoreException {
		fASTRewriteProposalCore.rewriteAST(fCompilationUnitRewrite, getLinkedProposalModel());
		return fCompilationUnitRewrite.getASTRewrite();
	}
}