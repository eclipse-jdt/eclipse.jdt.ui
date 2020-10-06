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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

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
 * A fix that removes useless continue:
 * <ul>
 * <li>A continue statement at the end of a loop is removed,</li>
 * <li>A continue statement at the end of a control statement is removed if the control statement is at the end of a loop,</li>
 * <li>A continue statement is kept if it has a label.</li>
 * </ul>
 */
public class UselessContinueCleanUp extends AbstractMultiFix {
	public UselessContinueCleanUp() {
		this(Collections.emptyMap());
	}

	public UselessContinueCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.REMOVE_USELESS_CONTINUE);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.REMOVE_USELESS_CONTINUE)) {
			return new String[] { MultiFixMessages.UselessContinueCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();
		bld.append("for (String text : texts) {\n"); //$NON-NLS-1$

		if (isEnabled(CleanUpConstants.REMOVE_USELESS_CONTINUE)) {
			bld.append("}\n\n"); //$NON-NLS-1$
		} else {
			bld.append("    continue;\n"); //$NON-NLS-1$
			bld.append("}\n"); //$NON-NLS-1$
		}

		return bld.toString();
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.REMOVE_USELESS_CONTINUE)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final Block node) {
				ContinueInBlockVisitor continueInBlockVisitor= new ContinueInBlockVisitor(node);
				node.accept(continueInBlockVisitor);
				return continueInBlockVisitor.result;
			}

			final class ContinueInBlockVisitor extends ASTVisitor {
				private final Block startNode;
				private boolean result= true;

				public ContinueInBlockVisitor(final Block startNode) {
					this.startNode= startNode;
				}

				@Override
				public boolean visit(final Block node) {
					return startNode == node;
				}

				@Override
				public boolean visit(final ContinueStatement node) {
					if (result && node.getLabel() == null && isLastStatementInLoop(node)) {
						rewriteOperations.add(new UselessContinueOperation(node, startNode));

						result= false;
						return false;
					}

					return true;
				}

				private boolean isLastStatementInLoop(final Statement node) {
					Statement nextStatement= ASTNodes.getNextStatement(node);

					if (nextStatement == null) {
						ASTNode parent= node.getParent();

						if (parent instanceof WhileStatement
								|| parent instanceof EnhancedForStatement
								|| parent instanceof ForStatement
								|| parent instanceof DoStatement) {
							return true;
						}

						if (parent instanceof MethodDeclaration) {
							return false;
						}

						if (parent instanceof Statement) {
							return isLastStatementInLoop((Statement) parent);
						}
					}

					return false;
				}
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.UselessContinueCleanUp_description, unit,
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

	private static class UselessContinueOperation extends CompilationUnitRewriteOperation {
		private final ContinueStatement node;
		private final Block block;

		public UselessContinueOperation(final ContinueStatement node, final Block block) {
			this.node= node;
			this.block= block;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.UselessContinueCleanUp_description, cuRewrite);

			if (block.getLocationInParent() == IfStatement.ELSE_STATEMENT_PROPERTY
					&& ASTNodes.asList(block).size() == 1
					&& node.equals(ASTNodes.asList(block).get(0))) {
				rewrite.remove(block, group);
			} else if (ASTNodes.canHaveSiblings(node) || node.getLocationInParent() == IfStatement.ELSE_STATEMENT_PROPERTY) {
				rewrite.remove(node, group);
			} else {
				rewrite.replace(node, ast.newBlock(), group);
			}
		}
	}
}
