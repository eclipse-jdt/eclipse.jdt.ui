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
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Statement;
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
 * A fix that merge conditions of if/else if/else that have the same blocks.
 */
public class MergeConditionalBlocksCleanUp extends AbstractMultiFix {
	public MergeConditionalBlocksCleanUp() {
		this(Collections.emptyMap());
	}

	public MergeConditionalBlocksCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.MERGE_CONDITIONAL_BLOCKS);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.MERGE_CONDITIONAL_BLOCKS)) {
			return new String[] { MultiFixMessages.MergeConditionalBlocksCleanup_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();

		if (isEnabled(CleanUpConstants.MERGE_CONDITIONAL_BLOCKS)) {
			bld.append("if ((i == 0) || (i == 1)) {\n"); //$NON-NLS-1$
		} else {
			bld.append("if (i == 0) {\n"); //$NON-NLS-1$
			bld.append("    System.out.println(\"Duplicate\");\n"); //$NON-NLS-1$
			bld.append("} else if (i == 1) {\n"); //$NON-NLS-1$
		}

		bld.append("    System.out.println(\"Duplicate\");\n"); //$NON-NLS-1$
		bld.append("} else {\n"); //$NON-NLS-1$
		bld.append("    System.out.println(\"Different\");\n"); //$NON-NLS-1$
		bld.append("}\n"); //$NON-NLS-1$

		return bld.toString();
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.MERGE_CONDITIONAL_BLOCKS)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final IfStatement node) {
				if (node.getElseStatement() != null) {
					List<IfStatement> duplicateIfBlocks= new ArrayList<>(4);
					List<Boolean> isThenStatement= new ArrayList<>(4);
					AtomicInteger operandCount= new AtomicInteger(ASTNodes.getNbOperands(node.getExpression()));
					duplicateIfBlocks.add(node);
					isThenStatement.add(Boolean.TRUE);

					while (addOneMoreIf(duplicateIfBlocks, isThenStatement, operandCount)) {
						// OK continue
					}

					if (duplicateIfBlocks.size() > 1) {
						rewriteOperations.add(new MergeConditionalBlocksOperation(duplicateIfBlocks, isThenStatement));
						return false;
					}
				}

				return true;
			}

			private boolean addOneMoreIf(final List<IfStatement> duplicateIfBlocks, final List<Boolean> isThenStatement, final AtomicInteger operandCount) {
				IfStatement lastBlock= getLast(duplicateIfBlocks);
				Statement previousStatement= getLast(isThenStatement) ? lastBlock.getThenStatement() : lastBlock.getElseStatement();
				Statement nextStatement= getLast(isThenStatement) ? lastBlock.getElseStatement() : lastBlock.getThenStatement();

				if (nextStatement != null) {
					IfStatement nextElse= ASTNodes.as(nextStatement, IfStatement.class);

					if (nextElse != null
							&& operandCount.get() + ASTNodes.getNbOperands(nextElse.getExpression()) < ASTNodes.EXCESSIVE_OPERAND_NUMBER) {
						if (match(previousStatement, nextElse.getThenStatement())) {
							operandCount.addAndGet(ASTNodes.getNbOperands(nextElse.getExpression()));
							duplicateIfBlocks.add(nextElse);
							isThenStatement.add(Boolean.TRUE);
							return true;
						}

						if (nextElse.getElseStatement() != null
								&& match(previousStatement, nextElse.getElseStatement())) {
							operandCount.addAndGet(ASTNodes.getNbOperands(nextElse.getExpression()));
							duplicateIfBlocks.add(nextElse);
							isThenStatement.add(Boolean.FALSE);
							return true;
						}
					}
				}

				return false;
			}

			private boolean match(final Statement expectedStatement, final Statement actualStatement) {
				ASTMatcher matcher= new ASTMatcher();
				List<Statement> expectedStatements= ASTNodes.asList(expectedStatement);
				List<Statement> actualStatements= ASTNodes.asList(actualStatement);

				if (expectedStatements.size() != actualStatements.size()) {
					return false;
				}

				for (int codeLine= 0; codeLine < expectedStatements.size(); codeLine++) {
					if (!matcher.safeSubtreeMatch(expectedStatements.get(codeLine), actualStatements.get(codeLine))) {
						return false;
					}
				}

				return true;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.MergeConditionalBlocksCleanup_description, unit,
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

	private static class MergeConditionalBlocksOperation extends CompilationUnitRewriteOperation {
		private final List<IfStatement> duplicateIfBlocks;
		private final List<Boolean> isThenStatement;

		public MergeConditionalBlocksOperation(final List<IfStatement> duplicateIfBlocks, final List<Boolean> isThenStatement) {
			this.duplicateIfBlocks= duplicateIfBlocks;
			this.isThenStatement= isThenStatement;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();

			List<Expression> newConditions= new ArrayList<>(duplicateIfBlocks.size());

			for (int i= 0; i < duplicateIfBlocks.size(); i++) {
				if (isThenStatement.get(i)) {
					newConditions.add(ASTNodeFactory.parenthesizeIfNeeded(ast, ASTNodes.createMoveTarget(rewrite, duplicateIfBlocks.get(i).getExpression())));
				} else {
					newConditions.add(ASTNodeFactory.parenthesizeIfNeeded(ast, ASTNodeFactory.negate(ast, rewrite, duplicateIfBlocks.get(i).getExpression(), true)));
				}
			}

			IfStatement lastBlock= getLast(duplicateIfBlocks);
			Statement remainingStatement= getLast(isThenStatement) ? lastBlock.getElseStatement() : lastBlock.getThenStatement();
			InfixExpression newCondition= ast.newInfixExpression();
			newCondition.setOperator(InfixExpression.Operator.CONDITIONAL_OR);
			newCondition.setLeftOperand(newConditions.remove(0));
			newCondition.setRightOperand(newConditions.remove(0));
			newCondition.extendedOperands().addAll(newConditions);

			rewrite.replace(duplicateIfBlocks.get(0).getExpression(), newCondition, null);

			if (remainingStatement != null) {
				rewrite.replace(duplicateIfBlocks.get(0).getElseStatement(), ASTNodes.createMoveTarget(rewrite, remainingStatement), null);
			} else if (duplicateIfBlocks.get(0).getElseStatement() != null) {
				rewrite.remove(duplicateIfBlocks.get(0).getElseStatement(), null);
			}
		}
	}

	private static <E> E getLast(final List<E> list) {
		return list.get(list.size() - 1);
	}
}
