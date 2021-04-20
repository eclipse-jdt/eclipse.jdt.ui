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
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class OneIfRatherThanDuplicateBlocksThatFallThroughFixCore extends CompilationUnitRewriteOperationsFixCore {
	public static final class OneIfRatherThanDuplicateBlocksThatFallThroughFinder extends ASTVisitor {
		private List<CompilationUnitRewriteOperation> fResult;

		public OneIfRatherThanDuplicateBlocksThatFallThroughFinder(List<CompilationUnitRewriteOperation> ops) {
			fResult= ops;
		}

		@Override
		public boolean visit(final Block visited) {
			SuccessiveIfVisitor successiveIfVisitor= new SuccessiveIfVisitor(visited);
			visited.accept(successiveIfVisitor);
			return successiveIfVisitor.result;
		}

		private final class SuccessiveIfVisitor extends ASTVisitor {
			private final Block startNode;
			private boolean result= true;

			public SuccessiveIfVisitor(final Block startNode) {
				this.startNode= startNode;
			}

			@Override
			public boolean visit(final Block node) {
				return startNode == node;
			}

			@Override
			public boolean visit(final IfStatement visited) {
				if (result && ASTNodes.fallsThrough(visited.getThenStatement())) {
					List<IfStatement> duplicateIfBlocks= new ArrayList<>(ASTNodes.EXCESSIVE_OPERAND_NUMBER - 1);
					AtomicInteger operandCount= new AtomicInteger(ASTNodes.getNbOperands(visited.getExpression()));
					duplicateIfBlocks.add(visited);

					while (addOneMoreIf(duplicateIfBlocks, operandCount)) {
						// OK continue
					}

					if (duplicateIfBlocks.size() > 1) {
						fResult.add(new OneIfRatherThanDuplicateBlocksThatFallThroughOperation(duplicateIfBlocks));
						result= false;
						return false;
					}
				}

				return true;
			}

			private boolean addOneMoreIf(final List<IfStatement> duplicateIfBlocks, final AtomicInteger operandCount) {
				IfStatement lastBlock= duplicateIfBlocks.get(duplicateIfBlocks.size() - 1);

				if (lastBlock.getElseStatement() == null) {
					IfStatement nextSibling= ASTNodes.as(ASTNodes.getNextSibling(lastBlock), IfStatement.class);

					if (nextSibling != null
							&& nextSibling.getElseStatement() == null
							&& operandCount.get() + ASTNodes.getNbOperands(nextSibling.getExpression()) < ASTNodes.EXCESSIVE_OPERAND_NUMBER
							&& ASTNodes.match(lastBlock.getThenStatement(), nextSibling.getThenStatement())) {
						operandCount.addAndGet(ASTNodes.getNbOperands(nextSibling.getExpression()));
						duplicateIfBlocks.add(nextSibling);
						return true;
					}
				}

				return false;
			}
		}
	}

	private static class OneIfRatherThanDuplicateBlocksThatFallThroughOperation extends CompilationUnitRewriteOperation {
		private final List<IfStatement> duplicateIfBlocks;

		public OneIfRatherThanDuplicateBlocksThatFallThroughOperation(final List<IfStatement> duplicateIfBlocks) {
			this.duplicateIfBlocks= duplicateIfBlocks;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.OneIfRatherThanDuplicateBlocksThatFallThroughCleanUp_description, cuRewrite);
			rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
					if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
						return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
					}

					return super.computeSourceRange(nodeWithComment);
				}
			});

			List<Expression> newConditions= new ArrayList<>(duplicateIfBlocks.size());

			for (IfStatement ifStatement : duplicateIfBlocks) {
				InfixExpression oneOriginalCondition= ASTNodes.as(ifStatement.getExpression(), InfixExpression.class);

				if (ASTNodes.hasOperator(oneOriginalCondition, InfixExpression.Operator.CONDITIONAL_OR)) {
					newConditions.add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression(ifStatement.getExpression())));
				} else {
					newConditions.add(ASTNodeFactory.parenthesizeIfNeeded(ast, ASTNodes.createMoveTarget(rewrite, ifStatement.getExpression())));
				}
			}

			InfixExpression newCondition= ast.newInfixExpression();
			newCondition.setOperator(InfixExpression.Operator.CONDITIONAL_OR);
			newCondition.setLeftOperand(newConditions.remove(0));
			newCondition.setRightOperand(newConditions.remove(0));
			newCondition.extendedOperands().addAll(newConditions);

			for (int i= 0; i < duplicateIfBlocks.size() - 1; i++) {
				ASTNodes.removeButKeepComment(rewrite, duplicateIfBlocks.get(i), group);
			}

			rewrite.replace(duplicateIfBlocks.get(duplicateIfBlocks.size() - 1).getExpression(), newCondition, group);
		}
	}

	public static ICleanUpFixCore createCleanUp(final CompilationUnit compilationUnit) {
		List<CompilationUnitRewriteOperation> operations= new ArrayList<>();
		OneIfRatherThanDuplicateBlocksThatFallThroughFinder finder= new OneIfRatherThanDuplicateBlocksThatFallThroughFinder(operations);
		compilationUnit.accept(finder);

		if (operations.isEmpty()) {
			return null;
		}

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[0]);
		return new OneIfRatherThanDuplicateBlocksThatFallThroughFixCore(FixMessages.OneIfRatherThanDuplicateBlocksThatFallThroughFix_description, compilationUnit, ops);
	}

	protected OneIfRatherThanDuplicateBlocksThatFallThroughFixCore(final String name, final CompilationUnit compilationUnit, final CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
}
