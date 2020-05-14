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
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

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
					final PrefixExpression pe= (PrefixExpression) operand;

					if (ASTNodes.hasOperator(pe, PrefixExpression.Operator.NOT)) {
						rewriteOperations.add(new RemoveDoubleNegationOperation(node, pe.getOperand()));
						secondNotOperator= pe;
						return true;
					}
				} else if (operand instanceof InfixExpression) {
					final InfixExpression ie= (InfixExpression) operand;
					final InfixExpression.Operator reverseOp= ASTNodes.oppositeInfixOperator(ie.getOperator());

					if (reverseOp != null) {
						rewriteOperations.add(new PushDownNegationInInfixExpressionOperation(node, ie, reverseOp));
						return false;
					}
				} else {
					final Boolean constant= ASTNodes.getBooleanLiteral(operand);

					if (constant != null) {
						rewriteOperations.add(new ReverseBooleanConstantOperation(node, !constant.booleanValue()));
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
		private Expression replacement;

		private ReplacementOperation nextOperation;

		public RemoveDoubleNegationOperation(ASTNode node, Expression replacement) {
			this.setNode(node);
			this.replacement= replacement;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			Expression copyOfReplacement= (Expression) rewrite.createCopyTarget(this.replacement);

			// if next operation has been replaced above by a copy, update the target node to change
			if (nextOperation != null) {
				nextOperation.setNode(copyOfReplacement);
			}

			rewrite.replace(this.getNode(), copyOfReplacement, null);
		}

		public void setNextOperation(ReplacementOperation nextOperation) {
			this.nextOperation= nextOperation;
		}

		public Expression getReplacementExpression() {
			return this.replacement;
		}
	}

	private static class ReverseBooleanConstantOperation extends ReplacementOperation {
		private boolean replacement;

		public ReverseBooleanConstantOperation(ASTNode node, boolean replacement) {
			this.setNode(node);
			this.replacement= replacement;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			Expression copyOfReplacement= ast.newBooleanLiteral(this.replacement);

			rewrite.replace(this.getNode(), copyOfReplacement, null);
		}
	}

	private static class PushDownNegationInInfixExpressionOperation extends ReplacementOperation {
		private InfixExpression infixExpression;

		private final Operator reverseOp;

		public PushDownNegationInInfixExpressionOperation(ASTNode node, InfixExpression infixExpression, Operator reverseOp) {
			this.setNode(node);
			this.infixExpression= infixExpression;
			this.reverseOp= reverseOp;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			ParenthesizedExpression parenthesizedExpression= doRewriteAST(rewrite, ast, infixExpression, reverseOp);

			rewrite.replace(this.getNode(), parenthesizedExpression, null);
		}

		private ParenthesizedExpression doRewriteAST(ASTRewrite rewrite, AST ast, InfixExpression pInfixExpression, Operator pReverseOp) {
			List<Expression> allOperands= new ArrayList<>(ASTNodes.allOperands(pInfixExpression));

			if (ASTNodes.hasOperator(pInfixExpression, InfixExpression.Operator.CONDITIONAL_AND, InfixExpression.Operator.CONDITIONAL_OR, InfixExpression.Operator.AND,
					InfixExpression.Operator.OR)) {
				for (ListIterator<Expression> it= allOperands.listIterator(); it.hasNext();) {
					final Expression anOperand= it.next();
					final Expression oppositeExpression= getCopyOfOppositeExpression(rewrite, ast, anOperand);

					if (oppositeExpression != null) {
						it.set(oppositeExpression);
					} else {
						PrefixExpression prefixExpression= ast.newPrefixExpression();
						prefixExpression.setOperator(PrefixExpression.Operator.NOT);
						if (anOperand instanceof InstanceofExpression) {
							ParenthesizedExpression parenExpression= ast.newParenthesizedExpression();
							parenExpression.setExpression((Expression) rewrite.createCopyTarget(anOperand));
							prefixExpression.setOperand(parenExpression);
						} else {
							prefixExpression.setOperand((Expression) rewrite.createCopyTarget(anOperand));
						}

						it.set(prefixExpression);
					}
				}
			} else {
				for (ListIterator<Expression> it= allOperands.listIterator(); it.hasNext();) {
					it.set((Expression) rewrite.createCopyTarget(it.next()));
				}
			}

			InfixExpression newIe= ast.newInfixExpression();
			List<Expression> copyOfAllOperands= new ArrayList<>(allOperands);
			newIe.setOperator(pReverseOp);
			newIe.setLeftOperand(copyOfAllOperands.remove(0));
			newIe.setRightOperand(copyOfAllOperands.remove(0));
			newIe.extendedOperands().addAll(copyOfAllOperands);

			ParenthesizedExpression parenthesizedExpression= ast.newParenthesizedExpression();
			parenthesizedExpression.setExpression(newIe);
			return parenthesizedExpression;
		}

		private Expression getCopyOfOppositeExpression(ASTRewrite rewrite, AST ast, final Expression operand) {
			if (operand instanceof ParenthesizedExpression) {
				return getCopyOfOppositeExpression(rewrite, ast, ((ParenthesizedExpression) operand).getExpression());
			}

			if (operand instanceof PrefixExpression) {
				final PrefixExpression pe= (PrefixExpression) operand;

				if (ASTNodes.hasOperator(pe, PrefixExpression.Operator.NOT)) {
					Expression otherOperand= pe.getOperand();
					PrefixExpression otherPe= ASTNodes.as(otherOperand, PrefixExpression.class);

					if (otherPe != null && ASTNodes.hasOperator(otherPe, PrefixExpression.Operator.NOT)) {
						return getCopyOfOppositeExpression(rewrite, ast, otherPe.getOperand());
					}

					return (Expression) rewrite.createCopyTarget(otherOperand);
				}
			} else if (operand instanceof InfixExpression) {
				final InfixExpression ie= (InfixExpression) operand;
				final InfixExpression.Operator aReverseOp= ASTNodes.oppositeInfixOperator(ie.getOperator());

				if (aReverseOp != null) {
					return doRewriteAST(rewrite, ast, ie, aReverseOp);
				}
			} else {
				final Boolean constant= ASTNodes.getBooleanLiteral(operand);

				if (constant != null) {
					return ast.newBooleanLiteral(!constant.booleanValue());
				}
			}

			return null;
		}
	}
}
