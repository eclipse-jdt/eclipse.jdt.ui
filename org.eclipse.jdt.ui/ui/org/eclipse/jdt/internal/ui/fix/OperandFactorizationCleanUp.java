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

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
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
 * A fix that replaces (X && Y) || (X && Z) by (X && (Y || Z)):
 * <ul>
 * <li>The operands must be passive and primitive.</li>
 * </ul>
 */
public class OperandFactorizationCleanUp extends AbstractMultiFix {
	public OperandFactorizationCleanUp() {
		this(Collections.emptyMap());
	}

	public OperandFactorizationCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.OPERAND_FACTORIZATION);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.OPERAND_FACTORIZATION)) {
			return new String[] { MultiFixMessages.OperandFactorizationCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.OPERAND_FACTORIZATION)) {
			return "boolean newBoolean = (repeatedBoolean && (isValid || isActive));\n"; //$NON-NLS-1$
		}

		return "boolean newBoolean = repeatedBoolean && isValid || repeatedBoolean && isActive;\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.OPERAND_FACTORIZATION)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final InfixExpression visited) {
				if (!visited.hasExtendedOperands()
						&& ASTNodes.hasOperator(visited, InfixExpression.Operator.CONDITIONAL_OR, InfixExpression.Operator.OR)) {
					InfixExpression firstCondition= ASTNodes.as(visited.getLeftOperand(), InfixExpression.class);
					InfixExpression secondCondition= ASTNodes.as(visited.getRightOperand(), InfixExpression.class);

					if (firstCondition != null
							&& secondCondition != null
							&& !firstCondition.hasExtendedOperands()
							&& !secondCondition.hasExtendedOperands()
							&& ASTNodes.hasOperator(firstCondition, InfixExpression.Operator.CONDITIONAL_AND, InfixExpression.Operator.AND)
							&& ASTNodes.hasOperator(secondCondition, InfixExpression.Operator.CONDITIONAL_AND, InfixExpression.Operator.AND)
							&& ASTNodes.hasType(firstCondition.getLeftOperand(), boolean.class.getCanonicalName(), int.class.getCanonicalName(), long.class.getCanonicalName(),
									short.class.getCanonicalName(), char.class.getCanonicalName(), byte.class.getCanonicalName())
							&& ASTNodes.hasType(firstCondition.getRightOperand(), boolean.class.getCanonicalName(), int.class.getCanonicalName(), long.class.getCanonicalName(),
									short.class.getCanonicalName(), char.class.getCanonicalName(), byte.class.getCanonicalName())
							&& ASTNodes.hasType(secondCondition.getLeftOperand(), boolean.class.getCanonicalName(), int.class.getCanonicalName(), long.class.getCanonicalName(),
									short.class.getCanonicalName(), char.class.getCanonicalName(), byte.class.getCanonicalName())
							&& ASTNodes.hasType(secondCondition.getRightOperand(), boolean.class.getCanonicalName(), int.class.getCanonicalName(), long.class.getCanonicalName(),
									short.class.getCanonicalName(), char.class.getCanonicalName(), byte.class.getCanonicalName())
							&& ASTNodes.isPassiveWithoutFallingThrough(firstCondition.getLeftOperand())
							&& ASTNodes.isPassiveWithoutFallingThrough(firstCondition.getRightOperand())
							&& ASTNodes.isPassiveWithoutFallingThrough(secondCondition.getLeftOperand())
							&& ASTNodes.isPassiveWithoutFallingThrough(secondCondition.getRightOperand())) {
						return maybeReplaceDuplicateExpression(visited, firstCondition, firstCondition.getLeftOperand(),
								secondCondition.getLeftOperand(), firstCondition.getRightOperand(), secondCondition.getRightOperand())
								&& maybeReplaceDuplicateExpression(visited, firstCondition, firstCondition.getLeftOperand(),
										secondCondition.getRightOperand(), firstCondition.getRightOperand(), secondCondition.getLeftOperand())
								&& maybeReplaceDuplicateExpression(visited, firstCondition, firstCondition.getRightOperand(),
										secondCondition.getLeftOperand(), firstCondition.getLeftOperand(), secondCondition.getRightOperand())
								&& maybeReplaceDuplicateExpression(visited, firstCondition, firstCondition.getRightOperand(),
										secondCondition.getRightOperand(), firstCondition.getLeftOperand(), secondCondition.getLeftOperand());
					}
				}

				return true;
			}

			private boolean maybeReplaceDuplicateExpression(final InfixExpression visited, final InfixExpression firstCondition,
					final Expression firstExpression, final Expression firstOppositeExpression, final Expression secondExpression, final Expression secondOppositeExpression) {
				if (ASTNodes.match(firstExpression, firstOppositeExpression)
						&& !ASTNodes.match(secondExpression, secondOppositeExpression)
						&& !ASTSemanticMatcher.INSTANCE.matchNegative(secondExpression, secondOppositeExpression)) {
					rewriteOperations.add(new OperandFactorizationOperation(visited, firstCondition, firstExpression, secondExpression, secondOppositeExpression));
					return false;
				}

				return true;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.OperandFactorizationCleanUp_description, unit,
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

	private static class OperandFactorizationOperation extends CompilationUnitRewriteOperation {
		private final InfixExpression visited;
		private final InfixExpression firstCondition;
		private final Expression firstExpression;
		private final Expression secondExpression;
		private final Expression secondOppositeExpression;

		public OperandFactorizationOperation(
				final InfixExpression visited,
				final InfixExpression firstCondition,
				final Expression firstExpression,
				final Expression secondExpression,
				final Expression secondOppositeExpression) {
			this.visited= visited;
			this.firstCondition= firstCondition;
			this.firstExpression= firstExpression;
			this.secondExpression= secondExpression;
			this.secondOppositeExpression= secondOppositeExpression;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.OperandFactorizationCleanUp_description, cuRewrite);

			InfixExpression newInnerInfixExpression= ast.newInfixExpression();
			newInnerInfixExpression.setOperator(visited.getOperator());
			newInnerInfixExpression.setLeftOperand(ASTNodes.createMoveTarget(rewrite, secondExpression));
			newInnerInfixExpression.setRightOperand(ASTNodes.createMoveTarget(rewrite, secondOppositeExpression));

			InfixExpression newMainInfixExpression= ast.newInfixExpression();
			newMainInfixExpression.setOperator(firstCondition.getOperator());
			newMainInfixExpression.setLeftOperand(ASTNodes.createMoveTarget(rewrite, firstExpression));
			newMainInfixExpression.setRightOperand(ASTNodeFactory.parenthesizeIfNeeded(ast, newInnerInfixExpression));

			ASTNodes.replaceButKeepComment(rewrite, visited, ASTNodeFactory.parenthesizeIfNeeded(ast, newMainInfixExpression), group);
		}
	}
}
