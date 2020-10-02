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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTSemanticMatcher;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that merges blocks that end with a jump statement into the following same code:
 * <ul>
 * <li><code>return</code>, <code>continue</code>, <code>break</code> and <code>throw</code> are jump statements.</li>
 * <li>The block can be a lone block, a <code>if</code>, <code>else</code>, <code>catch</code> or <code>finally</code>.</li>
 * <li>The block cannot be a loop statement.</li>
 * <li>The statements must be the same and must reference to the same variables.</li>
 * <li>The block can start with other statements. Such statements will be kept.</li>
 * </ul>
 */
public class RedundantFallingThroughBlockEndCleanUp extends AbstractMultiFix implements ICleanUpFix {
	public RedundantFallingThroughBlockEndCleanUp() {
		this(Collections.emptyMap());
	}

	public RedundantFallingThroughBlockEndCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.REDUNDANT_FALLING_THROUGH_BLOCK_END);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.REDUNDANT_FALLING_THROUGH_BLOCK_END)) {
			return new String[] { MultiFixMessages.RedundantFallingThroughBlockEndCleanup_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();
		bld.append("if (0 < i) {\n"); //$NON-NLS-1$
		bld.append("  System.out.println(\"Doing something\");\n"); //$NON-NLS-1$

		if (!isEnabled(CleanUpConstants.REDUNDANT_FALLING_THROUGH_BLOCK_END)) {
			bld.append("  return i + 10;\n"); //$NON-NLS-1$
		}

		bld.append("}\n"); //$NON-NLS-1$
		bld.append("return i + 10;\n"); //$NON-NLS-1$

		if (isEnabled(CleanUpConstants.REDUNDANT_FALLING_THROUGH_BLOCK_END)) {
			bld.append("\n"); //$NON-NLS-1$
		}

		return bld.toString();
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.REDUNDANT_FALLING_THROUGH_BLOCK_END)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final Block node) {
				BlocksAndFollowingCodeVisitor blocksAndFollowingCodeVisitor= new BlocksAndFollowingCodeVisitor(node);
				node.accept(blocksAndFollowingCodeVisitor);
				return blocksAndFollowingCodeVisitor.result;
			}

			final class BlocksAndFollowingCodeVisitor extends ASTVisitor {
				private final Block startNode;
				private boolean result= true;

				public BlocksAndFollowingCodeVisitor(final Block startNode) {
					this.startNode= startNode;
				}

				@Override
				public boolean visit(final Block node) {
					return startNode == node;
				}

				@Override
				public boolean visit(final TryStatement node) {
					return visitStatement(node);
				}

				@Override
				public boolean visit(final IfStatement node) {
					return visitStatement(node);
				}

				private boolean visitStatement(final Statement node) {
					if (result) {
						List<Statement> redundantStatements= new ArrayList<>();
						collectStatements(node, redundantStatements);
						return maybeRemoveRedundantCode(node, redundantStatements);
					}

					return true;
				}

				private void collectStatements(final Statement node, final List<Statement> redundantStatements) {
					if (node != null) {
						TryStatement ts= ASTNodes.as(node, TryStatement.class);
						IfStatement is= ASTNodes.as(node, IfStatement.class);

						if (ts != null && ts.getFinally() == null) {
							List<CatchClause> catchClauses= ts.catchClauses();

							for (CatchClause catchClause : catchClauses) {
								doCollectStatements(catchClause.getBody(), redundantStatements);
							}
						} else if (is != null) {
							doCollectStatements(is.getThenStatement(), redundantStatements);
							doCollectStatements(is.getElseStatement(), redundantStatements);
						}
					}
				}

				private void doCollectStatements(final Statement node, final List<Statement> redundantStatements) {
					if (node != null) {
						redundantStatements.add(node);
						List<Statement> statements= ASTNodes.asList(node);

						if (!isEmpty(statements)) {
							collectStatements(statements.get(statements.size() - 1), redundantStatements);
						}
					}
				}

				private boolean maybeRemoveRedundantCode(final Statement node, final List<Statement> redundantStatements) {
					List<Statement> referenceStatements= ASTNodes.getNextSiblings(node);

					if (redundantStatements.isEmpty() || isEmpty(referenceStatements)) {
						return true;
					}

					for (Statement redundantStatement : redundantStatements) {
						List<Statement> statements= ASTNodes.asList(redundantStatement);

						if (isEmpty(statements) || !ASTNodes.fallsThrough(statements.get(statements.size() - 1))) {
							continue;
						}

						Statement lastStatement= null;

						List<Statement> stmtsToCompare;
						if (statements.size() > referenceStatements.size()) {
							stmtsToCompare= statements.subList(statements.size() - referenceStatements.size(),
									statements.size());
						} else {
							stmtsToCompare= new ArrayList<>(statements);
						}

						ASTSemanticMatcher matcher= new ASTSemanticMatcher() {
							@Override
							public boolean match(final SimpleName simpleName, final Object other) {
								return other instanceof SimpleName
										&& simpleName.resolveBinding() != null
										&& ((SimpleName) other).resolveBinding() != null
										&& ((simpleName.resolveBinding().getKind() != IBinding.VARIABLE && ((SimpleName) other).resolveBinding().getKind() != IBinding.VARIABLE) || ASTNodes.isSameVariable(simpleName, (SimpleName) other))
										&& super.match(simpleName, other);
							}
						};
						boolean match= ASTNodes.match(matcher, referenceStatements, stmtsToCompare);

						if (!match) {
							lastStatement= statements.get(statements.size() - 1);
							ReturnStatement returnStatement= ASTNodes.as(lastStatement, ReturnStatement.class);
							ContinueStatement continueStatement= ASTNodes.as(lastStatement, ContinueStatement.class);

							if ((isIn(node, MethodDeclaration.class)
									&& returnStatement != null
									&& returnStatement.getExpression() == null)
									|| (isIn(node, EnhancedForStatement.class, ForStatement.class, WhileStatement.class, DoStatement.class)
											&& continueStatement != null
											&& continueStatement.getLabel() == null)) {
								if (statements.size() > referenceStatements.size() + 1) {
									stmtsToCompare= statements.subList(statements.size() - referenceStatements.size() - 1,
											statements.size() - 1);
								} else {
									stmtsToCompare= statements.subList(0, statements.size() - 1);
								}

								match= ASTNodes.match(matcher, referenceStatements, stmtsToCompare);
							}
						}

						if (match) {
							rewriteOperations.add(new RedundantFallingThroughBlockEndOperation(redundantStatement, stmtsToCompare, lastStatement));

							result= false;
						}
					}

					return result;
				}

				private boolean isEmpty(final Collection<?> collection) {
					return collection == null || collection.isEmpty();
				}

				private boolean isIn(final Statement node, final Class<?>... domClasses) {
					for (Class<?> domClass : domClasses) {
						if (node.getParent().getClass().isAssignableFrom(domClass)
								|| (node.getParent() instanceof Block && node.getParent().getParent().getClass().isAssignableFrom(domClass))) {
							return true;
						}
					}

					return false;
				}
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.RedundantFallingThroughBlockEndCleanup_description, unit,
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

	private static class RedundantFallingThroughBlockEndOperation extends CompilationUnitRewriteOperation {
		private final Statement redundantStatement;
		private final List<Statement> stmtsToCompare;
		private final Statement lastStatement;

		public RedundantFallingThroughBlockEndOperation(final Statement redundantStatement, final List<Statement> stmtsToCompare, final Statement lastStatement) {
			this.redundantStatement= redundantStatement;
			this.stmtsToCompare= stmtsToCompare;
			this.lastStatement= lastStatement;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.RedundantFallingThroughBlockEndCleanup_description, cuRewrite);

			if (redundantStatement instanceof Block) {
				for (Statement stmtToCompare : stmtsToCompare) {
					rewrite.remove(stmtToCompare, group);
				}

				if (lastStatement != null) {
					rewrite.remove(lastStatement, group);
				}
			} else if (redundantStatement.getLocationInParent() == IfStatement.ELSE_STATEMENT_PROPERTY) {
				rewrite.remove(redundantStatement, group);
			} else {
				rewrite.replace(redundantStatement, ast.newBlock(), group);
			}
		}
	}
}
