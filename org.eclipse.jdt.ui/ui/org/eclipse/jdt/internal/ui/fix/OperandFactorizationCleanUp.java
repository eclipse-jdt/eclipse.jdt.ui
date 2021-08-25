/*******************************************************************************
 * Copyright (c) 2020 Fabrice TIERCELIN and others.
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
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTSemanticMatcher;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that replaces (X && Y) || (X && Z) by (X && (Y || Z)):
 * <ul>
 * <li>The operators can be lazy or eager,</li>
 * <li>The factor operand must be passive and primitive,</li>
 * <li>An operand can alter the expression if it's still at the very start of the expression,</li>
 * <li>An operand can alter the expression if it's still at the very end of an eager expression.</li>
 * </ul>
 *
 * Truth table:
 * <pre>
   -------------------------------------------------------------------------------------------
   | a | b | c | (a && b) || (a && c) | a && (b || c) | (a || b) && (a || c) | a || (b && c) |
   | 0 | 0 | 0 |                    0 |             0 |                    0 |             0 |
   | 0 | 0 | 1 |                    0 |             0 |                    0 |             0 |
   | 0 | 1 | 0 |                    0 |             0 |                    0 |             0 |
   | 0 | 1 | 1 |                    0 |             0 |                    1 |             1 |
   | 1 | 0 | 0 |                    0 |             0 |                    1 |             1 |
   | 1 | 0 | 1 |                    1 |             1 |                    1 |             1 |
   | 1 | 1 | 0 |                    1 |             1 |                    1 |             1 |
   | 1 | 1 | 1 |                    1 |             1 |                    1 |             1 |
   -------------------------------------------------------------------------------------------
   </pre>
 */
public class OperandFactorizationCleanUp extends AbstractMultiFix {
	public OperandFactorizationCleanUp() {
		this(Collections.emptyMap());
	}

