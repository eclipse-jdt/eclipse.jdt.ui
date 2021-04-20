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

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
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
		if (isEnabled(CleanUpConstants.MERGE_CONDITIONAL_BLOCKS)) {
			return "" //$NON-NLS-1$
					+ "if (isValid || (i != 1)) {\n" //$NON-NLS-1$
					+ "    System.out.println(\"Duplicate\");\n" //$NON-NLS-1$
					+ "} else {\n" //$NON-NLS-1$
					+ "    System.out.println(\"Different\");\n" //$NON-NLS-1$
					+ "}\n\n\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "if (isValid) {\n" //$NON-NLS-1$
				+ "    System.out.println(\"Duplicate\");\n" //$NON-NLS-1$
				+ "} else if (i == 1) {\n" //$NON-NLS-1$
				+ "    System.out.println(\"Different\");\n" //$NON-NLS-1$
				+ "} else {\n" //$NON-NLS-1$
				+ "    System.out.println(\"Duplicate\");\n" //$NON-NLS-1$
				+ "}\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.MERGE_CONDITIONAL_BLOCKS)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final IfStatement visited) {
				if (visited.getElseStatement() != null) {
					IfStatement innerIf= ASTNodes.as(visited.getThenStatement(), IfStatement.class);

					if (innerIf != null
							&& innerIf.getElseStatement() != null
							&& !ASTNodes.asList(visited.getElseStatement()).isEmpty()
							&& ASTNodes.getNbOperands(visited.getExpression()) + ASTNodes.getNbOperands(innerIf.getExpression()) < ASTNodes.EXCESSIVE_OPERAND_NUMBER) {
						if (ASTNodes.match(visited.getElseStatement(), innerIf.getElseStatement())) {
							rewriteOperations.add(new InnerIfOperation(visited, innerIf, true));
							return false;
						}

						if (ASTNodes.match(visited.getElseStatement(), innerIf.getThenStatement())) {
							rewriteOperations.add(new InnerIfOperation(visited, innerIf, false));
							return false;
						}
					}

					List<IfStatement> duplicateIfBlocks= new ArrayList<>(4);
					List<Boolean> isThenStatement= new ArrayList<>(4);
					AtomicInteger operandCount= new AtomicInteger(ASTNodes.getNbOperands(visited.getExpression()));
					duplicateIfBlocks.add(visited);
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
						if (ASTNodes.match(previousStatement, nextElse.getThenStatement())) {
							operandCount.addAndGet(ASTNodes.getNbOperands(nextElse.getExpression()));
							duplicateIfBlocks.add(nextElse);
							isThenStatement.add(Boolean.TRUE);
							return true;
						}

						if (nextElse.getElseStatement() != null
								&& ASTNodes.match(previousStatement, nextElse.getElseStatement())) {
							operandCount.addAndGet(ASTNodes.getNbOperands(nextElse.getExpression()));
							duplicateIfBlocks.add(nextElse);
							isThenStatement.add(Boolean.FALSE);
							return true;
						}
					}
				}

				return false;
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

	private static class InnerIfOperation extends CompilationUnitRewriteOperation {
		private final IfStatement visited;
		private final IfStatement innerIf;
		private final boolean isInnerMainFirst;

		public InnerIfOperation(final IfStatement visited, final IfStatement innerIf,
				final boolean isInnerMainFirst) {
			this.visited= visited;
			this.innerIf= innerIf;
			this.isInnerMainFirst= isInnerMainFirst;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.MergeConditionalBlocksCleanup_description_inner_if, cuRewrite);

			InfixExpression newInfixExpression= ast.newInfixExpression();

			Expression outerCondition;
			if (isInnerMainFirst) {
				outerCondition= ASTNodes.createMoveTarget(rewrite, visited.getExpression());
			} else {
				outerCondition= ASTNodeFactory.negate(ast, rewrite, visited.getExpression(), true);
			}

			newInfixExpression.setLeftOperand(ASTNodeFactory.parenthesizeIfNeeded(ast, outerCondition));
			newInfixExpression.setOperator(isInnerMainFirst ? InfixExpression.Operator.CONDITIONAL_AND
					: InfixExpression.Operator.CONDITIONAL_OR);
			newInfixExpression.setRightOperand(ASTNodeFactory.parenthesizeIfNeeded(ast,
					ASTNodes.createMoveTarget(rewrite, innerIf.getExpression())));

			ASTNodes.replaceButKeepComment(rewrite, innerIf.getExpression(), newInfixExpression, group);
			ASTNodes.replaceButKeepComment(rewrite, visited, ASTNodes.createMoveTarget(rewrite, innerIf), group);
		}
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
			TextEditGroup group= createTextEditGroup(MultiFixMessages.MergeConditionalBlocksCleanup_description_if_suite, cuRewrite);

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
			ASTNode node= duplicateIfBlocks.get(0).getExpression();

			ASTNodes.replaceButKeepComment(rewrite, node, newCondition, group);

			if (remainingStatement != null) {
				ASTNode node1= duplicateIfBlocks.get(0).getElseStatement();
				ASTNode replacement= ASTNodes.createMoveTarget(rewrite, remainingStatement);
				ASTNodes.replaceButKeepComment(rewrite, node1, replacement, group);
			} else if (duplicateIfBlocks.get(0).getElseStatement() != null) {
				rewrite.remove(duplicateIfBlocks.get(0).getElseStatement(), group);
			}
		}
	}

	private static <E> E getLast(final List<E> list) {
		return list.get(list.size() - 1);
	}
}
