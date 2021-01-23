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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.OrderedInfixExpression;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class StandardComparisonFixCore extends CompilationUnitRewriteOperationsFixCore {
	public static final class StandardComparisonFinder extends ASTVisitor {
		private List<StandardComparisonFixOperation> fResult;

		public StandardComparisonFinder(List<StandardComparisonFixOperation> ops) {
			fResult= ops;
		}

		@Override
		public boolean visit(final InfixExpression visited) {
			OrderedInfixExpression<MethodInvocation, Expression> orderedCondition= ASTNodes.orderedInfix(visited, MethodInvocation.class, Expression.class);

			if (orderedCondition != null
					&& Arrays.asList(InfixExpression.Operator.EQUALS, InfixExpression.Operator.NOT_EQUALS).contains(orderedCondition.getOperator())) {
				MethodInvocation comparisonMI= orderedCondition.getFirstOperand();
				Long literalValue= ASTNodes.getIntegerLiteral(orderedCondition.getSecondOperand());

				if (literalValue != null
						&& literalValue.compareTo(0L) != 0
						&& comparisonMI.getExpression() != null
						&& !ASTNodes.is(comparisonMI.getExpression(), ThisExpression.class)
						&& (ASTNodes.usesGivenSignature(comparisonMI, Comparable.class.getCanonicalName(), "compareTo", Object.class.getCanonicalName()) //$NON-NLS-1$
						|| ASTNodes.usesGivenSignature(comparisonMI, Comparator.class.getCanonicalName(), "compare", Object.class.getCanonicalName(), Object.class.getCanonicalName()) //$NON-NLS-1$
						|| JavaModelUtil.is1d2OrHigher(((CompilationUnit) visited.getRoot()).getJavaElement().getJavaProject())
						&& ASTNodes.usesGivenSignature(comparisonMI, String.class.getCanonicalName(), "compareToIgnoreCase", String.class.getCanonicalName()))) { //$NON-NLS-1$
					if (literalValue.compareTo(0L) < 0) {
						if (InfixExpression.Operator.EQUALS.equals(orderedCondition.getOperator())) {
							fResult.add(new StandardComparisonFixOperation(visited, comparisonMI, InfixExpression.Operator.LESS));
						} else {
							fResult.add(new StandardComparisonFixOperation(visited, comparisonMI, InfixExpression.Operator.GREATER_EQUALS));
						}
					} else if (InfixExpression.Operator.EQUALS.equals(orderedCondition.getOperator())) {
						fResult.add(new StandardComparisonFixOperation(visited, comparisonMI, InfixExpression.Operator.GREATER));
					} else {
						fResult.add(new StandardComparisonFixOperation(visited, comparisonMI, InfixExpression.Operator.LESS_EQUALS));
					}

					return false;
				}
			}

			return true;
		}
	}

	public static class StandardComparisonFixOperation extends CompilationUnitRewriteOperation {
		private final InfixExpression visited;
		private final MethodInvocation comparisonMethod;
		private final InfixExpression.Operator operator;

		public StandardComparisonFixOperation(final InfixExpression visited, final MethodInvocation comparisonMethod, final InfixExpression.Operator operator) {
			this.visited= visited;
			this.comparisonMethod= comparisonMethod;
			this.operator= operator;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.StandardComparisonCleanUp_description, cuRewrite);
			rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
					if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
						return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
					}

					return super.computeSourceRange(nodeWithComment);
				}
			});

			InfixExpression newInfixExpression= ast.newInfixExpression();
			newInfixExpression.setLeftOperand(ASTNodes.createMoveTarget(rewrite, comparisonMethod));
			newInfixExpression.setOperator(operator);
			newInfixExpression.setRightOperand(ast.newNumberLiteral("0")); //$NON-NLS-1$

			ASTNodes.replaceButKeepComment(rewrite, visited, newInfixExpression, group);
		}
	}


	public static ICleanUpFixCore createCleanUp(final CompilationUnit compilationUnit) {
		List<StandardComparisonFixOperation> operations= new ArrayList<>();
		StandardComparisonFinder finder= new StandardComparisonFinder(operations);
		compilationUnit.accept(finder);

		if (operations.isEmpty()) {
			return null;
		}

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[0]);
		return new StandardComparisonFixCore(FixMessages.StandardComparisonFix_compare_to_zero, compilationUnit, ops);
	}

	protected StandardComparisonFixCore(final String name, final CompilationUnit compilationUnit, final CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
}
