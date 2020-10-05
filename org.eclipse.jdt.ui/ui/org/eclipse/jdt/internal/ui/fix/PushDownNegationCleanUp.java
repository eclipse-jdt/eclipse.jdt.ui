/*******************************************************************************
 * Copyright (c) 2019 Fabrice TIERCELIN and others.
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
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
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
 * A fix that pushes down the negation into a boolean expression:
 * <ul>
 * <li>Removes double negations,</li>
 * <li>Uses opposite boolean constants,</li>
 * <li>Reverses arithmetic expressions.</li>
 * </ul>
 */
public class PushDownNegationCleanUp extends AbstractMultiFix {
	public PushDownNegationCleanUp() {
		this(Collections.emptyMap());
	}

	public PushDownNegationCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.PUSH_DOWN_NEGATION);
		Map<String, String> requiredOptions= null;
		return new CleanUpRequirements(requireAST, false, false, requiredOptions);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.PUSH_DOWN_NEGATION)) {
			return new String[] { MultiFixMessages.PushDownNegationCleanup_description };
		}
		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();
		if (isEnabled(CleanUpConstants.PUSH_DOWN_NEGATION)) {
			bld.append("boolean b = (myInt <= 0);\n"); //$NON-NLS-1$
			bld.append("boolean b2 = (!isEnabled && !isValid);\n"); //$NON-NLS-1$
		} else {
			bld.append("boolean b = !(myInt > 0);\n"); //$NON-NLS-1$
			bld.append("boolean b2 = !(isEnabled || isValid);\n"); //$NON-NLS-1$
		}

		return bld.toString();
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.PUSH_DOWN_NEGATION)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			PrefixExpression secondNotOperator= null;
			@Override
			public boolean visit(PrefixExpression node) {
				if (!ASTNodes.hasOperator(node, PrefixExpression.Operator.NOT)) {
					return true;
				}

				if (node.subtreeMatch(new ASTMatcher(), secondNotOperator)) {
					// already processed as part of RemoveDoubleNegationOperation
					return true;
				}

				return pushDown(node, node.getOperand());
			}

			private boolean pushDown(final PrefixExpression node, Expression operand) {
				operand= ASTNodes.getUnparenthesedExpression(operand);

				if (operand instanceof PrefixExpression) {
					final PrefixExpression prefixExpression= (PrefixExpression) operand;

					if (ASTNodes.hasOperator(prefixExpression, PrefixExpression.Operator.NOT)) {
						rewriteOperations.add(new RemoveDoubleNegationOperation(node, prefixExpression.getOperand()));
						secondNotOperator= prefixExpression;
						return true;
					}
				} else if (operand instanceof InfixExpression) {
					final InfixExpression infixExpression= (InfixExpression) operand;
					final InfixExpression.Operator negatedOperator= ASTNodes.negatedInfixOperator(infixExpression.getOperator());

					if (negatedOperator != null) {
						rewriteOperations.add(new PushDownNegationInExpressionOperation(node, infixExpression));
						return false;
					}
				} else {
					final Boolean constant= ASTNodes.getBooleanLiteral(operand);

					if (constant != null) {
						rewriteOperations.add(new PushDownNegationInExpressionOperation(node, operand));
						return false;
					}
				}

				return true;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		RemoveDoubleNegationOperation lastDoubleNegation= null;
		for (CompilationUnitRewriteOperation op : rewriteOperations) {
			if (op instanceof ReplacementOperation) {
				ReplacementOperation chainedOp= (ReplacementOperation) op;
				if (lastDoubleNegation != null && chainedOp.getNode().subtreeMatch(new ASTMatcher(), lastDoubleNegation.getReplacementExpression())) {
					lastDoubleNegation.setNextOperation(chainedOp);
				}
				if (op instanceof RemoveDoubleNegationOperation) {
					lastDoubleNegation= (RemoveDoubleNegationOperation) op;
				}
			}
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.PushDownNegationCleanup_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[rewriteOperations.size()]));
	}

	@Override
	public boolean canFix(ICompilationUnit compilationUnit, IProblemLocation problem) {
		return false;
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit, IProblemLocation[] problems) throws CoreException {
		return null;
	}

	private abstract static class ReplacementOperation extends CompilationUnitRewriteOperation {
		private ASTNode node;

		public void setNode(ASTNode node) {
			this.node= node;
		}

		public ASTNode getNode() {
			return this.node;
		}
	}

	private static class RemoveDoubleNegationOperation extends ReplacementOperation {
		private final Expression replacement;
		private ReplacementOperation nextOperation;

		public RemoveDoubleNegationOperation(final ASTNode node, final Expression replacement) {
			setNode(node);
			this.replacement= replacement;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			Expression copyOfReplacement= ASTNodes.createMoveTarget(rewrite, replacement);
			TextEditGroup group= createTextEditGroup(MultiFixMessages.PushDownNegationCleanup_description, cuRewrite);

			// if next operation has been replaced above by a copy, update the target node to change
			if (nextOperation != null) {
				nextOperation.setNode(copyOfReplacement);
			}

			ASTNodes.replaceButKeepComment(rewrite, getNode(), copyOfReplacement, group);
		}

		public void setNextOperation(ReplacementOperation nextOperation) {
			this.nextOperation= nextOperation;
		}

		public Expression getReplacementExpression() {
			return this.replacement;
		}
	}

	public static class PushDownNegationInExpressionOperation extends ReplacementOperation {
		private final Expression expression;

		public PushDownNegationInExpressionOperation(final ASTNode node, final Expression expression) {
			setNode(node);
			this.expression= expression;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.PushDownNegationCleanup_description, cuRewrite);
			Expression negatedExpression= ASTNodeFactory.negate(ast, rewrite, expression, true);

			ASTNodes.replaceButKeepComment(rewrite, getNode(), negatedExpression, group);
		}
	}
}
