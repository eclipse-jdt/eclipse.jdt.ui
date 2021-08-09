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
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.CreationReference;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodReference;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperMethodReference;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeMethodReference;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.VarDefinitionsUsesVisitor;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class DoWhileRatherThanWhileFixCore extends CompilationUnitRewriteOperationsFixCore {
	public static final class DoWhileRatherThanWhileFinder extends ASTVisitor {
		private List<CompilationUnitRewriteOperation> fResult;

		public DoWhileRatherThanWhileFinder(List<CompilationUnitRewriteOperation> ops) {
			fResult= ops;
		}

		@Override
		public boolean visit(final WhileStatement visited) {
			if (ASTNodes.isPassiveWithoutFallingThrough(visited.getExpression()) && Boolean.TRUE.equals(peremptoryValue(visited, visited.getExpression()))) {
				fResult.add(new DoWhileRatherThanWhileOperation(visited));
				return false;
			}

			return true;
		}

		private Object peremptoryValue(final ASTNode visited, final Expression condition) {
			Object constantCondition= condition.resolveConstantExpressionValue();

			if (constantCondition != null) {
				return constantCondition;
			}

			Long integerLiteral= ASTNodes.getIntegerLiteral(condition);

			if (integerLiteral != null) {
				return integerLiteral;
			}

			SimpleName variable= ASTNodes.as(condition, SimpleName.class);

			if (variable != null
					&& variable.resolveBinding() != null
					&& variable.resolveBinding().getKind() == IBinding.VARIABLE) {
				List<ASTNode> precedingStatements= getPrecedingCode(visited);

				Collections.reverse(precedingStatements);

				for (ASTNode precedingStatement : precedingStatements) {
					if (isConditionalCode(precedingStatement)) {
						return null;
					}

					VarDefinitionsUsesVisitor visitor= new VarDefinitionsUsesVisitor((IVariableBinding) variable.resolveBinding(), precedingStatement, true);

					if (visitor.getWrites().size() > 1) {
						return null;
					}

					for (SimpleName astNode : visitor.getReads()) {
						ASTNode parent= astNode.getParent();

						while (parent instanceof ParenthesizedExpression) {
							parent= astNode.getParent();
						}

						if (parent instanceof PrefixExpression && ASTNodes.hasOperator((PrefixExpression) parent, PrefixExpression.Operator.INCREMENT, PrefixExpression.Operator.DECREMENT)
								|| parent instanceof PostfixExpression && ASTNodes.hasOperator((PostfixExpression) parent, PostfixExpression.Operator.INCREMENT, PostfixExpression.Operator.DECREMENT)) {
							return null;
						}
					}

					if (!visitor.getWrites().isEmpty()) {
						SimpleName write= visitor.getWrites().get(0);
						ASTNode parent= write;

						while (parent != precedingStatement) {
							if (isConditionalCode(parent)) {
								return null;
							}

							parent= parent.getParent();
						}

						switch (write.getParent().getNodeType()) {
						case ASTNode.ASSIGNMENT:
							Assignment assignment= (Assignment) write.getParent();

							if (ASTNodes.hasOperator(assignment, Assignment.Operator.ASSIGN)) {
								return peremptoryValue(precedingStatement, assignment.getRightHandSide());
							}

							break;

						case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
							VariableDeclarationFragment fragment= (VariableDeclarationFragment) write.getParent();

							if (fragment.getInitializer() != null) {
								return peremptoryValue(precedingStatement, fragment.getInitializer());
							}

							break;

						case ASTNode.SINGLE_VARIABLE_DECLARATION:
							SingleVariableDeclaration singleVariableDeclaration= (SingleVariableDeclaration) write.getParent();

							if (singleVariableDeclaration.getInitializer() != null) {
								return peremptoryValue(precedingStatement, singleVariableDeclaration.getInitializer());
							}

							break;

						default:
							break;
						}

						return null;
					}
				}

				return null;
			}

			InfixExpression infixExpression= ASTNodes.as(condition, InfixExpression.class);

			if (infixExpression != null) {
				if (!infixExpression.hasExtendedOperands()
						&& ASTNodes.hasOperator(infixExpression, InfixExpression.Operator.EQUALS,
								InfixExpression.Operator.NOT_EQUALS,
								InfixExpression.Operator.GREATER,
								InfixExpression.Operator.GREATER_EQUALS,
								InfixExpression.Operator.LESS,
								InfixExpression.Operator.LESS_EQUALS)) {
					Object leftOperand= peremptoryValue(visited, infixExpression.getLeftOperand());
					Object rightOperand= peremptoryValue(visited, infixExpression.getRightOperand());

					if (leftOperand instanceof Number && rightOperand instanceof Number) {
						Number leftNumber= (Number) leftOperand;
						Number rightNumber= (Number) rightOperand;

						if (ASTNodes.hasOperator(infixExpression, InfixExpression.Operator.EQUALS)) {
							return leftNumber.longValue() == rightNumber.longValue();
						}

						if (ASTNodes.hasOperator(infixExpression, InfixExpression.Operator.NOT_EQUALS)) {
							return leftNumber.longValue() != rightNumber.longValue();
						}

						if (ASTNodes.hasOperator(infixExpression, InfixExpression.Operator.GREATER)) {
							return leftNumber.longValue() > rightNumber.longValue();
						}

						if (ASTNodes.hasOperator(infixExpression, InfixExpression.Operator.GREATER_EQUALS)) {
							return leftNumber.longValue() >= rightNumber.longValue();
						}

						if (ASTNodes.hasOperator(infixExpression, InfixExpression.Operator.LESS)) {
							return leftNumber.longValue() < rightNumber.longValue();
						}

						if (ASTNodes.hasOperator(infixExpression, InfixExpression.Operator.LESS_EQUALS)) {
							return leftNumber.longValue() <= rightNumber.longValue();
						}
					}

					return null;
				}

				if (ASTNodes.hasOperator(infixExpression, InfixExpression.Operator.CONDITIONAL_AND,
								InfixExpression.Operator.AND)) {
					for (Expression operand : ASTNodes.allOperands(infixExpression)) {
						final Object hasAlwaysValue= peremptoryValue(visited, operand);

						if (!Boolean.TRUE.equals(hasAlwaysValue)) {
							return hasAlwaysValue;
						}
					}

					return Boolean.TRUE;
				}

				if (ASTNodes.hasOperator(infixExpression, InfixExpression.Operator.CONDITIONAL_OR,
								InfixExpression.Operator.OR)) {
					for (Expression operand : ASTNodes.allOperands(infixExpression)) {
						final Object hasAlwaysValue= peremptoryValue(visited, operand);

						if (!Boolean.FALSE.equals(hasAlwaysValue)) {
							return hasAlwaysValue;
						}
					}

					return Boolean.FALSE;
				}
			}

			return Boolean.FALSE;
		}

		private boolean isConditionalCode(final ASTNode expression) {
			return expression == null
					|| expression instanceof IfStatement
					|| expression instanceof ConditionalExpression
					|| expression instanceof EnhancedForStatement
					|| expression instanceof SwitchStatement
					|| expression instanceof WhileStatement
					|| expression instanceof ForStatement
					|| expression instanceof DoStatement
					|| expression instanceof AbstractTypeDeclaration
					|| expression instanceof LambdaExpression
					|| expression instanceof MethodReference
					|| expression instanceof SuperMethodReference
					|| expression instanceof CreationReference
					|| expression instanceof TypeMethodReference
					|| expression instanceof SuperMethodReference;
		}

		@SuppressWarnings({ "deprecation" })
		private List<ASTNode> getPrecedingCode(final ASTNode node) {
			Statement statement= null;

			if (node instanceof Statement) {
				statement= (Statement) node;
			} else {
				statement= ASTNodes.getTypedAncestor(node, Statement.class);
			}

			if (statement == null) {
				return new ArrayList<>();
			}

			List<ASTNode> precedingStatements= new ArrayList<>(ASTNodes.getPreviousSiblings(statement));
			ASTNode parent= statement.getParent();

			if (parent instanceof Block) {
				precedingStatements.addAll(0, getPrecedingCode(parent));
				return precedingStatements;
			}

			if (parent instanceof IfStatement) {
				precedingStatements.add(0, ((IfStatement) parent).getExpression());
				precedingStatements.addAll(0, getPrecedingCode(parent));
			}

			if (parent instanceof CatchClause) {
				TryStatement tryStatement= (TryStatement) parent.getParent();
				precedingStatements.addAll(0, ASTNodes.asList(tryStatement.getBody()));

				if (statement.getParent().getLocationInParent() != TryStatement.RESOURCES_PROPERTY) {
					precedingStatements.addAll(0, tryStatement.resources());
				}

				precedingStatements.addAll(0, getPrecedingCode(tryStatement));
			}

			if (parent instanceof TryStatement) {
				if (statement.getLocationInParent() == TryStatement.FINALLY_PROPERTY) {
					return precedingStatements;
				}

				if (statement.getLocationInParent() != TryStatement.RESOURCES2_PROPERTY) {
					precedingStatements.addAll(0, ((TryStatement) parent).resources());
				}

				precedingStatements.addAll(0, getPrecedingCode(parent));
			}

			return precedingStatements;
		}
	}

	public static class DoWhileRatherThanWhileOperation extends CompilationUnitRewriteOperation {
		private final WhileStatement visited;

		public DoWhileRatherThanWhileOperation(final WhileStatement visited) {
			this.visited= visited;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.DoWhileRatherThanWhileCleanUp_description, cuRewrite);
			rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
					if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
						return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
					}

					return super.computeSourceRange(nodeWithComment);
				}
			});

			DoStatement newDoStatement= ast.newDoStatement();
			newDoStatement.setExpression(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression(visited.getExpression())));
			newDoStatement.setBody(ASTNodes.createMoveTarget(rewrite, visited.getBody()));
			ASTNodes.replaceButKeepComment(rewrite, visited, newDoStatement, group);
		}
	}

	public static ICleanUpFixCore createCleanUp(final CompilationUnit compilationUnit) {
		List<CompilationUnitRewriteOperation> operations= new ArrayList<>();
		DoWhileRatherThanWhileFinder finder= new DoWhileRatherThanWhileFinder(operations);
		compilationUnit.accept(finder);

		if (operations.isEmpty()) {
			return null;
		}

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[0]);
		return new DoWhileRatherThanWhileFixCore(FixMessages.DoWhileRatherThanWhileFix_description, compilationUnit, ops);
	}

	public static ICleanUpFixCore createCleanUp(final CompilationUnit compilationUnit, final WhileStatement whileStatement) {
		List<CompilationUnitRewriteOperation> operations= new ArrayList<>();
		DoWhileRatherThanWhileFinder finder= new DoWhileRatherThanWhileFinder(operations);
		whileStatement.accept(finder);

		if (operations.isEmpty()) {
			return null;
		}

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[0]);
		return new DoWhileRatherThanWhileFixCore(FixMessages.DoWhileRatherThanWhileFix_description, compilationUnit, ops);
	}

	protected DoWhileRatherThanWhileFixCore(final String name, final CompilationUnit compilationUnit, final CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
}
