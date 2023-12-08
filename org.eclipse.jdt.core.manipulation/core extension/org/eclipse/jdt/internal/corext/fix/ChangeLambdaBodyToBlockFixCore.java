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
 *     Gayan Perera - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.LambdaExpression;

import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.text.correction.QuickAssistProcessorUtil;

public class ChangeLambdaBodyToBlockFixCore extends CompilationUnitRewriteOperationsFixCore {

	public ChangeLambdaBodyToBlockFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation operation) {
		super(name, compilationUnit, operation);
	}

	public static ChangeLambdaBodyToBlockFixCore createChangeLambdaBodyToBlockFix(CompilationUnit compilationUnit, ASTNode node) {
		LambdaExpression lambda;
		if (node instanceof LambdaExpression) {
			lambda= (LambdaExpression) node;
		} else if (node.getLocationInParent() == LambdaExpression.BODY_PROPERTY) {
			lambda= (LambdaExpression) node.getParent();
		} else {
			return null;
		}

		if (!(lambda.getBody() instanceof Expression))
			return null;
		if (lambda.resolveMethodBinding() == null)
			return null;

		String label= CorrectionMessages.QuickAssistProcessor_change_lambda_body_to_block;
		return new ChangeLambdaBodyToBlockFixCore(label, compilationUnit, new ChangeLambdaBodyToBlockProposalOperation(lambda));
	}

	private static class ChangeLambdaBodyToBlockProposalOperation extends CompilationUnitRewriteOperation {

		private LambdaExpression lambda;

		public ChangeLambdaBodyToBlockProposalOperation(LambdaExpression lambda) {
			this.lambda= lambda;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
			QuickAssistProcessorUtil.changeLambdaBodyToBlock(lambda, cuRewrite.getAST(), cuRewrite.getASTRewrite());
		}
	}
}
