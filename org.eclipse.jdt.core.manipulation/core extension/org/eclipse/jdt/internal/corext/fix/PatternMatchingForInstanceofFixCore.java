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
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.PatternInstanceofExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class PatternMatchingForInstanceofFixCore extends CompilationUnitRewriteOperationsFixCore {
	public static final class PatternMatchingForInstanceofFinder extends ASTVisitor {
		private List<PatternMatchingForInstanceofFixOperation> fResult;

		public PatternMatchingForInstanceofFinder(List<PatternMatchingForInstanceofFixOperation> ops) {
			fResult= ops;
		}

		@Override
		public boolean visit(final Block visited) {
			InstanceofVisitor instanceofVisitor= new InstanceofVisitor(visited);
			visited.accept(instanceofVisitor);
			return instanceofVisitor.getResult();
		}

		final class InstanceofVisitor extends ASTVisitor {
			private final Block startNode;
			private boolean result= true;

			public InstanceofVisitor(final Block startNode) {
				this.startNode= startNode;
			}

			/**
			 * @return The result
			 */
			public boolean getResult() {
				return result;
			}

			@Override
			public boolean visit(final Block visited) {
				return startNode == visited;
			}

			@Override
			public boolean visit(final InstanceofExpression visited) {
				if (!ASTNodes.isPassive(visited.getLeftOperand())
						|| visited.getRightOperand().resolveBinding() == null) {
					return true;
				}

				boolean isPositiveCaseToAnalyze= true;
				ASTNode currentNode= visited;

				while (currentNode.getParent() != null
						&& (!(currentNode.getParent() instanceof IfStatement)
						|| currentNode.getLocationInParent() != IfStatement.EXPRESSION_PROPERTY)) {
					switch (currentNode.getParent().getNodeType()) {
						case ASTNode.PARENTHESIZED_EXPRESSION:
							break;

						case ASTNode.PREFIX_EXPRESSION:
							if (!ASTNodes.hasOperator((PrefixExpression) currentNode.getParent(), PrefixExpression.Operator.NOT)) {
								return true;
							}

							isPositiveCaseToAnalyze= !isPositiveCaseToAnalyze;
							break;

						case ASTNode.INFIX_EXPRESSION:
							if (isPositiveCaseToAnalyze
									? !ASTNodes.hasOperator((InfixExpression) currentNode.getParent(), InfixExpression.Operator.CONDITIONAL_AND, InfixExpression.Operator.AND)
											: !ASTNodes.hasOperator((InfixExpression) currentNode.getParent(), InfixExpression.Operator.CONDITIONAL_OR, InfixExpression.Operator.OR)) {
								return true;
							}

							break;

						default:
							return true;
					}

					currentNode= currentNode.getParent();
				}

				if (currentNode.getParent() == null) {
					return true;
				}

				IfStatement ifStatement= (IfStatement) currentNode.getParent();

				if (isPositiveCaseToAnalyze) {
					return maybeMatchPattern(visited, ifStatement.getThenStatement());
				}

				if (ifStatement.getElseStatement() != null) {
					return maybeMatchPattern(visited, ifStatement.getElseStatement());
				}

				if (ASTNodes.fallsThrough(ifStatement.getThenStatement())) {
					return maybeMatchPattern(visited, ASTNodes.getNextSibling(ifStatement));
				}

				return true;
			}

			private boolean maybeMatchPattern(final InstanceofExpression visited, final Statement conditionalStatements) {
				List<Statement> statements= ASTNodes.asList(conditionalStatements);

				if (!statements.isEmpty()) {
					VariableDeclarationStatement variableDeclarationExpression= ASTNodes.as(statements.get(0), VariableDeclarationStatement.class);
					VariableDeclarationFragment variableDeclarationFragment= ASTNodes.getUniqueFragment(variableDeclarationExpression);

					if (variableDeclarationFragment != null
							&& Objects.equals(visited.getRightOperand().resolveBinding(), variableDeclarationExpression.getType().resolveBinding())) {
						CastExpression castExpression= ASTNodes.as(variableDeclarationFragment.getInitializer(), CastExpression.class);

						if (castExpression != null
								&& Objects.equals(visited.getRightOperand().resolveBinding(), castExpression.getType().resolveBinding())
								&& ASTNodes.match(visited.getLeftOperand(), castExpression.getExpression())
								&& ASTNodes.isPassive(visited.getLeftOperand())) {
							fResult.add(new PatternMatchingForInstanceofFixOperation(visited, variableDeclarationExpression, variableDeclarationFragment.getName()));
							return false;
						}
					}
				}

				return true;
			}
		}
	}

	public static class PatternMatchingForInstanceofFixOperation extends CompilationUnitRewriteOperation {
		private final InstanceofExpression nodeToComplete;
		private final VariableDeclarationStatement statementToRemove;
		private final SimpleName expressionToMove;

		public PatternMatchingForInstanceofFixOperation(final InstanceofExpression nodeToComplete, final VariableDeclarationStatement statementToRemove, final SimpleName expressionToMove) {
			this.nodeToComplete= nodeToComplete;
			this.statementToRemove= statementToRemove;
			this.expressionToMove= expressionToMove;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.PatternMatchingForInstanceofCleanup_description, cuRewrite);
			rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
					if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
						return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
					}

					return super.computeSourceRange(nodeWithComment);
				}
			});

			PatternInstanceofExpression newInstanceof= ast.newPatternInstanceofExpression();
			newInstanceof.setLeftOperand(ASTNodes.createMoveTarget(rewrite, nodeToComplete.getLeftOperand()));
			SingleVariableDeclaration newSVDecl= ast.newSingleVariableDeclaration();
			newSVDecl.setName(ASTNodes.createMoveTarget(rewrite, expressionToMove));
			newSVDecl.setType(ASTNodes.createMoveTarget(rewrite, nodeToComplete.getRightOperand()));
			newInstanceof.setRightOperand(newSVDecl);

			ASTNodes.replaceButKeepComment(rewrite, nodeToComplete, newInstanceof, group);

			if (ASTNodes.canHaveSiblings(statementToRemove) || statementToRemove.getLocationInParent() == IfStatement.ELSE_STATEMENT_PROPERTY) {
				rewrite.remove(statementToRemove, group);
			} else {
				ASTNodes.replaceButKeepComment(rewrite, statementToRemove, ast.newBlock(), group);
			}
		}
	}


	public static ICleanUpFixCore createCleanUp(final CompilationUnit compilationUnit) {
		List<PatternMatchingForInstanceofFixOperation> operations= new ArrayList<>();
		PatternMatchingForInstanceofFinder finder= new PatternMatchingForInstanceofFinder(operations);
		compilationUnit.accept(finder);

		if (operations.isEmpty()) {
			return null;
		}

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[0]);
		return new PatternMatchingForInstanceofFixCore(FixMessages.PatternMatchingForInstanceofFix_refactor, compilationUnit, ops);
	}

	protected PatternMatchingForInstanceofFixCore(final String name, final CompilationUnit compilationUnit, final CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
}
