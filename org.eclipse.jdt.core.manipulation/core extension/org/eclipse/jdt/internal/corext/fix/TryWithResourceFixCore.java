/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.VarDefinitionsUsesVisitor;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

/**
 * A fix that changes code to make use of Java 7 try-with-resources feature. In particular, it
 * removes now useless finally clauses.
 */
public class TryWithResourceFixCore extends CompilationUnitRewriteOperationsFixCore {

	public TryWithResourceFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation operation) {
		super(name, compilationUnit, operation);
	}

	public static ICleanUpFix createCleanUp(final CompilationUnit unit) {

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final Block visited) {
				DeclarationAndTryVisitor declarationAndTryVisitor= new DeclarationAndTryVisitor(visited);
				visited.accept(declarationAndTryVisitor);
				return declarationAndTryVisitor.result;
			}

			final class DeclarationAndTryVisitor extends ASTVisitor {
				private final Block startNode;

				private boolean result= true;

				public DeclarationAndTryVisitor(final Block startNode) {
					this.startNode= startNode;
				}

				@Override
				public boolean visit(final Block visited) {
					return startNode == visited;
				}

				@Override
				public boolean visit(final TryStatement visited) {
					if (!result) {
						return true;
					}

					VariableDeclarationStatement previousDeclarationStatement= ASTNodes.as(ASTNodes.getPreviousSibling(visited),
							VariableDeclarationStatement.class);
					List<Statement> finallyStatements= ASTNodes.asList(visited.getFinally());

					if (previousDeclarationStatement == null
							|| finallyStatements.isEmpty()) {
						return true;
					}

					VariableDeclarationFragment previousDeclarationFragment= ASTNodes.getUniqueFragment(previousDeclarationStatement);

					if (previousDeclarationFragment == null
							|| previousDeclarationFragment.resolveBinding() == null) {
						return true;
					}

					Statement finallyFirstFStatement= finallyStatements.get(0);
					List<ASTNode> nodesToRemove= new ArrayList<>();
					nodesToRemove.add(finallyStatements.size() == 1 ? visited.getFinally() : finallyFirstFStatement);

					boolean isCloseableUsedAfter= isCloseableUsedAfter(previousDeclarationFragment, visited);
					Expression closedVariable= getClosedVariable(previousDeclarationFragment, finallyFirstFStatement);

					if ((isCloseableUsedAfter && !JavaModelUtil.is9OrHigher(((CompilationUnit) visited.getRoot()).getJavaElement().getJavaProject()))
							|| closedVariable == null
							|| !ASTNodes.areSameVariables(previousDeclarationFragment, closedVariable)) {
						return true;
					}

					return maybeUseTryWithResource(visited, previousDeclarationStatement, previousDeclarationFragment, isCloseableUsedAfter, nodesToRemove);
				}

				private boolean maybeUseTryWithResource(
						final TryStatement visited,
						final VariableDeclarationStatement previousDeclarationStatement,
						final VariableDeclarationFragment previousDeclarationFragment,
						final boolean isCloseableUsedAfter,
						final List<ASTNode> nodesToRemove) {
					VarDefinitionsUsesVisitor visitor= new VarDefinitionsUsesVisitor(previousDeclarationFragment);
					List<SimpleName> closeableAssignments= visitor.getWrites();
					List<Statement> tryStatements= ASTNodes.asList(visited.getBody());

					if (!isCloseableUsedAfter
							&& !tryStatements.isEmpty()) {
						Statement tryFirstStatement= tryStatements.get(0);
						Assignment assignResource= ASTNodes.asExpression(tryFirstStatement, Assignment.class);

						if (assignResource != null
								&& ASTNodes.isSameVariable(previousDeclarationFragment, assignResource.getLeftHandSide())) {
							if (!containsExactly(closeableAssignments, previousDeclarationFragment.getName(), assignResource.getLeftHandSide())) {
								return true;
							}

							nodesToRemove.add(tryFirstStatement);
							rewriteOperations.add(new TryWithResourceOperation(visited, previousDeclarationStatement, previousDeclarationFragment, assignResource, nodesToRemove));

							result= false;
							return false;
						}
					}

					if (containsExactly(closeableAssignments, previousDeclarationFragment.getName())) {
						rewriteOperations.add(new TryWithResourceOperation(visited, previousDeclarationStatement, previousDeclarationFragment, null, nodesToRemove));

						result= false;
						return false;
					}

					return true;
				}

				private boolean containsExactly(final List<SimpleName> closeableOccurrences, final Expression... simpleNames) {
					return closeableOccurrences.size() == simpleNames.length && closeableOccurrences.containsAll(Arrays.asList(simpleNames));
				}

				private boolean isCloseableUsedAfter(final VariableDeclarationFragment previousDeclarationFragment, final TryStatement visited) {
					IVariableBinding varBinding= previousDeclarationFragment.resolveBinding();
					List<Statement> nextStatements= ASTNodes.getNextSiblings(visited);

					for (Statement nextStatement : nextStatements) {
						VarDefinitionsUsesVisitor visitor= new VarDefinitionsUsesVisitor(varBinding, nextStatement, true);

						if (!visitor.getReads().isEmpty() || !visitor.getWrites().isEmpty()) {
							return true;
						}
					}

					return false;
				}

				private Expression getClosedVariable(final VariableDeclarationFragment previousDeclarationFragment, final Statement finallyStatement) {
					Statement firstStatement= finallyStatement;
					IfStatement finallyIfStatement= ASTNodes.as(finallyStatement, IfStatement.class);

					if (finallyIfStatement != null
							&& ASTNodes.asList(finallyIfStatement.getThenStatement()).size() == 1
							&& ASTNodes.asList(finallyIfStatement.getElseStatement()).isEmpty()) {
						Expression closedVariable= ASTNodes.getNullCheckedExpression(finallyIfStatement.getExpression());

						if (ASTNodes.areSameVariables(previousDeclarationFragment, closedVariable)) {
							firstStatement= ASTNodes.asList(finallyIfStatement.getThenStatement()).get(0);
						}
					}

					MethodInvocation closeMethod= ASTNodes.asExpression(firstStatement, MethodInvocation.class);

					if (closeMethod != null
							&& ASTNodes.usesGivenSignature(closeMethod, Closeable.class.getCanonicalName(), "close")) { //$NON-NLS-1$
						return closeMethod.getExpression();
					}

					return null;
				}
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFixCore(MultiFixMessages.TryWithResourceCleanup_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[0]));
	}

	private static class TryWithResourceOperation extends CompilationUnitRewriteOperation {
		private final TryStatement visited;

		private final VariableDeclarationStatement previousDeclStatement;

		private final VariableDeclarationFragment previousDeclFragment;

		private final Assignment assignResource;

		private final List<ASTNode> nodesToRemove;

		public TryWithResourceOperation(final TryStatement visited,
				final VariableDeclarationStatement previousDeclStatement,
				final VariableDeclarationFragment previousDeclFragment,
				final Assignment assignResource,
				final List<ASTNode> nodesToRemove) {
			this.visited= visited;
			this.previousDeclStatement= previousDeclStatement;
			this.previousDeclFragment= previousDeclFragment;
			this.assignResource= assignResource;
			this.nodesToRemove= nodesToRemove;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.TryWithResourceCleanup_description, cuRewrite);

			rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode node) {
					if (Boolean.TRUE.equals(node.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
						return new SourceRange(node.getStartPosition(), node.getLength());
					}

					return super.computeSourceRange(node);
				}
			});

			Expression newResource;
			if (JavaModelUtil.is9OrHigher(((CompilationUnit) visited.getRoot()).getJavaElement().getJavaProject())
					&& assignResource == null) {
				// So we are in Java 9 or higher
				newResource= (Expression) rewrite.createCopyTarget(previousDeclFragment.getName());
			} else {
				VariableDeclarationFragment newFragment;
				if (assignResource != null) {
					newFragment= ast.newVariableDeclarationFragment();
					newFragment.setName(ASTNodes.createMoveTarget(rewrite, previousDeclFragment.getName()));
					newFragment.setInitializer(ASTNodes.createMoveTarget(rewrite, assignResource.getRightHandSide()));
				} else {
					newFragment= ASTNodes.createMoveTarget(rewrite, previousDeclFragment);
				}

				VariableDeclarationExpression newResourceDeclaration= ast.newVariableDeclarationExpression(newFragment);
				newResourceDeclaration.setType(ASTNodes.createMoveTarget(rewrite, previousDeclStatement.getType()));

				newResource= newResourceDeclaration;
				ASTNodes.removeButKeepComment(rewrite, previousDeclStatement, group);
			}

			ListRewrite listRewrite= rewrite.getListRewrite(visited, TryStatement.RESOURCES2_PROPERTY);
			listRewrite.insertFirst(newResource, group);

			for (ASTNode nodeToRemove : nodesToRemove) {
				ASTNodes.removeButKeepComment(rewrite, nodeToRemove, group);
			}
		}
	}
}
