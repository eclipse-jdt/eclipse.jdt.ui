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
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

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
 * A fix that removes useless indentation when the opposite workflow falls through:
 * <ul>
 * <li>When several blocks fall through, reduce the block with the greatest indentation.</li>
 * </ul>
 */
public class ReduceIndentationCleanUp extends AbstractMultiFix {
	private static final class IndentationVisitor extends ASTVisitor {
		private int indentation;

		public int getIndentation() {
			return indentation;
		}

		@Override
		public boolean visit(final IfStatement visited) {
			computeGreatestIndentation(visited.getThenStatement());

			if (visited.getElseStatement() != null) {
				computeGreatestIndentation(visited.getElseStatement());
			}

			return false;
		}

		@Override
		public boolean visit(final WhileStatement visited) {
			computeGreatestIndentation(visited.getBody());
			return false;
		}

		@Override
		public boolean visit(final DoStatement visited) {
			computeGreatestIndentation(visited.getBody());
			return false;
		}

		@Override
		public boolean visit(final ForStatement visited) {
			computeGreatestIndentation(visited.getBody());
			return false;
		}

		@Override
		public boolean visit(final EnhancedForStatement visited) {
			computeGreatestIndentation(visited.getBody());
			return false;
		}

		@Override
		public boolean visit(final TryStatement visited) {
			computeGreatestIndentation(visited.getBody());

			for (Object object : visited.catchClauses()) {
				CatchClause clause= (CatchClause) object;
				computeGreatestIndentation(clause.getBody());
			}

			if (visited.getFinally() != null) {
				computeGreatestIndentation(visited.getFinally());
			}

			if (visited.getFinally() != null) {
				computeGreatestIndentation(visited.getFinally());
			}

			return false;
		}

		@Override
		public boolean visit(final Block visited) {
			computeGreatestIndentation(visited);
			return false;
		}

		private void computeGreatestIndentation(final Statement statements) {
			for (Statement statement : ASTNodes.asList(statements)) {
				IndentationVisitor visitor= new IndentationVisitor();

				statement.accept(visitor);

				indentation= Math.max(indentation, visitor.getIndentation() + 1);
			}
		}
	}

	public ReduceIndentationCleanUp() {
		this(Collections.emptyMap());
	}

