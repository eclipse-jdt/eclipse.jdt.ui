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
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.fix.helper.LambdaQueries;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public class ChangeLambdaBodyToExpressionFixCore extends CompilationUnitRewriteOperationsFixCore {

	public ChangeLambdaBodyToExpressionFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation operation) {
		super(name, compilationUnit, operation);
	}

	public static ChangeLambdaBodyToExpressionFixCore createChangeLambdaBodyToBlockFix(CompilationUnit compilationUnit, ASTNode node) {
		LambdaExpression lambda;
		if (node instanceof LambdaExpression) {
			lambda= (LambdaExpression) node;
		} else if (node.getLocationInParent() == LambdaExpression.BODY_PROPERTY) {
			lambda= (LambdaExpression) node.getParent();
		} else {
			return null;
		}

		if (!(lambda.getBody() instanceof Block))
			return null;

		Block lambdaBody= (Block) lambda.getBody();

		Expression exprBody= LambdaQueries.getSingleExpressionFromLambdaBody(lambdaBody);
		if (exprBody == null)
			return null;

		String label= CorrectionMessages.QuickAssistProcessor_change_lambda_body_to_expression;
		return new ChangeLambdaBodyToExpressionFixCore(label, compilationUnit, new ChangeLambdaBodyToExpressionProposalOperation(lambda, exprBody));
	}

	private static class ChangeLambdaBodyToExpressionProposalOperation extends CompilationUnitRewriteOperation {

		private LambdaExpression lambda;

		private Expression exprBody;

		public ChangeLambdaBodyToExpressionProposalOperation(LambdaExpression lambda, Expression exprBody) {
			this.lambda= lambda;
			this.exprBody= exprBody;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			Expression movedBody= (Expression) rewrite.createMoveTarget(exprBody);
			rewrite.set(lambda, LambdaExpression.BODY_PROPERTY, movedBody, null);
		}
	}
}
