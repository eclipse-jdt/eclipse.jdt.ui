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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

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
 * A fix that replaces <code>(X && Y) || (!X && Z)</code> by <code>X ? Y : Z</code>:
 * <ul>
 * <li>The operands must be passive and boolean.</li>
 * </ul>
 */
public class TernaryOperatorCleanUp extends AbstractMultiFix {
	public TernaryOperatorCleanUp() {
		this(Collections.emptyMap());
	}

	public TernaryOperatorCleanUp(final Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.TERNARY_OPERATOR);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.TERNARY_OPERATOR)) {
			return new String[] { MultiFixMessages.TernaryOperatorCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.TERNARY_OPERATOR)) {
			return "boolean result = ((0 < i) : isValid ? isEnabled);\n"; //$NON-NLS-1$
		}

		return "boolean result = (0 < i) && isValid || (i <= 0) && isEnabled;\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.TERNARY_OPERATOR)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final InfixExpression visited) {
				if (ASTNodes.hasOperator(visited, InfixExpression.Operator.CONDITIONAL_OR, InfixExpression.Operator.OR)) {
					List<Expression> operands= ASTNodes.allOperands(visited);

					for (int i= 1; i < operands.size(); i++) {
						List<Expression> previousOperands= operands.subList(0, i - 1);
						InfixExpression firstCondition= ASTNodes.as(operands.get(i - 1), InfixExpression.class);
						InfixExpression secondCondition= ASTNodes.as(operands.get(i), InfixExpression.class);
						List<Expression> nextOperands= operands.subList(i + 1, operands.size());

						if (firstCondition != null
								&& secondCondition != null
								&& !firstCondition.hasExtendedOperands()
								&& !secondCondition.hasExtendedOperands()
								&& ASTNodes.hasOperator(firstCondition, InfixExpression.Operator.CONDITIONAL_AND, InfixExpression.Operator.AND)
								&& ASTNodes.hasOperator(secondCondition, InfixExpression.Operator.CONDITIONAL_AND, InfixExpression.Operator.AND)
								&& isBooleanAndPassive(firstCondition.getLeftOperand())
								&& isBooleanAndPassive(firstCondition.getRightOperand())
								&& isBooleanAndPassive(secondCondition.getLeftOperand())
								&& isBooleanAndPassive(secondCondition.getRightOperand())) {
							if (!maybeReplaceDuplicateExpression(visited, firstCondition.getLeftOperand(), secondCondition.getLeftOperand(),
									firstCondition.getRightOperand(), secondCondition.getRightOperand(),
									previousOperands, nextOperands)
									|| !maybeReplaceDuplicateExpression(visited, firstCondition.getLeftOperand(), secondCondition.getRightOperand(),
											firstCondition.getRightOperand(), secondCondition.getLeftOperand(),
											previousOperands, nextOperands)
									|| !maybeReplaceDuplicateExpression(visited, firstCondition.getRightOperand(), secondCondition.getLeftOperand(),
											firstCondition.getLeftOperand(), secondCondition.getRightOperand(),
											previousOperands, nextOperands)
									|| !maybeReplaceDuplicateExpression(visited, firstCondition.getRightOperand(), secondCondition.getRightOperand(),
											firstCondition.getLeftOperand(), secondCondition.getLeftOperand(),
											previousOperands, nextOperands)) {
								return false;
							}
						}
					}
				}

				return true;
			}

			private boolean isBooleanAndPassive(final Expression expression) {
				return ASTNodes.isPrimitive(expression, boolean.class.getSimpleName()) && ASTNodes.isPassive(expression);
			}

			private boolean maybeReplaceDuplicateExpression(final InfixExpression visited, final Expression oneCondition,
					final Expression oppositeCondition, final Expression oneExpression, final Expression oppositeExpression,
					final List<Expression> previousOperands, final List<Expression> nextOperands) {
				if (ASTSemanticMatcher.INSTANCE.matchNegative(oneCondition, oppositeCondition)
						&& !ASTNodes.match(oneExpression, oppositeExpression)
						&& !ASTSemanticMatcher.INSTANCE.matchNegative(oneExpression, oppositeExpression)) {
					rewriteOperations.add(new TernaryOperatorOperation(visited, oneCondition, oneExpression, oppositeExpression, previousOperands,
							nextOperands));
					return false;
				}

				return true;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.TernaryOperatorCleanUp_description, unit,
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

	private static class TernaryOperatorOperation extends CompilationUnitRewriteOperation {
		private final InfixExpression visited;
		private final Expression oneCondition;
		private final Expression oneExpression;
		private final Expression alternateExpression;
		private final List<Expression> previousOperands;
		private final List<Expression> nextOperands;

		public TernaryOperatorOperation(
				final InfixExpression visited,
				final Expression oneCondition,
				final Expression oneExpression,
				final Expression alternateExpression,
				final List<Expression> previousOperands,
				final List<Expression> nextOperands) {
			this.visited= visited;
			this.oneCondition= oneCondition;
			this.oneExpression= oneExpression;
			this.alternateExpression= alternateExpression;
			this.previousOperands= previousOperands;
			this.nextOperands= nextOperands;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.TernaryOperatorCleanUp_description, cuRewrite);

			PrefixExpression negateExpression= ASTNodes.as(oneCondition, PrefixExpression.class);

			Expression basisExpression;
			Expression thenExpression;
			Expression elseExpression;
			if (ASTNodes.hasOperator(negateExpression, PrefixExpression.Operator.NOT)) {
				basisExpression= negateExpression.getOperand();
				thenExpression= alternateExpression;
				elseExpression= oneExpression;
			} else {
				basisExpression= oneCondition;
				thenExpression= oneExpression;
				elseExpression= alternateExpression;
			}

			ConditionalExpression newConditionalExpression= ast.newConditionalExpression();
			newConditionalExpression.setExpression(ASTNodes.createMoveTarget(rewrite, basisExpression));
			newConditionalExpression.setThenExpression(ASTNodes.createMoveTarget(rewrite, thenExpression));
			newConditionalExpression.setElseExpression(ASTNodes.createMoveTarget(rewrite, elseExpression));

			ParenthesizedExpression newTernaryExpression= ast.newParenthesizedExpression();
			newTernaryExpression.setExpression(newConditionalExpression);

			if (previousOperands.isEmpty() && nextOperands.isEmpty()) {
				ASTNodes.replaceButKeepComment(rewrite, visited, newTernaryExpression, group);
			} else {
				List<Expression> newOperands= ASTNodes.createMoveTarget(rewrite, previousOperands);
				newOperands.add(newTernaryExpression);
				newOperands.addAll(ASTNodes.createMoveTarget(rewrite, nextOperands));

				InfixExpression newInfixExpression= ast.newInfixExpression();
				newInfixExpression.setOperator(visited.getOperator());
				newInfixExpression.setLeftOperand(newOperands.remove(0));
				newInfixExpression.setRightOperand(newOperands.remove(0));
				newInfixExpression.extendedOperands().addAll(newOperands);
				ASTNodes.replaceButKeepComment(rewrite, visited, newInfixExpression, group);
			}
		}
	}
}
