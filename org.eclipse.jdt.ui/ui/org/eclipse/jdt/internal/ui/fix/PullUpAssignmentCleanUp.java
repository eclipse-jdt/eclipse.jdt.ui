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
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.VarDefinitionsUsesVisitor;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that moves assignments inside an if condition above the if visited:
 * <ul>
 * <li>Check that the previous expressions in the same statement are passive,</li>
 * <li>Check that the assignment does not affect the previous expressions in the same statement.</li>
 * </ul>
 */
public class PullUpAssignmentCleanUp extends AbstractMultiFix {
	public PullUpAssignmentCleanUp() {
		this(Collections.emptyMap());
	}

	public PullUpAssignmentCleanUp(final Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.PULL_UP_ASSIGNMENT);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.PULL_UP_ASSIGNMENT)) {
			return new String[] { MultiFixMessages.CodeStyleCleanUp_PullUpAssignment_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.PULL_UP_ASSIGNMENT)) {
			return "" //$NON-NLS-1$
					+ "isRemoved = list.remove(o);\n" //$NON-NLS-1$
					+ "if (isRemoved) {}\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "if (isRemoved = list.remove(o)) {}\n\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.PULL_UP_ASSIGNMENT)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final Block visited) {
				IfWithAssignmentVisitor ifWithAssignmentVisitor= new IfWithAssignmentVisitor(visited);
				visited.accept(ifWithAssignmentVisitor);
				return ifWithAssignmentVisitor.result;
			}

			final class IfWithAssignmentVisitor extends ASTVisitor {
				private final Block startNode;
				private boolean result= true;

				public IfWithAssignmentVisitor(final Block startNode) {
					this.startNode= startNode;
				}

				@Override
				public boolean visit(final Block visited) {
					return startNode == visited;
				}

				@Override
				public boolean visit(final IfStatement visited) {
					return !result || maybePullUpExpression(visited, visited.getExpression(), new ArrayList<Expression>());
				}

				private boolean maybePullUpExpression(final IfStatement visited, final Expression expression, final List<Expression> evaluatedExpression) {
					Assignment assignment= ASTNodes.as(expression, Assignment.class);

					if (assignment != null) {
						return maybePullUpAssignment(visited, assignment, evaluatedExpression);
					}

					PrefixExpression prefixExpression= ASTNodes.as(expression, PrefixExpression.class);

					if (prefixExpression != null && ASTNodes.hasOperator(prefixExpression,
							PrefixExpression.Operator.NOT,
							PrefixExpression.Operator.COMPLEMENT,
							PrefixExpression.Operator.MINUS,
							PrefixExpression.Operator.PLUS)) {
						return maybePullUpExpression(visited, prefixExpression.getOperand(), evaluatedExpression);
					}

					InfixExpression infixExpression= ASTNodes.as(expression, InfixExpression.class);

					if (infixExpression != null) {
						List<Expression> operands= ASTNodes.allOperands(infixExpression);
						boolean isAllOperandsEvaluated= ASTNodes.hasOperator(infixExpression,
								InfixExpression.Operator.EQUALS,
								InfixExpression.Operator.NOT_EQUALS,
								InfixExpression.Operator.PLUS,
								InfixExpression.Operator.MINUS,
								InfixExpression.Operator.DIVIDE,
								InfixExpression.Operator.TIMES,
								InfixExpression.Operator.XOR,
								InfixExpression.Operator.GREATER,
								InfixExpression.Operator.GREATER_EQUALS,
								InfixExpression.Operator.LEFT_SHIFT,
								InfixExpression.Operator.LESS,
								InfixExpression.Operator.LESS_EQUALS,
								InfixExpression.Operator.REMAINDER,
								InfixExpression.Operator.RIGHT_SHIFT_SIGNED,
								InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED,
								InfixExpression.Operator.AND,
								InfixExpression.Operator.OR);

						for (Expression operand : operands) {
							if (!maybePullUpExpression(visited, operand, evaluatedExpression)) {
								return false;
							}

							if (!isAllOperandsEvaluated || !ASTNodes.isPassive(operand)) {
								break;
							}

							evaluatedExpression.add(operand);
						}
					}

					ConditionalExpression conditionalExpression= ASTNodes.as(expression, ConditionalExpression.class);

					return conditionalExpression == null || maybePullUpExpression(visited, conditionalExpression.getExpression(), evaluatedExpression);
				}

				private boolean maybePullUpAssignment(final IfStatement visited, final Assignment assignment, final List<Expression> evaluatedExpression) {
					Expression leftHandSide= ASTNodes.getUnparenthesedExpression(assignment.getLeftHandSide());

					if (!evaluatedExpression.isEmpty()) {
						Name mame= ASTNodes.as(leftHandSide, Name.class);
						FieldAccess fieldAccess= ASTNodes.as(leftHandSide, FieldAccess.class);
						SuperFieldAccess superFieldAccess= ASTNodes.as(leftHandSide, SuperFieldAccess.class);
						IVariableBinding variableBinding;

						if (mame != null) {
							IBinding binding= mame.resolveBinding();

							if (!(binding instanceof IVariableBinding)) {
								return true;
							}

							variableBinding= (IVariableBinding) binding;
						} else if (fieldAccess != null) {
							variableBinding= fieldAccess.resolveFieldBinding();
						} else if (superFieldAccess != null) {
							variableBinding= superFieldAccess.resolveFieldBinding();
						} else {
							return true;
						}

						for (Expression expression : evaluatedExpression) {
							VarDefinitionsUsesVisitor variableUseVisitor= new VarDefinitionsUsesVisitor(variableBinding,
							expression, true);

							if (!variableUseVisitor.getReads().isEmpty()) {
								return true;
							}
						}
					}

					VariableDeclarationStatement variableDeclarationStatement= ASTNodes.as(ASTNodes.getPreviousSibling(visited), VariableDeclarationStatement.class);
					VariableDeclarationFragment fragment= findFragmentIfNotUsed(variableDeclarationStatement, leftHandSide);

					if (fragment != null && (fragment.getInitializer() == null || ASTNodes.isPassive(fragment.getInitializer()))) {
						rewriteOperations.add(new MoveToDeclarationOperation(assignment, leftHandSide, fragment));
						result= false;
						return false;
					}

					if (!ASTNodes.isInElse(visited)) {
						rewriteOperations.add(new PullUpAssignmentOperation(visited, assignment, leftHandSide));

						result= false;
						return false;
					}

					return true;
				}

				private VariableDeclarationFragment findFragmentIfNotUsed(final VariableDeclarationStatement variableDeclarationStatement,
						final Expression expression) {
					VariableDeclarationFragment theFragment= null;
					IVariableBinding bindingOfPreviousVariable= null;

					if (variableDeclarationStatement != null && expression instanceof SimpleName) {
						for (VariableDeclarationFragment aFragment : (List<VariableDeclarationFragment>) variableDeclarationStatement.fragments()) {
							if (bindingOfPreviousVariable != null) {
								VarDefinitionsUsesVisitor varOccurrencesVisitor= new VarDefinitionsUsesVisitor(bindingOfPreviousVariable,
								aFragment, true);

								if (!varOccurrencesVisitor.getReads().isEmpty()) {
									return null;
								}
							} else if (ASTNodes.isSameVariable(expression, aFragment)) {
								theFragment= aFragment;
								bindingOfPreviousVariable= theFragment.resolveBinding();

								if (bindingOfPreviousVariable == null) {
									return null;
								}
							}
						}
					}

					return theFragment;
				}
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.CodeStyleCleanUp_PullUpAssignment_description, unit,
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

	private static class PullUpAssignmentOperation extends CompilationUnitRewriteOperation {
		private final IfStatement visited;
		private final Assignment assignment;
		private final Expression leftHandSide;

		public PullUpAssignmentOperation(final IfStatement visited, final Assignment assignment, final Expression leftHandSide) {
			this.visited= visited;
			this.assignment= assignment;
			this.leftHandSide= leftHandSide;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.CodeStyleCleanUp_PullUpAssignment_description, cuRewrite);

			ASTNodes.replaceButKeepComment(rewrite, ASTNodes.getHighestCompatibleNode(assignment, ParenthesizedExpression.class), rewrite.createCopyTarget(leftHandSide), group);
			Statement newAssignment= ast.newExpressionStatement(ASTNodes.createMoveTarget(rewrite, assignment));

			if (ASTNodes.canHaveSiblings(visited)) {
				ListRewrite listRewrite= rewrite.getListRewrite(visited.getParent(), (ChildListPropertyDescriptor) visited.getLocationInParent());
				listRewrite.insertBefore(newAssignment, visited, group);
			} else {
				Block newBlock= ast.newBlock();
				newBlock.statements().add(newAssignment);
				newBlock.statements().add(ASTNodes.createMoveTarget(rewrite, visited));
				ASTNodes.replaceButKeepComment(rewrite, visited, newBlock, group);
			}
		}
	}

	private static class MoveToDeclarationOperation extends CompilationUnitRewriteOperation {
		private final Assignment assignment;
		private final Expression leftHandSide;
		private final VariableDeclarationFragment fragment;

		public MoveToDeclarationOperation(final Assignment assignment, final Expression leftHandSide, final VariableDeclarationFragment fragment) {
			this.assignment= assignment;
			this.leftHandSide= leftHandSide;
			this.fragment= fragment;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.CodeStyleCleanUp_PullUpAssignment_description, cuRewrite);

			rewrite.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, ASTNodes.createMoveTarget(rewrite, assignment.getRightHandSide()), group);
			ASTNodes.replaceButKeepComment(rewrite, ASTNodes.getHighestCompatibleNode(assignment, ParenthesizedExpression.class), ASTNodes.createMoveTarget(rewrite, leftHandSide), group);
		}
	}
}
