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
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
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
 * A fix that removes redundant null checks:
 * <ul>
 * <li>Before <code>equals()</code> method,</li>
 * <li>Before <code>instanceof</code> expression,</li>
 * <li>It also checks the expression is passive.</li>
 * </ul>
 */
public class EvaluateNullableCleanUp extends AbstractMultiFix implements ICleanUpFix {
	public EvaluateNullableCleanUp() {
		this(Collections.emptyMap());
	}

	public EvaluateNullableCleanUp(final Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.EVALUATE_NULLABLE);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.EVALUATE_NULLABLE)) {
			return new String[] { MultiFixMessages.EvaluateNullableCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.EVALUATE_NULLABLE)) {
			return "" //$NON-NLS-1$
					+ "boolean b1 = \"\".equals(s);\n" //$NON-NLS-1$
					+ "boolean b2 = \"\".equalsIgnoreCase(s);\n" //$NON-NLS-1$
					+ "boolean b3 = s instanceof String;\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "boolean b1 = s != null && \"\".equals(s);\n" //$NON-NLS-1$
				+ "boolean b2 = null != s && \"\".equalsIgnoreCase(s);\n" //$NON-NLS-1$
				+ "boolean b3 = s != null && s instanceof String;\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.EVALUATE_NULLABLE)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final InfixExpression visited) {
				if (ASTNodes.hasOperator(visited, InfixExpression.Operator.CONDITIONAL_AND)) {
					List<Expression> operands= ASTNodes.allOperands(visited);

					for (int i= 0; i < operands.size() - 1; i++) {
						Expression nullCheckedExpression= ASTNodes.getNullCheckedExpression(operands.get(i));

						if (nullCheckedExpression != null && isNullCheckRedundant(nullCheckedExpression, operands.get(i + 1))) {
							operands.remove(i);

							rewriteOperations.add(new EvaluateNullableOperation(visited, operands));
							return false;
						}
					}
				}

				return true;
			}

			/**
			 * The previous null check is redundant if:
			 * <ul>
			 * <li>the null checked expression is reused in an <code>instanceof</code> expression</li>
			 * <li>the null checked expression is reused in an expression checking for
			 * object equality against an expression that resolves to a non null
			 * constant</li>
			 * </ul>
			 *
			 * @param nullCheckedExpression The null checked expression
			 * @param nextExpression The expression of evaluation
			 * @return True if null check is redundant
			 */
			private boolean isNullCheckRedundant(final Expression nullCheckedExpression, final Expression nextExpression) {
				if (nullCheckedExpression != null
						&& ASTNodes.isPassive(nullCheckedExpression)) {
					if (nextExpression instanceof InstanceofExpression) {
						Expression leftOperand= ((InstanceofExpression) nextExpression).getLeftOperand();
						return ASTNodes.match(leftOperand, nullCheckedExpression);
					}

					if (nextExpression instanceof MethodInvocation) {
						MethodInvocation methodInvocation= (MethodInvocation) nextExpression;

						if (methodInvocation.getExpression() != null
								&& methodInvocation.getExpression().resolveConstantExpressionValue() != null
								&& methodInvocation.arguments().size() == 1
								&& ASTNodes.match((Expression) methodInvocation.arguments().get(0), nullCheckedExpression)) {
							// Did we invoke java.lang.Object.equals() or
							// java.lang.String.equalsIgnoreCase()?
							return ASTNodes.usesGivenSignature(methodInvocation, Object.class.getCanonicalName(), "equals", Object.class.getCanonicalName()) //$NON-NLS-1$
									|| ASTNodes.usesGivenSignature(methodInvocation, String.class.getCanonicalName(), "equalsIgnoreCase", String.class.getCanonicalName()); //$NON-NLS-1$
						}
					}
				}

				return false;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.EvaluateNullableCleanUp_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[0]));
	}

	@Override
	public CompilationUnitChange createChange(final IProgressMonitor progressMonitor) throws CoreException {
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

	private static class EvaluateNullableOperation extends CompilationUnitRewriteOperation {
		private final InfixExpression visited;
		private final List<Expression> operands;

		public EvaluateNullableOperation(final InfixExpression visited, final List<Expression> operands) {
			this.visited= visited;
			this.operands= operands;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.EvaluateNullableCleanUp_description, cuRewrite);

			if (operands.size() == 1) {
				ASTNodes.replaceButKeepComment(rewrite, visited, ASTNodes.createMoveTarget(rewrite, operands.get(0)), group);
			} else {
				InfixExpression newInfixExpression= ast.newInfixExpression();
				newInfixExpression.setLeftOperand(ASTNodes.createMoveTarget(rewrite, operands.remove(0)));
				newInfixExpression.setOperator(visited.getOperator());
				newInfixExpression.setRightOperand(ASTNodes.createMoveTarget(rewrite, operands.remove(0)));
				newInfixExpression.extendedOperands().addAll(ASTNodes.createMoveTarget(rewrite, operands));

				ASTNodes.replaceButKeepComment(rewrite, visited, newInfixExpression, group);
			}
		}
	}
}
