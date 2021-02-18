/*******************************************************************************
 * Copyright (c) 2021 Fabrice TIERCELIN and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fabrice TIERCELIN - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class InvertEqualsFixCore extends CompilationUnitRewriteOperationsFixCore {
	public static final class InvertEqualsFinder extends ASTVisitor {
		private List<InvertEqualsFixOperation> fResult;

		public InvertEqualsFinder(List<InvertEqualsFixOperation> ops) {
			fResult= ops;
		}

		@Override
		public boolean visit(final MethodInvocation visited) {
			Expression expression= visited.getExpression();
			InfixExpression concatenation= ASTNodes.as(expression, InfixExpression.class);

			if (expression == null
					|| ASTNodes.is(expression, ThisExpression.class)
					|| isConstant(expression)
					|| concatenation != null && ASTNodes.hasOperator(concatenation, InfixExpression.Operator.PLUS) && ASTNodes.hasType(concatenation, String.class.getCanonicalName())) {
				return true;
			}

			if (ASTNodes.usesGivenSignature(visited, Object.class.getCanonicalName(), "equals", Object.class.getCanonicalName()) //$NON-NLS-1$
					|| ASTNodes.usesGivenSignature(visited, String.class.getCanonicalName(), "equalsIgnoreCase", String.class.getCanonicalName())) { //$NON-NLS-1$
				Expression arg0= (Expression) visited.arguments().get(0);
				InfixExpression concatenationArgument= ASTNodes.as(arg0, InfixExpression.class);

				if (isConstant(arg0) && arg0.resolveTypeBinding() != null && !arg0.resolveTypeBinding().isPrimitive()
						|| ASTNodes.is(arg0, ThisExpression.class)
						|| concatenationArgument != null && ASTNodes.hasOperator(concatenationArgument, InfixExpression.Operator.PLUS) && ASTNodes.hasType(concatenationArgument, String.class.getCanonicalName())) {
					fResult.add(new InvertEqualsFixOperation(expression, arg0));
					return false;
				}
			}

			return true;
		}

		private static boolean isConstant(final Expression expression) {
			if (expression != null && expression.resolveConstantExpressionValue() != null) {
				return true;
			}

			if (expression instanceof Name) {
				IBinding binding= ((Name) expression).resolveBinding();

				if (binding instanceof IVariableBinding) {
					return ((IVariableBinding) binding).isEnumConstant();
				}
			}

			return false;
		}
	}

	public static class InvertEqualsFixOperation extends CompilationUnitRewriteOperation {
		private final Expression expression;
		private final Expression arg0;

		public InvertEqualsFixOperation(final Expression expression, final Expression arg0) {
			this.expression= expression;
			this.arg0= arg0;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.InvertEqualsCleanUp_description, cuRewrite);
			rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
					if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
						return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
					}

					return super.computeSourceRange(nodeWithComment);
				}
			});


			ASTNodes.replaceButKeepComment(rewrite, expression, ASTNodeFactory.parenthesizeIfNeeded(ast, ASTNodes.createMoveTarget(rewrite, arg0)), group);
			ASTNodes.replaceButKeepComment(rewrite, arg0, ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression(expression)), group);
		}
	}


	public static ICleanUpFixCore createCleanUp(final CompilationUnit compilationUnit) {
		List<InvertEqualsFixOperation> operations= new ArrayList<>();
		InvertEqualsFinder finder= new InvertEqualsFinder(operations);
		compilationUnit.accept(finder);

		if (operations.isEmpty()) {
			return null;
		}

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[0]);
		return new InvertEqualsFixCore(FixMessages.InvertEqualsFix_invert, compilationUnit, ops);
	}

	protected InvertEqualsFixCore(final String name, final CompilationUnit compilationUnit, final CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
}
