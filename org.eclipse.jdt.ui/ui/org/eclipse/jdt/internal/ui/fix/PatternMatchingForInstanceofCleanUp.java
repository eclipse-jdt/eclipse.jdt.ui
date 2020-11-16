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
 *     IBM Corporation - Bug 565447
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.PreviewFeaturesSubProcessor;

/**
 * A fix that uses pattern matching for the instanceof expression when possible.
 */
public class PatternMatchingForInstanceofCleanUp extends AbstractMultiFix implements ICleanUpFix {
	public PatternMatchingForInstanceofCleanUp() {
		this(Collections.emptyMap());
	}

	public PatternMatchingForInstanceofCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.USE_PATTERN_MATCHING_FOR_INSTANCEOF);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.USE_PATTERN_MATCHING_FOR_INSTANCEOF)) {
			return new String[] { MultiFixMessages.PatternMatchingForInstanceofCleanup_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.USE_PATTERN_MATCHING_FOR_INSTANCEOF)) {
			return "" //$NON-NLS-1$
					+ "if (object instanceof Integer i) {\n" //$NON-NLS-1$
					+ "    return i.intValue();\n" //$NON-NLS-1$
					+ "}\n\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "if (object instanceof Integer) {\n" //$NON-NLS-1$
				+ "    final Integer i = (Integer) object;\n" //$NON-NLS-1$
				+ "    return i.intValue();\n" //$NON-NLS-1$
				+ "}\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.USE_PATTERN_MATCHING_FOR_INSTANCEOF)
				|| !PreviewFeaturesSubProcessor.isPreviewFeatureEnabled(unit.getJavaElement().getJavaProject())
				|| !JavaModelUtil.is15OrHigher(unit.getJavaElement().getJavaProject())) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final Block node) {
				InstanceofVisitor instanceofVisitor= new InstanceofVisitor(node);
				node.accept(instanceofVisitor);
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
				public boolean visit(final Block node) {
					return startNode == node;
				}

				@Override
				public boolean visit(final InstanceofExpression node) {
					if (node.getPatternVariable() != null
							|| !ASTNodes.isPassive(node.getLeftOperand())
							|| node.getRightOperand().resolveBinding() == null) {
						return true;
					}

					boolean isPositiveCaseToAnalyze= true;
					ASTNode currentNode= node;

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
						return maybeMatchPattern(node, ifStatement.getThenStatement());
					}

					if (ifStatement.getElseStatement() != null) {
						return maybeMatchPattern(node, ifStatement.getElseStatement());
					}

					if (ASTNodes.fallsThrough(ifStatement.getThenStatement())) {
						return maybeMatchPattern(node, ASTNodes.getNextSibling(ifStatement));
					}

					return true;
				}

				private boolean maybeMatchPattern(final InstanceofExpression node, final Statement expression) {
					List<Statement> statements= ASTNodes.asList(expression);

					if (!statements.isEmpty()) {
						VariableDeclarationStatement variableDeclarationExpression= ASTNodes.as(statements.get(0), VariableDeclarationStatement.class);
						VariableDeclarationFragment variableDeclarationFragment= ASTNodes.getUniqueFragment(variableDeclarationExpression);

						if (variableDeclarationFragment != null && Modifier.isFinal(variableDeclarationExpression.getModifiers())) {
							CastExpression castExpression= ASTNodes.as(variableDeclarationFragment.getInitializer(), CastExpression.class);

							if (castExpression != null
									&& Objects.equals(node.getRightOperand().resolveBinding(), variableDeclarationExpression.getType().resolveBinding())
									&& Objects.equals(node.getRightOperand().resolveBinding(), castExpression.getType().resolveBinding())
									&& ASTNodes.isPassive(node.getLeftOperand())
									&& ASTNodes.match(node.getLeftOperand(), castExpression.getExpression())) {
								rewriteOperations.add(new PatternMatchingForInstanceofOperation(node, variableDeclarationExpression, variableDeclarationFragment.getName()));
								return false;
							}
						}
					}

					return true;
				}
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.PatternMatchingForInstanceofCleanup_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[0]));
	}

	@Override
	public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {
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

	private static class PatternMatchingForInstanceofOperation extends CompilationUnitRewriteOperation {
		private final InstanceofExpression nodeToComplete;
		private final VariableDeclarationStatement statementToRemove;
		private final SimpleName expressionToMove;

		public PatternMatchingForInstanceofOperation(final InstanceofExpression nodeToComplete, final VariableDeclarationStatement statementToRemove, final SimpleName expressionToMove) {
			this.nodeToComplete= nodeToComplete;
			this.statementToRemove= statementToRemove;
			this.expressionToMove= expressionToMove;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.PatternMatchingForInstanceofCleanup_description, cuRewrite);

			InstanceofExpression newInstanceof= ast.newInstanceofExpression();
			newInstanceof.setLeftOperand(ASTNodes.createMoveTarget(rewrite, nodeToComplete.getLeftOperand()));
			newInstanceof.setRightOperand(ASTNodes.createMoveTarget(rewrite, nodeToComplete.getRightOperand()));
			newInstanceof.setPatternVariable(ASTNodes.createMoveTarget(rewrite, expressionToMove));

			ASTNodes.replaceButKeepComment(rewrite, nodeToComplete, newInstanceof, group);

			if (ASTNodes.canHaveSiblings(statementToRemove) || statementToRemove.getLocationInParent() == IfStatement.ELSE_STATEMENT_PROPERTY) {
				rewrite.remove(statementToRemove, group);
			} else {
				ASTNodes.replaceButKeepComment(rewrite, statementToRemove, ast.newBlock(), group);
			}
		}
	}
}