	public ReduceIndentationCleanUp(final Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.REDUCE_INDENTATION);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.REDUCE_INDENTATION)) {
			return new String[] { MultiFixMessages.CodeStyleCleanUp_ReduceIndentation_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();
		bld.append("if (i > 0) {\n"); //$NON-NLS-1$
		bld.append("    return 0;\n"); //$NON-NLS-1$

		if (isEnabled(CleanUpConstants.REDUCE_INDENTATION)) {
			bld.append("}\n"); //$NON-NLS-1$
			bld.append("i = i + 1;\n\n"); //$NON-NLS-1$
		} else {
			bld.append("} else {\n"); //$NON-NLS-1$
			bld.append("    i = i + 1;\n"); //$NON-NLS-1$
			bld.append("}\n"); //$NON-NLS-1$
		}

		return bld.toString();
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.REDUCE_INDENTATION)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final IfStatement visited) {
				if (visited.getElseStatement() != null && !ASTNodes.isInElse(visited)) {
					if (ASTNodes.fallsThrough(visited.getThenStatement())) {
						if (ASTNodes.fallsThrough(visited.getElseStatement())) {
							if (ASTNodes.getNextSiblings(visited).isEmpty()) {
								int thenIndentation= getIndentation(visited.getThenStatement());
								int elseIndentation= getIndentation(visited.getElseStatement());

								if (thenIndentation <= elseIndentation || visited.getElseStatement() instanceof IfStatement) {
									rewriteOperations.add(new ReduceIndentationElseOperation(visited));
								} else {
									rewriteOperations.add(new ReduceIndentationThenOperation(visited));
								}

								return false;
							}
						} else if (!ASTNodes.hasVariableConflict(visited, visited.getElseStatement())) {
							rewriteOperations.add(new ReduceIndentationElseOperation(visited));
							return false;
						}
					} else if (ASTNodes.fallsThrough(visited.getElseStatement())
							&& !ASTNodes.hasVariableConflict(visited, visited.getThenStatement())
							&& !(visited.getElseStatement() instanceof IfStatement)) {
						rewriteOperations.add(new ReduceIndentationThenOperation(visited));
						return false;
					}
				}

				return true;
			}

			private int getIndentation(final Statement statementInIf) {
				IndentationVisitor visitor= new IndentationVisitor();
				statementInIf.accept(visitor);
				return visitor.getIndentation() + (statementInIf instanceof Block ? -1 : 0);
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.CodeStyleCleanUp_ReduceIndentation_description, unit,
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

	private static class ReduceIndentationThenOperation extends CompilationUnitRewriteOperation {
		private final IfStatement visited;

		public ReduceIndentationThenOperation(final IfStatement visited) {
			this.visited= visited;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.CodeStyleCleanUp_ReduceIndentation_description, cuRewrite);

			List<Statement> statementsToMove= ASTNodes.asList(visited.getThenStatement());

			ASTNode moveTarget= null;
			if (statementsToMove.size() == 1) {
				moveTarget= ASTNodes.createMoveTarget(rewrite, statementsToMove.get(0));
			} else if (!statementsToMove.isEmpty()) {
				ListRewrite listRewrite= rewrite.getListRewrite(statementsToMove.get(0).getParent(), (ChildListPropertyDescriptor) statementsToMove.get(0).getLocationInParent());
				moveTarget= listRewrite.createMoveTarget(statementsToMove.get(0), statementsToMove.get(statementsToMove.size() - 1));
			}

			rewrite.replace(visited.getExpression(), ASTNodes.getUnparenthesedExpression(ASTNodeFactory.negate(ast, rewrite, visited.getExpression(), true)), group);
			ASTNodes.replaceButKeepComment(rewrite, visited.getThenStatement(), ASTNodes.createMoveTarget(rewrite, visited.getElseStatement()), group);

			if (!statementsToMove.isEmpty()) {
				if (ASTNodes.canHaveSiblings(visited)) {
					ListRewrite targetListRewrite= rewrite.getListRewrite(visited.getParent(), (ChildListPropertyDescriptor) visited.getLocationInParent());
					targetListRewrite.insertAfter(moveTarget, visited, group);
					rewrite.remove(visited.getElseStatement(), group);
				} else {
					Block newBlock= ast.newBlock();
					newBlock.statements().add(ASTNodes.createMoveTarget(rewrite, visited));
					newBlock.statements().add(moveTarget);
					ASTNodes.replaceButKeepComment(rewrite, visited, newBlock, group);
				}
			}
		}
	}

	private static class ReduceIndentationElseOperation extends CompilationUnitRewriteOperation {
		private final IfStatement visited;

		public ReduceIndentationElseOperation(final IfStatement visited) {
			this.visited= visited;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.CodeStyleCleanUp_ReduceIndentation_description, cuRewrite);

			List<Statement> statementsToMove= ASTNodes.asList(visited.getElseStatement());

			ASTNode moveTarget= null;
			if (statementsToMove.size() == 1) {
				moveTarget= ASTNodes.createMoveTarget(rewrite, statementsToMove.get(0));
			} else if (!statementsToMove.isEmpty()) {
				ListRewrite listRewrite= rewrite.getListRewrite(statementsToMove.get(0).getParent(), (ChildListPropertyDescriptor) statementsToMove.get(0).getLocationInParent());
				moveTarget= listRewrite.createMoveTarget(statementsToMove.get(0), statementsToMove.get(statementsToMove.size() - 1));
			}

			if (!statementsToMove.isEmpty()) {
				if (ASTNodes.canHaveSiblings(visited)) {
					ListRewrite targetListRewrite= rewrite.getListRewrite(visited.getParent(), (ChildListPropertyDescriptor) visited.getLocationInParent());
					targetListRewrite.insertAfter(moveTarget, visited, group);
				} else {
					Block newBlock= ast.newBlock();
					newBlock.statements().add(ASTNodes.createMoveTarget(rewrite, visited));
					newBlock.statements().add(moveTarget);

					ASTNodes.replaceButKeepComment(rewrite, visited, newBlock, group);
				}
			}

			rewrite.remove(visited.getElseStatement(), group);
		}
	}
}
