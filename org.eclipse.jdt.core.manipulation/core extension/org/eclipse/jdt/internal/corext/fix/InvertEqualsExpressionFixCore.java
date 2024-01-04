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

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public class InvertEqualsExpressionFixCore extends CompilationUnitRewriteOperationsFixCore {

	public InvertEqualsExpressionFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation operation) {
		super(name, compilationUnit, operation);
	}

	public static InvertEqualsExpressionFixCore createInvertEqualsFix(CompilationUnit compilationUnit, ASTNode node) {
		if (!(node instanceof MethodInvocation)) {
			node= node.getParent();
			if (!(node instanceof MethodInvocation)) {
				return null;
			}
		}
		MethodInvocation method= (MethodInvocation) node;
		String identifier= method.getName().getIdentifier();
		if (!"equals".equals(identifier) && !"equalsIgnoreCase".equals(identifier)) { //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}
		List<Expression> arguments= method.arguments();
		if (arguments.size() != 1) { //overloaded equals w/ more than 1 argument
			return null;
		}
		Expression right= arguments.get(0);
		ITypeBinding binding= right.resolveTypeBinding();
		if (binding != null
				&& !binding.isClass()
				&& !binding.isInterface()
				&& !binding.isEnum()) { //overloaded equals w/ non-class/interface argument or null
			return null;
		}

		Expression left= method.getExpression();
		String label= CorrectionMessages.QuickAssistProcessor_invertequals_description;
		return new InvertEqualsExpressionFixCore(label, compilationUnit, new InvertEqualsProposalOperation(method, left, right));
	}

	private static class InvertEqualsProposalOperation extends CompilationUnitRewriteOperation {

		private MethodInvocation method;

		private Expression left;

		private Expression right;

		public InvertEqualsProposalOperation(MethodInvocation method, Expression left, Expression right) {
			this.method= method;
			this.left= left;
			this.right= right;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
			AST ast= cuRewrite.getAST();
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			if (left == null) { // equals(x) -> x.equals(this)
				MethodInvocation replacement= ast.newMethodInvocation();
				replacement.setName((SimpleName) rewrite.createCopyTarget(method.getName()));
				replacement.arguments().add(ast.newThisExpression());
				replacement.setExpression((Expression) rewrite.createCopyTarget(right));
				rewrite.replace(method, replacement, null);
			} else if (right instanceof ThisExpression) { // x.equals(this) -> equals(x)
				MethodInvocation replacement= ast.newMethodInvocation();
				replacement.setName((SimpleName) rewrite.createCopyTarget(method.getName()));
				replacement.arguments().add(rewrite.createCopyTarget(left));
				rewrite.replace(method, replacement, null);
			} else {
				ASTNode leftExpression= ASTNodes.getUnparenthesedExpression(left);
				rewrite.replace(right, rewrite.createCopyTarget(leftExpression), null);

				if (right instanceof CastExpression
						|| right instanceof Assignment
						|| right instanceof ConditionalExpression
						|| right instanceof InfixExpression) {
					ParenthesizedExpression paren= ast.newParenthesizedExpression();
					paren.setExpression((Expression) rewrite.createCopyTarget(right));
					rewrite.replace(left, paren, null);
				} else {
					rewrite.replace(left, rewrite.createCopyTarget(right), null);
				}
			}
		}

	}
}
