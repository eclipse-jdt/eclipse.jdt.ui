/*******************************************************************************
 * Copyright (c) 2021, 2025 Fabrice TIERCELIN and others.
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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PatternInstanceofExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypePattern;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.ImportRemover;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class PatternMatchingForInstanceofFixCore extends CompilationUnitRewriteOperationsFixCore {
	public static final class PatternMatchingForInstanceofFinder extends ASTVisitor {
		private List<CompilationUnitRewriteOperation> fResult;

		public PatternMatchingForInstanceofFinder(List<CompilationUnitRewriteOperation> ops) {
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

			private final Set<String> excludedNames= new HashSet<>();

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
								|| currentNode.getLocationInParent() != IfStatement.EXPRESSION_PROPERTY)
						&& (!(currentNode.getParent() instanceof ConditionalExpression)
								|| currentNode.getLocationInParent() != ConditionalExpression.EXPRESSION_PROPERTY)) {
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

				if (currentNode instanceof InfixExpression infixExp) {
					if (infixExp.getOperator() != Operator.CONDITIONAL_AND &&
							infixExp.getOperator() != Operator.CONDITIONAL_OR) {
						return true;
					}
				}

				if (currentNode.getParent() instanceof ConditionalExpression condExp
						&& visited.getLeftOperand() instanceof SimpleName name) {
					ConditionalCollector collector= new ConditionalCollector(visited, name, excludedNames);
					if (isPositiveCaseToAnalyze) {
						condExp.getThenExpression().accept(collector);
					} else {
						condExp.getElseExpression().accept(collector);
					}
					if (collector.hasResult()) {
						fResult.add(collector.build());
						return false;
					}
				} else if (currentNode.getParent() instanceof IfStatement ifStatement) {
					ResultCollector collector= new ResultCollector(visited);
					if (isPositiveCaseToAnalyze) {
						collector.collect(ifStatement.getThenStatement());
					} else if (ifStatement.getElseStatement() != null) {
						collector.collect(ifStatement.getElseStatement());
					} else if (ASTNodes.fallsThrough(ifStatement.getThenStatement())) {
						collector.collect(ASTNodes.getNextSibling(ifStatement));
					}

					final boolean isPositiveCaseToAnalyzeFinal= isPositiveCaseToAnalyze;
					ASTVisitor expressionVisitor= new ASTVisitor() {
						@Override
						public boolean visit(CastExpression node) {
							if (node.getStartPosition() > visited.getStartPosition()) {
								if (Objects.equals(getIdentifierName(node.getExpression()), getIdentifierName(visited.getLeftOperand()))) {
									ITypeBinding nodeBinding= node.resolveTypeBinding();
									if (nodeBinding != null && nodeBinding.isEqualTo(visited.getRightOperand().resolveBinding())) {
										Expression exp= node;
										InfixExpression infixExp= ASTNodes.getFirstAncestorOrNull(exp, InfixExpression.class);
										InfixExpression originalInfixExp= ASTNodes.getFirstAncestorOrNull(visited, InfixExpression.class);
										while (infixExp != null && infixExp != originalInfixExp) {
											infixExp= ASTNodes.getFirstAncestorOrNull(infixExp, InfixExpression.class);
										}
										if (infixExp != null) {
											if ((isPositiveCaseToAnalyzeFinal && infixExp.getOperator() != InfixExpression.Operator.CONDITIONAL_AND) ||
													(!isPositiveCaseToAnalyzeFinal && infixExp.getOperator() != InfixExpression.Operator.CONDITIONAL_OR)) {
												return true;
											}
										}
										while (exp.getLocationInParent() == ParenthesizedExpression.EXPRESSION_PROPERTY) {
											exp= (Expression) node.getParent();
										}
										collector.addExpressionToReplace(exp);
									}
								}
							}
							return true;
						}
					};

					ifStatement.getExpression().accept(expressionVisitor);

					if (collector.hasResult()) {
						fResult.add(collector.build());
						return true;
					}
				}
				return true;
			}

			private static String getIdentifierName(Expression expression) {
				if (expression instanceof SimpleName simpleName) {
					return simpleName.getIdentifier();
				}
				return null;
			}

			static class ConditionalCollector extends ASTVisitor {
				final InstanceofExpression visited;

				final Set<String> excludedNames;

				final SimpleName variableName;

				String replacementName= null;

				final List<Expression> expressionsToReplace= new ArrayList<>();

				ConditionalCollector(InstanceofExpression visited, SimpleName name, Set<String> excludedNames) {
					this.visited= visited;
					this.excludedNames= excludedNames;
					this.variableName= name;
				}

				public PatternMatchingForConditionalInstanceofFixOperation build() {
					return new PatternMatchingForConditionalInstanceofFixOperation(visited, expressionsToReplace, replacementName);
				}

				boolean hasResult() {
					return replacementName != null && !expressionsToReplace.isEmpty();
				}

				void addMatching(final CastExpression castExp, final SimpleName name) {
					Expression expressionToReplace= castExp;
					ITypeBinding castBinding= castExp.resolveTypeBinding();
					if (this.variableName.getIdentifier().equals(name.getIdentifier())
							&& castBinding != null && castBinding.isEqualTo(visited.getRightOperand().resolveBinding())) {
						while (expressionToReplace.getLocationInParent() == ParenthesizedExpression.EXPRESSION_PROPERTY) {
							expressionToReplace= (Expression) expressionToReplace.getParent();
						}
						if (replacementName == null) {
							String baseName= castBinding.getName().toLowerCase().substring(0, 1);
							MethodDeclaration methodDecl= ASTNodes.getFirstAncestorOrNull(name, MethodDeclaration.class);
							if (methodDecl != null) {
								CompilationUnit cu= (CompilationUnit) name.getRoot();
								IJavaProject project= cu.getJavaElement().getJavaProject();
								replacementName= proposeLocalName(baseName, cu, project, castExp, excludedNames.toArray(new String[0]));
							}
						}
						this.expressionsToReplace.add(expressionToReplace);
					}
				}

				private String proposeLocalName(String baseName, CompilationUnit root, IJavaProject javaProject, ASTNode node, String[] usedNames) {
					// don't propose names that are already in use:
					Collection<String> variableNames= new ScopeAnalyzer(root).getUsedVariableNames(node.getStartPosition(), node.getLength());
					String[] names= new String[variableNames.size() + usedNames.length];
					variableNames.toArray(names);
					System.arraycopy(usedNames, 0, names, variableNames.size(), usedNames.length);
					return StubUtility.getLocalNameSuggestions(javaProject, baseName, 0, names)[0];
				}

				@Override
				public boolean visit(SimpleName node) {
					if (node.getLocationInParent() == CastExpression.EXPRESSION_PROPERTY) {
						addMatching((CastExpression) node.getParent(), node);
					}
					return false;
				}

			}

			static class ResultCollector {
				final InstanceofExpression visited;

				SimpleName variableName;

				final List<VariableDeclarationStatement> statementsToRemove= new ArrayList<>();

				final List<VariableDeclarationStatement> statementsToConvert= new ArrayList<>();

				final List<Expression> expressionsToReplace= new ArrayList<>();

				ResultCollector(InstanceofExpression visited) {
					this.visited= visited;
				}

				public PatternMatchingForInstanceofFixOperation build() {
					return new PatternMatchingForInstanceofFixOperation(visited, statementsToRemove, statementsToConvert, expressionsToReplace, variableName);
				}

				public void addExpressionToReplace(Expression exp) {
					expressionsToReplace.add(exp);
				}

				boolean hasResult() {
					return variableName != null && !statementsToRemove.isEmpty();
				}

				void addMatching(final VariableDeclarationStatement statement, final SimpleName name, boolean toConvert) {
					if (this.variableName == null || this.variableName.getIdentifier().equals(name.getIdentifier())) {
						this.variableName= name;
						if (toConvert) {
							this.statementsToConvert.add(statement);
						} else {
							this.statementsToRemove.add(statement);
						}
					}
				}

				boolean collect(final Statement conditionalStatements) {
					List<Statement> statements= ASTNodes.asList(conditionalStatements);
					boolean convertToAssignment= false;

					if (!statements.isEmpty()) {
						for (Statement statement : statements) {
							if (statement instanceof ExpressionStatement expressionStatement) {
								Assignment assignment= ASTNodes.as(expressionStatement.getExpression(), Assignment.class);
								if (assignment != null) {
									if (Objects.equals(getIdentifierName(visited.getLeftOperand()), getIdentifierName(assignment.getLeftHandSide()))) {
										// The same variable is assigned, this can't be handled further, something like this:
										// if (x instanceof T) {
										//      x = ...
										//
										return true;
									}
									if (Objects.equals(getIdentifierName(variableName), getIdentifierName(assignment.getLeftHandSide()))) {
										// The same variable is assigned, this can't be handled further, something like this:
										// if (x instanceof T y) {
										//      y = ...
										//
										return true;
									}
								}
							}
							if (statement instanceof VariableDeclarationStatement variableDeclarationExpression) {
								VariableDeclarationFragment variableDeclarationFragment= ASTNodes.getUniqueFragment(variableDeclarationExpression);
								if (variableDeclarationFragment != null
										&& Objects.equals(visited.getRightOperand().resolveBinding(), variableDeclarationExpression.getType().resolveBinding())) {
									CastExpression castExpression= ASTNodes.as(variableDeclarationFragment.getInitializer(), CastExpression.class);

									if (castExpression != null
											&& Objects.equals(visited.getRightOperand().resolveBinding(), castExpression.getType().resolveBinding())
											&& ASTNodes.match(visited.getLeftOperand(), castExpression.getExpression())
											&& ASTNodes.isPassive(visited.getLeftOperand())) {
										addMatching(variableDeclarationExpression, variableDeclarationFragment.getName(), convertToAssignment);
									}
								}
							}
							if (statement instanceof IfStatement innerIf) {
								if (collect(innerIf.getThenStatement())) {
									convertToAssignment= true;
								}
								if (innerIf.getElseStatement() != null) {
									if (collect(innerIf.getElseStatement())) {
										convertToAssignment= true;
									}
								}
							}
						}
					}
					return false;
				}

			}

		}
	}

	public static class PatternMatchingForConditionalInstanceofFixOperation extends CompilationUnitRewriteOperation {
		private final InstanceofExpression nodeToComplete;

		private final List<Expression> expressionsToReplace;

		private final String replacementName;

		public PatternMatchingForConditionalInstanceofFixOperation(final InstanceofExpression nodeToComplete, final List<Expression> expressionsToReplace, final String replacementName) {
			this.nodeToComplete= nodeToComplete;
			this.expressionsToReplace= expressionsToReplace;
			this.replacementName= replacementName;
		}

		@SuppressWarnings("deprecation")
		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= rewrite.getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.PatternMatchingForInstanceofCleanup_description, cuRewrite);

			PatternInstanceofExpression newInstanceof= ast.newPatternInstanceofExpression();
			newInstanceof.setLeftOperand(ASTNodes.createMoveTarget(rewrite, nodeToComplete.getLeftOperand()));
			SingleVariableDeclaration newSVDecl= ast.newSingleVariableDeclaration();
			newSVDecl.setName(ast.newSimpleName(replacementName));
			newSVDecl.setType(ASTNodes.createMoveTarget(rewrite, nodeToComplete.getRightOperand()));
			if ((ast.apiLevel() == AST.JLS20 && ast.isPreviewEnabled()) || ast.apiLevel() > AST.JLS20) {
				TypePattern newTypePattern= ast.newTypePattern();
				newTypePattern.setPatternVariable((VariableDeclaration) newSVDecl);
				newInstanceof.setPattern(newTypePattern);
			} else {
				newInstanceof.setRightOperand(newSVDecl);
			}
			ASTNodes.replaceButKeepComment(rewrite, nodeToComplete, newInstanceof, group);

			ASTNodes.replaceButKeepComment(rewrite, nodeToComplete, newInstanceof, group);
			for (Expression expressionToReplace : expressionsToReplace) {
				SimpleName name= ast.newSimpleName(replacementName);
				rewrite.replace(expressionToReplace, name, group);
			}
		}
	}

	public static class PatternMatchingForInstanceofFixOperation extends CompilationUnitRewriteOperation {
		private final InstanceofExpression nodeToComplete;

		private final List<VariableDeclarationStatement> statementsToRemove;

		private final List<VariableDeclarationStatement> statementsToConvert;

		private final List<Expression> expressionsToReplace;

		private final SimpleName expressionToMove;

		public PatternMatchingForInstanceofFixOperation(final InstanceofExpression nodeToComplete, final List<VariableDeclarationStatement> statementsToRemove,
				final List<VariableDeclarationStatement> statementsToConvert, final List<Expression> expressionsToReplace, final SimpleName expressionToMove) {
			this.nodeToComplete= nodeToComplete;
			this.statementsToRemove= statementsToRemove;
			this.statementsToConvert= statementsToConvert;
			this.expressionsToReplace= expressionsToReplace;
			this.expressionToMove= expressionToMove;
		}

		@SuppressWarnings("deprecation")
		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			ImportRemover importRemover= cuRewrite.getImportRemover();
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
			if (statementsToRemove.stream().allMatch(varDec -> Modifier.isFinal(varDec.getModifiers()))) {
				newSVDecl.modifiers().add(ast.newModifier(ModifierKeyword.fromFlagValue(Modifier.FINAL)));
			}
			if ((ast.apiLevel() == AST.JLS20 && ast.isPreviewEnabled()) || ast.apiLevel() > AST.JLS20) {
				TypePattern newTypePattern= ast.newTypePattern();
				newTypePattern.setPatternVariable((VariableDeclaration) newSVDecl);
				newInstanceof.setPattern(newTypePattern);
			} else {
				newInstanceof.setRightOperand(newSVDecl);
			}

			ASTNodes.replaceButKeepComment(rewrite, nodeToComplete, newInstanceof, group);

			for (var statementToRemove : statementsToRemove) {
				if (ASTNodes.canHaveSiblings(statementToRemove) || statementToRemove.getLocationInParent() == IfStatement.ELSE_STATEMENT_PROPERTY) {
					ASTNodes.removeButKeepComment(rewrite, statementToRemove, group);
				} else {
					ASTNodes.replaceButKeepComment(rewrite, statementToRemove, ast.newBlock(), group);
				}
				importRemover.registerRemovedNode(statementToRemove);
			}

			for (var expressionToReplace : expressionsToReplace) {
				var newName= rewrite.createCopyTarget(expressionToMove);
				rewrite.replace(expressionToReplace, newName, group);
			}

			for (var statementToConvert : statementsToConvert) {
				VariableDeclarationFragment fragment= ASTNodes.getUniqueFragment(statementToConvert);
				var assignment= ast.newAssignment();
				assignment.setLeftHandSide((Expression) ASTNode.copySubtree(ast, fragment.getName()));
				assignment.setRightHandSide((Expression) ASTNode.copySubtree(ast, fragment.getInitializer()));
				var replacement= ast.newExpressionStatement(assignment);
				ASTNodes.replaceButKeepComment(rewrite, statementToConvert, replacement, group);
			}
		}
	}


	public static ICleanUpFix createCleanUp(final CompilationUnit compilationUnit) {
		List<CompilationUnitRewriteOperation> operations= new ArrayList<>();
		PatternMatchingForInstanceofFinder finder= new PatternMatchingForInstanceofFinder(operations);
		compilationUnit.accept(finder);

		if (operations.isEmpty()) {
			return null;
		}

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[0]);
		return new PatternMatchingForInstanceofFixCore(FixMessages.PatternMatchingForInstanceofFix_refactor, compilationUnit, ops);
	}

	protected PatternMatchingForInstanceofFixCore(final String name, final CompilationUnit compilationUnit,
			final CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
}
