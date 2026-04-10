/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - refactored from TypeArgumentMismatchSubProcessor
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposalCore;

public abstract class TypeArgumentMismatchBaseSubProcessor<T> {

	protected TypeArgumentMismatchBaseSubProcessor() {
	}

	public void addRemoveMismatchedArgumentProposals(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {

		ICompilationUnit cu= context.getCompilationUnit();
		ASTNode selectedNode= problem.getCoveredNode(context.getASTRoot());
		if (!(selectedNode instanceof SimpleName)) {
			return;
		}

		ASTNode normalizedNode=ASTNodes.getNormalizedNode(selectedNode);
		if (normalizedNode instanceof ParameterizedType) {
			ASTRewrite rewrite = ASTRewrite.create(normalizedNode.getAST());
			ParameterizedType pt = (ParameterizedType) normalizedNode;
			ASTNode mt = rewrite.createMoveTarget(pt.getType());
			rewrite.replace(pt, mt, null);
			String label= CorrectionMessages.TypeArgumentMismatchSubProcessor_removeTypeArguments;
			T proposal= astRewriteCorrectionProposalToT(new ASTRewriteCorrectionProposalCore(label, cu, rewrite, IProposalRelevance.REMOVE_TYPE_ARGUMENTS));
			proposals.add(proposal);
		}
	}

	public abstract T astRewriteCorrectionProposalToT(ASTRewriteCorrectionProposalCore core);
}
