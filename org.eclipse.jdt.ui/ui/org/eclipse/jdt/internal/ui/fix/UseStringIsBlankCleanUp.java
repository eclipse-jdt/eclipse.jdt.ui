/*******************************************************************************
 * Copyright (c) 2021 Holger VOORMANN and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.manipulation.CleanUpContextCore;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.OrderedInfixExpression;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.FixMessages;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModelCore;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class UseStringIsBlankCleanUp extends AbstractCleanUp {
	public UseStringIsBlankCleanUp() {
	}

	public UseStringIsBlankCleanUp(final Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(isEnabled(CleanUpConstants.USE_STRING_IS_BLANK), false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.USE_STRING_IS_BLANK)) {
			return new String[] { FixMessages.UseStringIsBlankCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		boolean isEnabled= isEnabled(CleanUpConstants.USE_STRING_IS_BLANK);
		return (isEnabled
				? "if (input.isBlank()) {\n" //$NON-NLS-1$
				: "if (\"\".equals(input.strip())) {\n") //$NON-NLS-1$

				+ "    System.err.println(\"Input must not be blank\");\n" //$NON-NLS-1$
				+ "};\n" //$NON-NLS-1$

				+ "boolean hasComment = " //$NON-NLS-1$
				+ (isEnabled
						? "!comment.isBlank();\n" //$NON-NLS-1$
						: "comment.strip().length() > 0;\n"); //$NON-NLS-1$
	}

	@Override
	public ICleanUpFix createFix(CleanUpContext context) throws CoreException {
		ICleanUpFixCore fixCore= createFixCore(context);
		return fixCore != null ? new CleanUpFixWrapper(fixCore) : null;
	}

	private ICleanUpFixCore createFixCore(final CleanUpContextCore context) {
		CompilationUnit compilationUnit= context.getAST();

		if (compilationUnit == null
				|| !isEnabled(CleanUpConstants.USE_STRING_IS_BLANK)
				|| !JavaModelUtil.is11OrHigher(compilationUnit.getJavaElement().getJavaProject())) {
			return null;
		}

		return createCleanUp(compilationUnit);
	}

	private static ICleanUpFixCore createCleanUp(final CompilationUnit compilationUnit) {
		List<UseStringIsBlankFixOperation> operations= new ArrayList<>();
		compilationUnit.accept(new UseStringIsBlankFinder(operations));

		if (operations.isEmpty()) {
			return null;
		}

		CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperation[0]);
		return new CompilationUnitRewriteOperationsFixCore(FixMessages.UseStringIsBlankCleanUp_description, compilationUnit, ops);
	}


	private static class UseStringIsBlankFinder extends ASTVisitor {
		private List<UseStringIsBlankFixOperation> fResult;

		public UseStringIsBlankFinder(List<UseStringIsBlankFixOperation> ops) {
			fResult= ops;
		}

		@Override
		public boolean visit(MethodInvocation visited) {
			// s.strip().isEmpty()
			if (isStringMethodInvocation(visited, "isEmpty") //$NON-NLS-1$
					&& (isStringMethodInvocation(visited.getExpression(), "strip") //$NON-NLS-1$
							|| isStringMethodInvocation(visited.getExpression(), "stripLeading") //$NON-NLS-1$
							|| isStringMethodInvocation(visited.getExpression(), "stripTrailing"))) { //$NON-NLS-1$
				fResult.add(new UseStringIsBlankFixOperation(visited, visited.getExpression(), true));
				return false;
			}

			if (ASTNodes.usesGivenSignature(visited, Object.class.getCanonicalName(), "equals", Object.class.getCanonicalName())) { //$NON-NLS-1$
				List<Expression> arguments= visited.arguments();

				// s.strip().equals("")
				if ("".equals(arguments.get(0).resolveConstantExpressionValue()) //$NON-NLS-1$
						&& (isStringMethodInvocation(visited.getExpression(), "strip") //$NON-NLS-1$
								|| isStringMethodInvocation(visited.getExpression(), "stripLeading") //$NON-NLS-1$
								|| isStringMethodInvocation(visited.getExpression(), "stripTrailing"))) { //$NON-NLS-1$
					fResult.add(new UseStringIsBlankFixOperation(visited, visited.getExpression(), true));
					return false;
				}

				// "".equals(s.strip())
				Expression expression= visited.getExpression();
				if (expression != null
						&& "".equals(expression.resolveConstantExpressionValue()) //$NON-NLS-1$
						&& (isStringMethodInvocation(arguments.get(0), "strip") //$NON-NLS-1$
								|| isStringMethodInvocation(arguments.get(0), "stripLeading") //$NON-NLS-1$
								|| isStringMethodInvocation(arguments.get(0), "stripTrailing"))) { //$NON-NLS-1$
					fResult.add(new UseStringIsBlankFixOperation(visited, arguments.get(0), true));
					return false;
				}
			}

			return true;
		}

		@Override
		public boolean visit(final InfixExpression visited) {
			OrderedInfixExpression<MethodInvocation, Expression> orderedInfix= ASTNodes.orderedInfix(visited, MethodInvocation.class, Expression.class);

			if (orderedInfix != null
					&& isStringMethodInvocation(orderedInfix.getFirstOperand(), "length") //$NON-NLS-1$
					&& (isStringMethodInvocation(orderedInfix.getFirstOperand().getExpression(), "strip") //$NON-NLS-1$
							|| isStringMethodInvocation(orderedInfix.getFirstOperand().getExpression(), "stripLeading") //$NON-NLS-1$
							|| isStringMethodInvocation(orderedInfix.getFirstOperand().getExpression(), "stripTrailing"))) { //$NON-NLS-1$
				Long number= ASTNodes.getIntegerLiteral(orderedInfix.getSecondOperand());

				if (Long.valueOf(0L).equals(number)) {
					// s.strip().length() == 0
					// s.strip().length() <= 0
					if (Arrays.asList(InfixExpression.Operator.EQUALS, InfixExpression.Operator.LESS_EQUALS).contains(orderedInfix.getOperator())) {
						fResult.add(new UseStringIsBlankFixOperation(visited, orderedInfix.getFirstOperand().getExpression(), true));
						return false;
					}

					// s.strip().length() != 0
					// s.strip().length() > 0
					if (Arrays.asList(InfixExpression.Operator.NOT_EQUALS, InfixExpression.Operator.GREATER).contains(orderedInfix.getOperator())) {
						fResult.add(new UseStringIsBlankFixOperation(visited, orderedInfix.getFirstOperand().getExpression(), false));
						return false;
					}
				}

				if (Long.valueOf(1L).equals(number)) {
					// s.strip().length() < 1
					if (InfixExpression.Operator.LESS.equals(orderedInfix.getOperator())) {
						fResult.add(new UseStringIsBlankFixOperation(visited, orderedInfix.getFirstOperand().getExpression(), true));
						return false;
					}

					// s.strip().length() >= 1
					if (InfixExpression.Operator.GREATER_EQUALS.equals(orderedInfix.getOperator())) {
						fResult.add(new UseStringIsBlankFixOperation(visited, orderedInfix.getFirstOperand().getExpression(), false));
						return false;
					}
				}
			}

			return true;
		}

		private boolean isStringMethodInvocation(final Expression expression, final String methodName) {
			return expression instanceof MethodInvocation && ASTNodes.usesGivenSignature((MethodInvocation) expression, String.class.getCanonicalName(), methodName);
		}
	}

	private static class UseStringIsBlankFixOperation extends CompilationUnitRewriteOperation {
		private final Expression visited;
		private final Expression trim;
		private final boolean isPositive;

		public UseStringIsBlankFixOperation(final Expression visited, final Expression trim, final boolean isPositive) {
			this.visited= visited;
			this.trim= trim;
			this.isPositive= isPositive;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(FixMessages.UseStringIsBlankCleanUp_description, cuRewrite);
			rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
					if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
						return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
					}

					return super.computeSourceRange(nodeWithComment);
				}
			});

			ASTNodes.replaceButKeepComment(rewrite, visited, isPositive ? trim : ASTNodeFactory.negate(ast, rewrite, trim, true), group);
			rewrite.set(trim, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("isBlank"), group); //$NON-NLS-1$
		}
	}

}
