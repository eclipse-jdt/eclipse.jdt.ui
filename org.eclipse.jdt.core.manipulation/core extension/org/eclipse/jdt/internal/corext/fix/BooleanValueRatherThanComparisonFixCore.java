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
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class BooleanValueRatherThanComparisonFixCore extends CompilationUnitRewriteOperationsFixCore {
	public static final class BooleanValueRatherThanComparisonFinder extends ASTVisitor {
		private List<CompilationUnitRewriteOperation> fResult;

		public BooleanValueRatherThanComparisonFinder(List<CompilationUnitRewriteOperation> ops) {
			fResult= ops;
		}

		@Override
		public boolean visit(final MethodInvocation visited) {
			if (ASTNodes.usesGivenSignature(visited, Boolean.class.getCanonicalName(), "equals", Object.class.getCanonicalName())) { //$NON-NLS-1$
				Boolean isExpressionTrue= ASTNodes.getBooleanLiteral(visited.getExpression());
				Expression argument= (Expression) visited.arguments().get(0);

				if (isExpressionTrue != null
						// A primitive may not create a NPE
						&& ASTNodes.isPrimitive(argument, boolean.class.getSimpleName())) {
					fResult.add(new BooleanValueRatherThanComparisonOperation(visited, argument, isExpressionTrue));
					return false;
				}

				Boolean isArgumentTrue= ASTNodes.getBooleanLiteral(argument);

				// The result has as many NPE threads as the original code so it's OK
				if (visited.getExpression() != null
						&& (Boolean.FALSE.equals(isArgumentTrue)
								|| Boolean.TRUE.equals(isArgumentTrue) && visited.getLocationInParent() != MethodInvocation.ARGUMENTS_PROPERTY && ASTNodes.hasType(ASTNodes.getTargetType(visited), boolean.class.getSimpleName()))) {
					fResult.add(new BooleanValueRatherThanComparisonOperation(visited, visited.getExpression(), isArgumentTrue));
					return false;
				}
			}

			return true;
		}

		@Override
		public boolean visit(final ParenthesizedExpression visited) {
			InfixExpression originalCondition= ASTNodes.as(visited, InfixExpression.class);

			if (originalCondition != null) {
				return maybeRefactorInfixExpression(visited, originalCondition);
			}

			return true;
		}

		@Override
		public boolean visit(final InfixExpression visited) {
			return maybeRefactorInfixExpression(visited, visited);
		}

		private boolean maybeRefactorInfixExpression(final Expression visited, final InfixExpression originalCondition) {
			if (!originalCondition.hasExtendedOperands()
					&& ASTNodes.hasOperator(originalCondition, InfixExpression.Operator.EQUALS, InfixExpression.Operator.NOT_EQUALS, InfixExpression.Operator.XOR)
					// Either:
					// - Two boolean primitives: no possible NPE
					// - One boolean primitive and one Boolean object, this code already run
					// the risk of an NPE, so we can replace the infix expression without
					// fearing we would introduce a previously non existing NPE.
					&& (ASTNodes.isPrimitive(originalCondition.getLeftOperand(), boolean.class.getSimpleName()) || ASTNodes.isPrimitive(originalCondition.getRightOperand(), boolean.class.getSimpleName()))) {
				Expression leftExpression= originalCondition.getLeftOperand();
				Expression rightExpression= originalCondition.getRightOperand();
				boolean isEquals= ASTNodes.hasOperator(originalCondition, InfixExpression.Operator.EQUALS);

				return maybeRemoveConstantOperand(visited, leftExpression, rightExpression, isEquals)
						&& maybeRemoveConstantOperand(visited, rightExpression, leftExpression, isEquals);
			}

			return true;
		}

		private boolean maybeRemoveConstantOperand(final Expression visited, final Expression dynamicOperand,
				final Expression hardCodedOperand, final boolean isEquals) {
			Boolean booleanLiteral= ASTNodes.getBooleanLiteral(hardCodedOperand);

			if (booleanLiteral != null) {
				boolean isTrue= booleanLiteral == isEquals;

				if (!isTrue
						|| ASTNodes.isPrimitive(dynamicOperand, boolean.class.getSimpleName())
						|| (visited.getLocationInParent() != MethodInvocation.ARGUMENTS_PROPERTY && ASTNodes.hasType(ASTNodes.getTargetType(visited), boolean.class.getSimpleName()))) {
					fResult.add(new BooleanValueRatherThanComparisonOperation(visited, dynamicOperand, isTrue));
					return false;
				}
			}

			return true;
		}
	}

	private static class BooleanValueRatherThanComparisonOperation extends CompilationUnitRewriteOperation {
		private final ASTNode visited;
		private final Expression expressionToCopy;
		private final boolean isTrue;

		public BooleanValueRatherThanComparisonOperation(final ASTNode visited, final Expression expressionToCopy,
				final boolean isTrue) {
			this.visited= visited;
			this.expressionToCopy= expressionToCopy;
			this.isTrue= isTrue;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.BooleanValueRatherThanComparisonCleanUp_description, cuRewrite);
			rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
					if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
						return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
					}

					return super.computeSourceRange(nodeWithComment);
				}
			});

			Expression operand;
			if (isTrue) {
				operand= ASTNodes.createMoveTarget(rewrite, expressionToCopy);
			} else {
				operand= ASTNodeFactory.negate(ast, rewrite, expressionToCopy, true);
			}

			rewrite.replace(visited, ASTNodeFactory.parenthesizeIfNeeded(ast, operand), group);
		}
	}

	public static ICleanUpFixCore createCleanUp(final CompilationUnit compilationUnit) {
		List<CompilationUnitRewriteOperation> operations= new ArrayList<>();
		BooleanValueRatherThanComparisonFinder finder= new BooleanValueRatherThanComparisonFinder(operations);
		compilationUnit.accept(finder);

		if (operations.isEmpty()) {
			return null;
		}

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[0]);
		return new BooleanValueRatherThanComparisonFixCore(FixMessages.BooleanValueRatherThanComparisonFix_description, compilationUnit, ops);
	}

	protected BooleanValueRatherThanComparisonFixCore(final String name, final CompilationUnit compilationUnit, final CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
}
