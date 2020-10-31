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
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that reduces double negation in boolean expression:
 * <ul>
 * <li>Remove negations on both operands in an equality/difference operation,</li>
 * <li>Prefer equality/difference operation rather than negated operand.</li>
 * </ul>
 */
public class DoubleNegationCleanUp extends AbstractMultiFix implements ICleanUpFix {
	public DoubleNegationCleanUp() {
		this(Collections.emptyMap());
	}

	public DoubleNegationCleanUp(final Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.DOUBLE_NEGATION);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.DOUBLE_NEGATION)) {
			return new String[] { MultiFixMessages.DoubleNegationCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.DOUBLE_NEGATION)) {
			return "" //$NON-NLS-1$
					+ "boolean b1 = isValid == isEnabled;\n" //$NON-NLS-1$
					+ "boolean b2 = isValid ^ isEnabled;\n" //$NON-NLS-1$
					+ "boolean b3 = isValid == isEnabled;\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "boolean b1 = !isValid == !isEnabled;\n" //$NON-NLS-1$
				+ "boolean b2 = !isValid != !isEnabled;\n" //$NON-NLS-1$
				+ "boolean b3 = !isValid ^ isEnabled;\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.DOUBLE_NEGATION)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final InfixExpression visited) {
				if (ASTNodes.hasOperator(visited, InfixExpression.Operator.EQUALS, InfixExpression.Operator.NOT_EQUALS, InfixExpression.Operator.XOR)
						&& !visited.hasExtendedOperands()) {
					Expression leftExpression= visited.getLeftOperand();
					Expression rightExpression= visited.getRightOperand();

					PrefixExpression leftPrefix= ASTNodes.as(leftExpression, PrefixExpression.class);
					Expression leftNegatedExpression= null;
					if (leftPrefix != null && ASTNodes.hasOperator(leftPrefix, PrefixExpression.Operator.NOT)) {
						leftNegatedExpression= leftPrefix.getOperand();
					}

					PrefixExpression rightPrefix= ASTNodes.as(rightExpression, PrefixExpression.class);
					Expression rightNegatedExpression= null;
					if (rightPrefix != null && ASTNodes.hasOperator(rightPrefix, PrefixExpression.Operator.NOT)) {
						rightNegatedExpression= rightPrefix.getOperand();
					}

					if (leftNegatedExpression != null || rightNegatedExpression != null) {
						rewriteOperations.add(new DoubleNegationOperation(visited, leftExpression, rightExpression, leftNegatedExpression, rightNegatedExpression));
						return false;
					}
				}

				return true;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.DoubleNegationCleanUp_description, unit,
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

	private static class DoubleNegationOperation extends CompilationUnitRewriteOperation {
		private final InfixExpression visited;
		private final Expression leftExpression;
		private final Expression rightExpression;
		private final Expression leftNegatedExpression;
		private final Expression rightNegatedExpression;

		public DoubleNegationOperation(final InfixExpression visited, final Expression leftExpression, final Expression rightExpression, final Expression leftNegatedExpression, final Expression rightNegatedExpression) {
			this.visited= visited;
			this.leftExpression= leftExpression;
			this.rightExpression= rightExpression;
			this.leftNegatedExpression= leftNegatedExpression;
			this.rightNegatedExpression= rightNegatedExpression;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.DoubleNegationCleanUp_description, cuRewrite);

			InfixExpression newInfixExpression= ast.newInfixExpression();

			if (leftNegatedExpression != null) {
				newInfixExpression.setLeftOperand(ASTNodes.createMoveTarget(rewrite, leftNegatedExpression));

				if (rightNegatedExpression != null) {
					newInfixExpression.setOperator(getAppropriateOperator(visited));
					newInfixExpression.setRightOperand(ASTNodes.createMoveTarget(rewrite, rightNegatedExpression));
				} else {
					newInfixExpression.setOperator(getNegatedOperator(visited));
					newInfixExpression.setRightOperand(ASTNodes.createMoveTarget(rewrite, rightExpression));
				}
			} else {
				newInfixExpression.setLeftOperand(ASTNodes.createMoveTarget(rewrite, leftExpression));
				newInfixExpression.setOperator(getNegatedOperator(visited));
				newInfixExpression.setRightOperand(ASTNodes.createMoveTarget(rewrite, rightNegatedExpression));
			}

			ASTNodes.replaceButKeepComment(rewrite, visited, newInfixExpression, group);
		}

		private static InfixExpression.Operator getNegatedOperator(final InfixExpression expression) {
			if (ASTNodes.hasOperator(expression, InfixExpression.Operator.EQUALS)) {
				return InfixExpression.Operator.XOR;
			}

			return InfixExpression.Operator.EQUALS;
		}

		private static InfixExpression.Operator getAppropriateOperator(final InfixExpression expression) {
			if (ASTNodes.hasOperator(expression, InfixExpression.Operator.NOT_EQUALS)) {
				return InfixExpression.Operator.XOR;
			}

			return expression.getOperator();
		}
	}
}
