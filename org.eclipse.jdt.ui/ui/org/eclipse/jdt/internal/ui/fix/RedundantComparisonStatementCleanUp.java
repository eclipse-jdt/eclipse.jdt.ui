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
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

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
 * A fix that removes useless bad value checks before assignments or return statements.
 * Such useless bad value checks are comparing an expression against bad value,
 * then either assigning bad value or the expression depending on the result of the bad value check.
 * It is simpler to directly assign the expression:
 * <ul>
 * <li>The expression should be passive.</li>
 * <li>The excluded value should be hard coded.</li>
 * </ul>
 */
public class RedundantComparisonStatementCleanUp extends AbstractMultiFix implements ICleanUpFix {
	public RedundantComparisonStatementCleanUp() {
		this(Collections.emptyMap());
	}

	public RedundantComparisonStatementCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.REMOVE_REDUNDANT_COMPARISON_STATEMENT);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.REMOVE_REDUNDANT_COMPARISON_STATEMENT)) {
			return new String[] { MultiFixMessages.RedundantComparisonStatementCleanup_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.REMOVE_REDUNDANT_COMPARISON_STATEMENT)) {
			return "return i;\n\n\n\n\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "if (i != 123) {\n" //$NON-NLS-1$
				+ " return i;\n" //$NON-NLS-1$
				+ "} else {\n" //$NON-NLS-1$
				+ " return 123;\n" //$NON-NLS-1$
				+ "}\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.REMOVE_REDUNDANT_COMPARISON_STATEMENT)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final Block node) {
				IfAndReturnVisitor ifAndReturnVisitor= new IfAndReturnVisitor(node);
				node.accept(ifAndReturnVisitor);
				return ifAndReturnVisitor.result;
			}

			final class IfAndReturnVisitor extends ASTVisitor {
				private final Block startNode;
				private boolean result= true;

				public IfAndReturnVisitor(final Block startNode) {
					this.startNode= startNode;
				}

				@Override
				public boolean visit(final Block node) {
					return startNode == node;
				}

				@Override
				public boolean visit(final IfStatement node) {
					InfixExpression condition= ASTNodes.as(node.getExpression(), InfixExpression.class);
					Statement thenStatement= getThenStatement(node);
					Statement elseStatement= getElseStatement(node, thenStatement);

					if (result
							&& thenStatement != null
							&& elseStatement != null
							&& condition != null
							&& !condition.hasExtendedOperands()
							&& ASTNodes.hasOperator(condition, InfixExpression.Operator.EQUALS, InfixExpression.Operator.NOT_EQUALS)) {
						boolean isEqual= ASTNodes.hasOperator(condition, InfixExpression.Operator.EQUALS);
						Assignment thenAssignment= ASTNodes.asExpression(thenStatement, Assignment.class);
						Assignment elseAssignment= ASTNodes.asExpression(elseStatement, Assignment.class);

						if (ASTNodes.hasOperator(thenAssignment, Assignment.Operator.ASSIGN)
								&& ASTNodes.hasOperator(elseAssignment, Assignment.Operator.ASSIGN)
								&& ASTNodes.match(thenAssignment.getLeftHandSide(), elseAssignment.getLeftHandSide())) {
							if (isEqual) {
								return maybeReplace(node, condition, thenAssignment.getRightHandSide(), elseAssignment.getRightHandSide(), elseStatement, null);
							}

							return maybeReplace(node, condition, elseAssignment.getRightHandSide(), thenAssignment.getRightHandSide(), thenStatement, null);
						}

						ReturnStatement thenReturnStatement= ASTNodes.as(thenStatement, ReturnStatement.class);
						ReturnStatement elseReturnStatement= ASTNodes.as(elseStatement, ReturnStatement.class);

						if (thenReturnStatement != null && elseReturnStatement != null) {
							if (isEqual) {
								return maybeReplace(node, condition, thenReturnStatement.getExpression(), elseReturnStatement.getExpression(), elseReturnStatement, thenReturnStatement);
							}

							return maybeReplace(node, condition, elseReturnStatement.getExpression(), thenReturnStatement.getExpression(), thenReturnStatement, elseReturnStatement);
						}
					}

					return true;
				}

				private Statement getThenStatement(final IfStatement node) {
					List<Statement> thenStatements= ASTNodes.asList(node.getThenStatement());

					if (thenStatements.size() == 1) {
						return thenStatements.get(0);
					}

					return null;
				}

				private Statement getElseStatement(final IfStatement node, final Statement thenStatement) {
					List<Statement> elseStatements= ASTNodes.asList(node.getElseStatement());

					if (elseStatements.size() == 1) {
						return elseStatements.get(0);
					}

					if (ASTNodes.is(thenStatement, ReturnStatement.class)) {
						return ASTNodes.getNextSibling(node);
					}

					return null;
				}

				private boolean maybeReplace(final IfStatement node, final InfixExpression condition, final Expression hardCodedExpression, final Expression valuedExpression,
						final Statement statementToMove, final ReturnStatement returnToRemove) {
					if (ASTNodes.isHardCoded(hardCodedExpression)
							&& ASTNodes.isPassiveWithoutFallingThrough(hardCodedExpression)
							&& ASTNodes.isPassive(valuedExpression)
							&& ((ASTNodes.match(condition.getRightOperand(), hardCodedExpression) && ASTNodes.match(condition.getLeftOperand(), valuedExpression))
									|| (ASTNodes.match(condition.getRightOperand(), valuedExpression) && ASTNodes.match(condition.getLeftOperand(), hardCodedExpression)))) {
						rewriteOperations.add(new RedundantComparisonStatementOperation(node, statementToMove, returnToRemove));
						result= false;
						return false;
					}

					return true;
				}
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.RedundantComparisonStatementCleanup_description, unit,
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

	private static class RedundantComparisonStatementOperation extends CompilationUnitRewriteOperation {
		private final IfStatement node;
		private final Statement toMove;
		private final ReturnStatement toRemove;

		public RedundantComparisonStatementOperation(final IfStatement node, final Statement toMove, final ReturnStatement toRemove) {
			this.node= node;
			this.toMove= toMove;
			this.toRemove= toRemove;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.RedundantComparisonStatementCleanup_description, cuRewrite);

			ASTNodes.replaceButKeepComment(rewrite, node, ASTNodes.createMoveTarget(rewrite, toMove), group);

			if (toRemove != null) {
				rewrite.remove(toRemove, group);
			}
		}
	}
}
