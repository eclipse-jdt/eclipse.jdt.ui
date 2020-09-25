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
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
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
 * A fix that uses lazy logical operator when possible:
 * <ul>
 * <li>Replaces & by &&,</li>
 * <li>Replaces | by ||.</li>
 * </ul>
 */
public class LazyLogicalCleanUp extends AbstractMultiFix {
	public LazyLogicalCleanUp() {
		this(Collections.emptyMap());
	}

	public LazyLogicalCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.USE_LAZY_LOGICAL_OPERATOR);
		Map<String, String> requiredOptions= null;
		return new CleanUpRequirements(requireAST, false, false, requiredOptions);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.USE_LAZY_LOGICAL_OPERATOR)) {
			return new String[] { MultiFixMessages.CodeStyleCleanUp_LazyLogical_description };
		}
		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();
		if (isEnabled(CleanUpConstants.USE_LAZY_LOGICAL_OPERATOR)) {
			bld.append("boolean b = isEnabled || isValid;\n"); //$NON-NLS-1$
			bld.append("boolean b2 = isEnabled && isValid;\n"); //$NON-NLS-1$
		} else {
			bld.append("boolean b = isEnabled | isValid;\n"); //$NON-NLS-1$
			bld.append("boolean b2 = isEnabled & isValid;\n"); //$NON-NLS-1$
		}

		return bld.toString();
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.USE_LAZY_LOGICAL_OPERATOR)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(InfixExpression node) {
				if (ASTNodes.hasOperator(node, InfixExpression.Operator.AND, InfixExpression.Operator.OR)) {
					List<Expression> allOperands= ASTNodes.allOperands(node);
					boolean isFirst= true;

					for (Expression expression : allOperands) {
						if (!ASTNodes.hasType(expression, boolean.class.getSimpleName(), Boolean.class.getCanonicalName())) {
							return true;
						}

						if (isFirst) {
							isFirst= false;
						} else if (!ASTNodes.isPassiveWithoutFallingThrough(expression)) {
							return true;
						}
					}

					rewriteOperations.add(new LazyLogicalInInfixExpressionOperation(node));
					return false;
				}

				return true;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.CodeStyleCleanUp_LazyLogical_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[rewriteOperations.size()]));
	}

	@Override
	public boolean canFix(ICompilationUnit compilationUnit, IProblemLocation problem) {
		return false;
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit, IProblemLocation[] problems) throws CoreException {
		return null;
	}

	private static class LazyLogicalInInfixExpressionOperation extends CompilationUnitRewriteOperation {
		private final InfixExpression node;

		public LazyLogicalInInfixExpressionOperation(InfixExpression node) {
			this.node= node;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.CodeStyleCleanUp_LazyLogical_description, cuRewrite);
			List<Expression> allOperands= ASTNodes.allOperands(node);
			List<Expression> copyOfOperands= new ArrayList<>(allOperands.size());

			for (Expression expression : allOperands) {
				copyOfOperands.add((Expression) rewrite.createMoveTarget(expression));
			}

			InfixExpression newIe= ast.newInfixExpression();
			newIe.setOperator(ASTNodes.hasOperator(node, InfixExpression.Operator.AND) ? InfixExpression.Operator.CONDITIONAL_AND : InfixExpression.Operator.CONDITIONAL_OR);
			newIe.setLeftOperand(copyOfOperands.remove(0));
			newIe.setRightOperand(copyOfOperands.remove(0));
			newIe.extendedOperands().addAll(copyOfOperands);

			ASTNodes.replaceButKeepComment(rewrite, this.node, newIe, group);
		}
	}
}
