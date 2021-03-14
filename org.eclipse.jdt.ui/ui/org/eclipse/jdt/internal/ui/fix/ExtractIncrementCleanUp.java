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
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
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
 * A fix that moves increment or decrement outside an expression:
 * <ul>
 * <li>Moves before the prefix expressions,</li>
 * <li>Moves after the postfix expressions,</li>
 * <li>Converts as postfix expressions,</li>
 * <li>Do not move increments from loop condition,</li>
 * <li>Do not cleanup several increments in the same statement.</li>
 * </ul>
 */
public class ExtractIncrementCleanUp extends AbstractMultiFix {
	public ExtractIncrementCleanUp() {
		this(Collections.emptyMap());
	}

	public ExtractIncrementCleanUp(final Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.EXTRACT_INCREMENT);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.EXTRACT_INCREMENT)) {
			return new String[] { MultiFixMessages.CodeStyleCleanUp_ExtractIncrement_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.EXTRACT_INCREMENT)) {
			return "" //$NON-NLS-1$
					+ "i++;\n" //$NON-NLS-1$
					+ "boolean isPositive = i > 0;\n"; //$NON-NLS-1$
		}

		return "boolean isPositive = ++i > 0;\n\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.EXTRACT_INCREMENT)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final Block visited) {
				ExpressionVisitor expressionVisitor= new ExpressionVisitor(visited);
				visited.accept(expressionVisitor);
				return expressionVisitor.result;
			}

			final class ExpressionVisitor extends ASTVisitor {
				private final Block startNode;
				private boolean result= true;

				public ExpressionVisitor(final Block startNode) {
					this.startNode= startNode;
				}

				@Override
				public boolean visit(final Block visited) {
					return startNode == visited;
				}

				@Override
				public boolean visit(final PrefixExpression visited) {
					if (ASTNodes.hasOperator(visited, PrefixExpression.Operator.INCREMENT, PrefixExpression.Operator.DECREMENT)) {
						return visitExpression(visited, visited.getOperand());
					}

					return true;
				}

				@Override
				public boolean visit(final PostfixExpression visited) {
					if (ASTNodes.hasOperator(visited, PostfixExpression.Operator.INCREMENT, PostfixExpression.Operator.DECREMENT)) {
						return visitExpression(visited, visited.getOperand());
					}

					return true;
				}

				public boolean visitExpression(final Expression visited, final Expression variable) {
					SimpleName variableName= ASTNodes.as(variable, SimpleName.class);

					if (result
							&& !(visited.getParent() instanceof ExpressionStatement)
							&& variableName != null
							&& variableName.resolveBinding() != null
							&& variableName.resolveBinding().getKind() == IBinding.VARIABLE
							&& ASTNodes.isLocalVariable(variableName.resolveBinding())) {
						return visitParent(visited, variable, visited);
					}

					return true;
				}

				public boolean visitParent(final Expression visited, final Expression variable, final ASTNode parent) {
					ASTNode ancestor= parent.getParent();

					if (ancestor != null) {
						switch (ancestor.getNodeType()) {
						case ASTNode.IF_STATEMENT:
							IfStatement statement= (IfStatement) ancestor;

							if (visited instanceof PrefixExpression
									&& parent.getLocationInParent() == IfStatement.EXPRESSION_PROPERTY
									&& !ASTNodes.isInElse(statement)) {
								return maybeExtractIncrement(visited, variable, statement);
							}

							return true;

						case ASTNode.LABELED_STATEMENT:
						case ASTNode.VARIABLE_DECLARATION_STATEMENT:
						case ASTNode.EXPRESSION_STATEMENT:
							return maybeExtractIncrement(visited, variable, (Statement) ancestor);

						case ASTNode.THROW_STATEMENT:
						case ASTNode.RETURN_STATEMENT:
							if (visited instanceof PrefixExpression) {
								return maybeExtractIncrement(visited, variable, (Statement) ancestor);
							}

							return true;

						case ASTNode.CONSTRUCTOR_INVOCATION:
						case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
							if (visited instanceof PostfixExpression) {
								return maybeExtractIncrement(visited, variable, (Statement) ancestor);
							}

							return true;

						case ASTNode.QUALIFIED_NAME:
						case ASTNode.SIMPLE_NAME:
						case ASTNode.FIELD_ACCESS:
						case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
						case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
						case ASTNode.ASSIGNMENT:
						case ASTNode.INSTANCEOF_EXPRESSION:
						case ASTNode.CLASS_INSTANCE_CREATION:
						case ASTNode.METHOD_INVOCATION:
						case ASTNode.SUPER_METHOD_INVOCATION:
						case ASTNode.CAST_EXPRESSION:
						case ASTNode.PARENTHESIZED_EXPRESSION:
						case ASTNode.POSTFIX_EXPRESSION:
						case ASTNode.PREFIX_EXPRESSION:
						case ASTNode.ARRAY_ACCESS:
						case ASTNode.ARRAY_CREATION:
						case ASTNode.ARRAY_INITIALIZER:
							return visitParent(visited, variable, ancestor);

						case ASTNode.INFIX_EXPRESSION:
							if (parent.getLocationInParent() == InfixExpression.LEFT_OPERAND_PROPERTY
								|| ASTNodes.hasOperator((InfixExpression) ancestor,
									InfixExpression.Operator.AND,
									InfixExpression.Operator.DIVIDE,
									InfixExpression.Operator.EQUALS,
									InfixExpression.Operator.GREATER,
									InfixExpression.Operator.GREATER_EQUALS,
									InfixExpression.Operator.LEFT_SHIFT,
									InfixExpression.Operator.LESS,
									InfixExpression.Operator.LESS_EQUALS,
									InfixExpression.Operator.MINUS,
									InfixExpression.Operator.NOT_EQUALS,
									InfixExpression.Operator.OR,
									InfixExpression.Operator.PLUS,
									InfixExpression.Operator.REMAINDER,
									InfixExpression.Operator.RIGHT_SHIFT_SIGNED,
									InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED,
									InfixExpression.Operator.TIMES,
									InfixExpression.Operator.XOR)) {
								return visitParent(visited, variable, ancestor);
							}

							return true;

						case ASTNode.CONDITIONAL_EXPRESSION:
							if (parent.getLocationInParent() == ConditionalExpression.EXPRESSION_PROPERTY) {
								return visitParent(visited, variable, ancestor);
							}

							return true;

						default:
						}
					}

					return true;
				}

				private boolean maybeExtractIncrement(final Expression visited, final Expression variable, final Statement statement) {
					SimpleName variableName= ASTNodes.as(variable, SimpleName.class);
					VarDefinitionsUsesVisitor varDefinitionsUsesVisitor;
					try {
						varDefinitionsUsesVisitor= new VarDefinitionsUsesVisitor((IVariableBinding) variableName.resolveBinding(), statement, true);
					} catch (Exception e) {
						return true;
					}

					if (varDefinitionsUsesVisitor.getWrites().isEmpty()
							&& varDefinitionsUsesVisitor.getReads().size() == 1
							&& (visited instanceof PrefixExpression || !ASTNodes.fallsThrough(statement))) {
						rewriteOperations.add(new ExtractIncrementOperation(visited, variable, statement));

						result= false;
						return false;
					}

					return true;
				}
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.CodeStyleCleanUp_ExtractIncrement_description, unit,
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

	private static class ExtractIncrementOperation extends CompilationUnitRewriteOperation {
		private final Expression visited;
		private final Expression variable;
		private final Statement statement;

		public ExtractIncrementOperation(final Expression visited, final Expression variable, final Statement statement) {
			this.visited= visited;
			this.variable= variable;
			this.statement= statement;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.CodeStyleCleanUp_ExtractIncrement_description, cuRewrite);

			ASTNodes.replaceButKeepComment(rewrite, ASTNodes.getHighestCompatibleNode(visited, ParenthesizedExpression.class), rewrite.createCopyTarget(variable), group);

			if (visited instanceof PostfixExpression) {
				Statement newAssignment= ast.newExpressionStatement(ASTNodes.createMoveTarget(rewrite, visited));

				if (ASTNodes.canHaveSiblings(statement)) {
					ListRewrite listRewrite= rewrite.getListRewrite(statement.getParent(), (ChildListPropertyDescriptor) statement.getLocationInParent());
					listRewrite.insertAfter(newAssignment, statement, group);
				} else {
					Block newBlock= ast.newBlock();
					newBlock.statements().add(ASTNodes.createMoveTarget(rewrite, statement));
					newBlock.statements().add(newAssignment);
					ASTNodes.replaceButKeepComment(rewrite, statement, newBlock, group);
				}
			} else {
				PostfixExpression newPostfixExpression= ast.newPostfixExpression();
				newPostfixExpression.setOperand(ASTNodes.createMoveTarget(rewrite, variable));

				if (ASTNodes.hasOperator((PrefixExpression) visited, PrefixExpression.Operator.INCREMENT)) {
					newPostfixExpression.setOperator(PostfixExpression.Operator.INCREMENT);
				} else {
					newPostfixExpression.setOperator(PostfixExpression.Operator.DECREMENT);
				}

				Statement newAssignment= ast.newExpressionStatement(newPostfixExpression);

				if (ASTNodes.canHaveSiblings(statement)) {
					ListRewrite listRewrite= rewrite.getListRewrite(statement.getParent(), (ChildListPropertyDescriptor) statement.getLocationInParent());
					listRewrite.insertBefore(newAssignment, statement, group);
				} else {
					Block newBlock= ast.newBlock();
					newBlock.statements().add(newAssignment);
					newBlock.statements().add(ASTNodes.createMoveTarget(rewrite, statement));
					ASTNodes.replaceButKeepComment(rewrite, statement, newBlock, group);
				}
			}
		}
	}
}
