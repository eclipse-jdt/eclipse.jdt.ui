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
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

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
 * A fix that replaces <code>(X && !Y) || (!X && Y)</code> by <code>X ^ Y</code>:
 * <ul>
 * <li>Only works on boolean,</li>
 * <li>Works with lazy or eager operators,</li>
 * <li>The operands should be passive,</li>
 * <li>It does not matter an operand is on the left or right.</li>
 * </ul>
 */
public class StrictlyEqualOrDifferentCleanUp extends AbstractMultiFix implements ICleanUpFix {
	public StrictlyEqualOrDifferentCleanUp() {
		this(Collections.emptyMap());
	}

	public StrictlyEqualOrDifferentCleanUp(final Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.STRICTLY_EQUAL_OR_DIFFERENT);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.STRICTLY_EQUAL_OR_DIFFERENT)) {
			return new String[] { MultiFixMessages.StrictlyEqualOrDifferentCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.STRICTLY_EQUAL_OR_DIFFERENT)) {
			return "" //$NON-NLS-1$
					+ "boolean newBoolean1 = isValid == (i > 0);\n" //$NON-NLS-1$
					+ "boolean newBoolean2 = isValid ^ isEnabled;\n" //$NON-NLS-1$
					+ "boolean newBoolean3 = isActive == (0 <= i);\n" //$NON-NLS-1$
					+ "boolean newBoolean4 = isActive ^ isEnabled;\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "boolean newBoolean1 = isValid && (i > 0) || !isValid && (i <= 0);\n" //$NON-NLS-1$
				+ "boolean newBoolean2 = !isValid && isEnabled || isValid && !isEnabled;\n" //$NON-NLS-1$
				+ "boolean newBoolean3 = isActive ? (0 <= i) : (i < 0);\n" //$NON-NLS-1$
				+ "boolean newBoolean4 = !isActive ? isEnabled : !isEnabled;\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.STRICTLY_EQUAL_OR_DIFFERENT)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final InfixExpression visited) {
				if (ASTNodes.hasOperator(visited, InfixExpression.Operator.CONDITIONAL_OR, InfixExpression.Operator.OR)
						&& !visited.hasExtendedOperands()
						&& ASTNodes.hasType(visited, boolean.class.getCanonicalName())) {
					InfixExpression firstCondition= ASTNodes.as(visited.getLeftOperand(), InfixExpression.class);
					InfixExpression secondCondition= ASTNodes.as(visited.getRightOperand(), InfixExpression.class);

					if (firstCondition != null
							&& secondCondition != null
							&& !firstCondition.hasExtendedOperands()
							&& !secondCondition.hasExtendedOperands()
							&& ASTNodes.hasOperator(firstCondition, InfixExpression.Operator.CONDITIONAL_AND, InfixExpression.Operator.AND)
							&& ASTNodes.hasOperator(secondCondition, InfixExpression.Operator.CONDITIONAL_AND, InfixExpression.Operator.AND)
							&& ASTNodes.isPassive(firstCondition.getLeftOperand())
							&& ASTNodes.isPassive(firstCondition.getRightOperand())
							&& ASTNodes.isPassive(secondCondition.getLeftOperand())
							&& ASTNodes.isPassive(secondCondition.getRightOperand())) {
						return maybeReplaceDuplicateExpression(visited, firstCondition.getLeftOperand(), secondCondition.getLeftOperand(),
								firstCondition.getRightOperand(), secondCondition.getRightOperand())
								&& maybeReplaceDuplicateExpression(visited, firstCondition.getLeftOperand(), secondCondition.getRightOperand(),
										firstCondition.getRightOperand(), secondCondition.getLeftOperand());
					}
				}

				return true;
			}

			private boolean maybeReplaceDuplicateExpression(final InfixExpression visited, final Expression firstExpression,
					final Expression firstNegatedExpression, final Expression secondExpression, final Expression secondNegatedExpression) {
				if (ASTSemanticMatcher.INSTANCE.matchNegative(firstExpression, firstNegatedExpression)
						&& ASTSemanticMatcher.INSTANCE.matchNegative(secondExpression, secondNegatedExpression)) {
					AtomicBoolean isFirstExpressionPositive= new AtomicBoolean();
					AtomicBoolean isSecondExpressionPositive= new AtomicBoolean();

					Expression firstBasicExpression= getBasisExpression(firstExpression, isFirstExpressionPositive);
					Expression secondBasicExpression= getBasisExpression(secondExpression, isSecondExpressionPositive);

					rewriteOperations.add(new StrictlyEqualOrDifferentOperation(visited, firstBasicExpression, secondBasicExpression, isFirstExpressionPositive.get() == isSecondExpressionPositive.get()));
					return false;
				}

				return true;
			}

			@Override
			public boolean visit(final ConditionalExpression visited) {
				if (ASTNodes.hasType(visited.getThenExpression(), boolean.class.getCanonicalName())
						&& ASTNodes.hasType(visited.getElseExpression(), boolean.class.getCanonicalName())
						&& ASTNodes.isPassive(visited.getThenExpression())
						&& ASTNodes.isPassive(visited.getElseExpression())
						&& ASTSemanticMatcher.INSTANCE.matchNegative(visited.getThenExpression(), visited.getElseExpression())) {
					AtomicBoolean isFirstExpressionPositive= new AtomicBoolean();
					AtomicBoolean isSecondExpressionPositive= new AtomicBoolean();

					Expression firstBasicExpression= getBasisExpression(visited.getExpression(), isFirstExpressionPositive);
					Expression secondBasicExpression= getBasisExpression(visited.getThenExpression(), isSecondExpressionPositive);

					rewriteOperations.add(new StrictlyEqualOrDifferentOperation(visited, firstBasicExpression, secondBasicExpression, isFirstExpressionPositive.get() == isSecondExpressionPositive.get()));
					return false;
				}

				return true;
			}

			private Expression getBasisExpression(final Expression originalExpression, final AtomicBoolean isExpressionPositive) {
				PrefixExpression negateExpression= ASTNodes.as(originalExpression, PrefixExpression.class);

				if (ASTNodes.hasOperator(negateExpression, PrefixExpression.Operator.NOT)) {
					isExpressionPositive.lazySet(false);
					return negateExpression.getOperand();
				}

				isExpressionPositive.lazySet(true);
				return originalExpression;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.StrictlyEqualOrDifferentCleanUp_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[0]));
	}

	@Override
	public CompilationUnitChange createChange(final IProgressMonitor progressMonitor) throws CoreException {
		return null;
	}

	@Override
	public boolean canFix(final ICompilationUnit compilationUnit, final IProblemLocation problem) {
		return false;
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit, final IProblemLocation[] problems) throws CoreException {
		return null;
	}

	private static class StrictlyEqualOrDifferentOperation extends CompilationUnitRewriteOperation {
		private final Expression visited;
		private final Expression firstExpression;
		private final Expression secondExpression;
		private final boolean isEquality;

		public StrictlyEqualOrDifferentOperation(final Expression visited, final Expression firstExpression, final Expression secondExpression, final boolean isEquality) {
			this.visited= visited;
			this.firstExpression= firstExpression;
			this.secondExpression= secondExpression;
			this.isEquality= isEquality;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.StrictlyEqualOrDifferentCleanUp_description, cuRewrite);

			InfixExpression newInfixExpression= ast.newInfixExpression();
			newInfixExpression.setLeftOperand(ASTNodes.createMoveTarget(rewrite, firstExpression));
			newInfixExpression.setRightOperand(ASTNodes.createMoveTarget(rewrite, secondExpression));

			if (isEquality) {
				newInfixExpression.setOperator(InfixExpression.Operator.EQUALS);
			} else {
				newInfixExpression.setOperator(InfixExpression.Operator.XOR);
			}

			ASTNodes.replaceButKeepComment(rewrite, visited, newInfixExpression, group);
		}
	}
}
