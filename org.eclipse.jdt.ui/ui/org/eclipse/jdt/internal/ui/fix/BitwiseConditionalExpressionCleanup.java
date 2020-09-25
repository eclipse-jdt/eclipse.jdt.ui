/*******************************************************************************
 * Copyright (c) 2020 Karakun GmbH (http://www.karakun.com) and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Karsten Thoms (Karakun) - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.AbortSearchException;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * This fix changes bitwise comparison expressions where an operand is a constant and the result
 * is compared with &gt; 0.
 *
 * @see <a href="http://findbugs.sourceforge.net/bugDescriptions.html#BIT_SIGNED_CHECK">FindBugs:BIT_SIGNED_CHECK</a>
 */
public class BitwiseConditionalExpressionCleanup extends AbstractMultiFix {
	public BitwiseConditionalExpressionCleanup() {
		this(Collections.emptyMap());
	}

	public BitwiseConditionalExpressionCleanup(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.CHECK_SIGN_OF_BITWISE_OPERATION);
		Map<String, String> requiredOptions= null;
		return new CleanUpRequirements(requireAST, false, false, requiredOptions);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.CHECK_SIGN_OF_BITWISE_OPERATION)) {
			return new String[] { MultiFixMessages.CheckSignOfBitwiseOperation_description };
		}
		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.CHECK_SIGN_OF_BITWISE_OPERATION)) {
			return "if (value & CONSTANT != 0) {}\n"; //$NON-NLS-1$
		}

		return "if (value & CONSTANT > 0) {}\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.CHECK_SIGN_OF_BITWISE_OPERATION)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final InfixExpression node) {
				if (!node.hasExtendedOperands()) {
					if ((node.getOperator() == InfixExpression.Operator.GREATER && isBitwiseExpression(node.getLeftOperand()) && Long.valueOf(0).equals(ASTNodes.getIntegerLiteral(node.getRightOperand())))
							|| (node.getOperator() == InfixExpression.Operator.LESS && isBitwiseExpression(node.getRightOperand()) && Long.valueOf(0).equals(ASTNodes.getIntegerLiteral(node.getLeftOperand())))) {

						rewriteOperations.add(new RewriteInfixExpressionOperator(node));
						return false;
					}
				}

				return true;
			}

			private boolean isBitwiseExpression(final Expression bitwiseExpression) {
				if (bitwiseExpression instanceof ParenthesizedExpression) {
					return isBitwiseExpression(((ParenthesizedExpression) bitwiseExpression).getExpression());
				}

				if (!(bitwiseExpression instanceof InfixExpression)) {
					return false;
				}

				final AtomicBoolean result= new AtomicBoolean(false);
				try {
					bitwiseExpression.accept(new ASTVisitor() {
						@Override
						public boolean visit(InfixExpression node) {
							if (!ASTNodes.hasOperator(node, InfixExpression.Operator.AND, InfixExpression.Operator.OR, InfixExpression.Operator.XOR)) {
								result.set(false);
								throw new AbortSearchException();
							}

							result.set(true);
							return false;
						}});
				} catch (AbortSearchException e) {
					return false;
				}

				return result.get();
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.CheckSignOfBitwiseOperation_description, unit,
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

	private static class RewriteInfixExpressionOperator extends CompilationUnitRewriteOperation {
		private InfixExpression fExpression;

		public RewriteInfixExpressionOperator(final InfixExpression expression) {
			fExpression= expression;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();

			TextEditGroup group= createTextEditGroup(MultiFixMessages.CheckSignOfBitwiseOperation_description, cuRewrite);

			InfixExpression newInfixExpression= ast.newInfixExpression();
			newInfixExpression.setOperator(InfixExpression.Operator.NOT_EQUALS);
			newInfixExpression.setLeftOperand(ASTNodes.createMoveTarget(rewrite, fExpression.getLeftOperand()));
			newInfixExpression.setRightOperand(ASTNodes.createMoveTarget(rewrite, fExpression.getRightOperand()));
			newInfixExpression.setFlags(fExpression.getFlags());

			ASTNodes.replaceButKeepComment(rewrite, fExpression, newInfixExpression, group);
		}
	}
}