	public OperandFactorizationCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.OPERAND_FACTORIZATION);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.OPERAND_FACTORIZATION)) {
			return new String[] { MultiFixMessages.OperandFactorizationCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.OPERAND_FACTORIZATION)) {
			return "boolean newBoolean = (repeatedExpression && (thenExpression || elseExpression));\n"; //$NON-NLS-1$
		}

		return "boolean newBoolean = repeatedExpression && thenExpression || repeatedExpression && elseExpression;\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.OPERAND_FACTORIZATION)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final InfixExpression visited) {
				if (!visited.hasExtendedOperands()
						&& ASTNodes.hasOperator(visited, InfixExpression.Operator.CONDITIONAL_OR, InfixExpression.Operator.CONDITIONAL_AND, InfixExpression.Operator.OR, InfixExpression.Operator.AND)) {
					InfixExpression firstCondition= ASTNodes.as(visited.getLeftOperand(), InfixExpression.class);
					InfixExpression secondCondition= ASTNodes.as(visited.getRightOperand(), InfixExpression.class);

					if (firstCondition != null
							&& secondCondition != null
							&& !firstCondition.hasExtendedOperands()
							&& !secondCondition.hasExtendedOperands()
							&& ASTNodes.hasOperator(firstCondition, InfixExpression.Operator.CONDITIONAL_OR, InfixExpression.Operator.CONDITIONAL_AND, InfixExpression.Operator.OR, InfixExpression.Operator.AND)
							&& ASTNodes.hasOperator(secondCondition, InfixExpression.Operator.CONDITIONAL_OR, InfixExpression.Operator.CONDITIONAL_AND, InfixExpression.Operator.OR, InfixExpression.Operator.AND)
							&& Objects.equals(firstCondition.getOperator(), secondCondition.getOperator())
							&& (ASTNodes.hasOperator(visited, InfixExpression.Operator.CONDITIONAL_OR, InfixExpression.Operator.OR) ^ ASTNodes.hasOperator(firstCondition, InfixExpression.Operator.CONDITIONAL_OR, InfixExpression.Operator.OR))) {
						boolean eagerEvaluation= ASTNodes.hasOperator(visited, InfixExpression.Operator.AND, InfixExpression.Operator.OR)
								&& ASTNodes.hasOperator(firstCondition, InfixExpression.Operator.AND, InfixExpression.Operator.OR);

						return maybeReplaceDuplicateExpression(visited, firstCondition.getOperator(), firstCondition.getLeftOperand(),
								secondCondition.getLeftOperand(), firstCondition.getRightOperand(), secondCondition.getRightOperand(), eagerEvaluation, false, true)
								&& maybeReplaceDuplicateExpression(visited, firstCondition.getOperator(), firstCondition.getLeftOperand(),
										secondCondition.getRightOperand(), firstCondition.getRightOperand(), secondCondition.getLeftOperand(), eagerEvaluation, false, false)
								&& maybeReplaceDuplicateExpression(visited, firstCondition.getOperator(), firstCondition.getRightOperand(),
										secondCondition.getLeftOperand(), firstCondition.getLeftOperand(), secondCondition.getRightOperand(), eagerEvaluation, true, true)
								&& maybeReplaceDuplicateExpression(visited, firstCondition.getOperator(), firstCondition.getRightOperand(),
										secondCondition.getRightOperand(), firstCondition.getLeftOperand(), secondCondition.getLeftOperand(), eagerEvaluation, true, false);
					}
				}

				return true;
			}

			private boolean maybeReplaceDuplicateExpression(final InfixExpression visited, final InfixExpression.Operator innerOperator,
					final Expression factor, final Expression factorDuplicate, final Expression thenExpression, final Expression elseExpression,
					final boolean eagerEvaluation, final boolean isThenExpressionFirst, boolean isElseExpressionLast) {
				if (ASTNodes.match(factor, factorDuplicate)
						&& !ASTNodes.match(thenExpression, elseExpression)
						&& !ASTSemanticMatcher.INSTANCE.matchNegative(thenExpression, elseExpression)
						&& isPassiveWithoutBreak(factor)) {
					boolean isThenExpressionPassiveWithoutBreak= isPassiveWithoutBreak(thenExpression);
					boolean isElseExpressionPassiveWithoutBreak= isPassiveWithoutBreak(elseExpression);

					if (isThenExpressionPassiveWithoutBreak
							&& isElseExpressionLast
							&& (eagerEvaluation || isElseExpressionPassiveWithoutBreak)) {
						rewriteOperations.add(new OperandFactorizationOperation(visited, innerOperator, factor, thenExpression, elseExpression, true));
						return false;
					}

					if (isElseExpressionPassiveWithoutBreak
							&& (isThenExpressionFirst || isThenExpressionPassiveWithoutBreak)) {
						rewriteOperations.add(new OperandFactorizationOperation(visited, innerOperator, factor, thenExpression, elseExpression, false));
						return false;
					}
				}

				return true;
			}

			/**
			 * True if the expression is passive, may not throw exception and is a primitive
			 * (because a wrapper may create a Null Pointer Exception).
			 *
			 * @param disturbingExpression The expression
			 * @return true if the expression is passive, may not throw exception and is a primitive
			 */
			private boolean isPassiveWithoutBreak(final Expression disturbingExpression) {
				return ASTNodes.hasType(disturbingExpression,
						boolean.class.getCanonicalName(),
						int.class.getCanonicalName(),
						long.class.getCanonicalName(),
						short.class.getCanonicalName(),
						char.class.getCanonicalName(),
						byte.class.getCanonicalName())
						&& ASTNodes.isPassiveWithoutFallingThrough(disturbingExpression);
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.OperandFactorizationCleanUp_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[0]));
	}

	@Override
	public boolean canFix(final ICompilationUnit compilationUnit, final IProblemLocation problem) {
		return false;
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit, final IProblemLocation[] problems) throws CoreException {
		return null;
	}

	private static class OperandFactorizationOperation extends CompilationUnitRewriteOperation {
		private final InfixExpression visited;
		private final InfixExpression.Operator innerOperator;
		private final Expression factor;
		private final Expression thenExpression;
		private final Expression elseExpression;
		private final boolean isFactorOnTheLeft;

		public OperandFactorizationOperation(
				final InfixExpression visited,
				final InfixExpression.Operator innerOperator,
				final Expression factor,
				final Expression thenExpression,
				final Expression elseExpression,
				final boolean isFactorOnTheLeft) {
			this.visited= visited;
			this.innerOperator= innerOperator;
			this.factor= factor;
			this.thenExpression= thenExpression;
			this.elseExpression= elseExpression;
			this.isFactorOnTheLeft= isFactorOnTheLeft;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.OperandFactorizationCleanUp_description, cuRewrite);

			InfixExpression newInnerInfixExpression= ast.newInfixExpression();
			newInnerInfixExpression.setOperator(visited.getOperator());
			newInnerInfixExpression.setLeftOperand(ASTNodes.createMoveTarget(rewrite, thenExpression));
			newInnerInfixExpression.setRightOperand(ASTNodes.createMoveTarget(rewrite, elseExpression));

			InfixExpression newMainInfixExpression= ast.newInfixExpression();
			newMainInfixExpression.setOperator(innerOperator);

			if (isFactorOnTheLeft) {
				newMainInfixExpression.setLeftOperand(ASTNodes.createMoveTarget(rewrite, factor));
				newMainInfixExpression.setRightOperand(ASTNodeFactory.parenthesizeIfNeeded(ast, newInnerInfixExpression));
			} else  {
				newMainInfixExpression.setLeftOperand(ASTNodeFactory.parenthesizeIfNeeded(ast, newInnerInfixExpression));
				newMainInfixExpression.setRightOperand(ASTNodes.createMoveTarget(rewrite, factor));
			}

			ASTNodes.replaceButKeepComment(rewrite, visited, ASTNodeFactory.parenthesizeIfNeeded(ast, newMainInfixExpression), group);
		}
	}
}
