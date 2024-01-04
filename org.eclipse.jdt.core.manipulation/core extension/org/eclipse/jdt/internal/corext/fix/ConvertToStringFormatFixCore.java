/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Gayan Perera - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public class ConvertToStringFormatFixCore extends CompilationUnitRewriteOperationsFixCore {

	public ConvertToStringFormatFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation operation) {
		super(name, compilationUnit, operation);
	}

	public static ConvertToStringFormatFixCore createConvertToStringFormatFix(CompilationUnit compilationUnit, ASTNode node) {
		BodyDeclaration parentDecl= ASTResolving.findParentBodyDeclaration(node);
		if (!(parentDecl instanceof MethodDeclaration) && !(parentDecl instanceof Initializer))
			return null;

		AST ast= node.getAST();
		ITypeBinding stringBinding= ast.resolveWellKnownType("java.lang.String"); //$NON-NLS-1$

		if (node instanceof Expression && !(node instanceof InfixExpression)) {
			node= node.getParent();
		}
		if (node instanceof VariableDeclarationFragment) {
			node= ((VariableDeclarationFragment) node).getInitializer();
		} else if (node instanceof Assignment) {
			node= ((Assignment) node).getRightHandSide();
		}

		InfixExpression oldInfixExpression= null;
		while (node instanceof InfixExpression) {
			InfixExpression curr= (InfixExpression) node;
			if (curr.resolveTypeBinding() == stringBinding && curr.getOperator() == InfixExpression.Operator.PLUS) {
				oldInfixExpression= curr; // is a infix expression we can use
			} else {
				break;
			}
			node= node.getParent();
		}
		if (oldInfixExpression == null) {
			return null;
		}

		boolean is50OrHigher= JavaModelUtil.is50OrHigher(compilationUnit.getTypeRoot().getJavaProject());
		if (!is50OrHigher) {
			return null;
		}

		// collect operands
		List<Expression> operands= new ArrayList<>();
		collectInfixPlusOperands(oldInfixExpression, operands);

		boolean foundNoneLiteralOperand= false;
		// we need to loop through all to exclude any null binding scenarios.
		for (Expression operand : operands) {
			if (!(operand instanceof StringLiteral)) {
				if (!is50OrHigher) {
					ITypeBinding binding= operand.resolveTypeBinding();
					if (binding == null) {
						return null;
					}
				}
				foundNoneLiteralOperand= true;
			}
		}
		if (!foundNoneLiteralOperand) {
			return null;
		}

		return new ConvertToStringFormatFixCore(CorrectionMessages.QuickAssistProcessor_convert_to_string_format, compilationUnit,
				new ConvertToStringFormatProposalOperation(oldInfixExpression));
	}

	private static void collectInfixPlusOperands(Expression expression, List<Expression> collector) {
		if (expression instanceof InfixExpression && ((InfixExpression) expression).getOperator() == InfixExpression.Operator.PLUS) {
			InfixExpression infixExpression= (InfixExpression) expression;

			collectInfixPlusOperands(infixExpression.getLeftOperand(), collector);
			collectInfixPlusOperands(infixExpression.getRightOperand(), collector);
			List<Expression> extendedOperands= infixExpression.extendedOperands();
			for (Expression expression2 : extendedOperands) {
				collectInfixPlusOperands(expression2, collector);
			}

		} else {
			collector.add(expression);
		}
	}

	private static class ConvertToStringFormatProposalOperation extends CompilationUnitRewriteOperation {
		private InfixExpression infixExpression;

		public ConvertToStringFormatProposalOperation(InfixExpression infixExpression) {
			this.infixExpression= infixExpression;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getAST();

			// collect operands
			List<Expression> operands= new ArrayList<>();
			collectInfixPlusOperands(infixExpression, operands);

			List<Expression> formatArguments= new ArrayList<>();
			StringBuilder formatString= new StringBuilder();
			for (Expression operand : operands) {
				if (operand instanceof StringLiteral) {
					String value= ((StringLiteral) operand).getEscapedValue();
					value= value.substring(1, value.length() - 1);
					formatString.append(value);
				} else {
					ITypeBinding binding= operand.resolveTypeBinding();
					formatString.append("%").append(stringFormatConversion(binding)); //$NON-NLS-1$
					formatArguments.add((Expression) rewrite.createCopyTarget(operand));
				}
			}


			MethodInvocation formatInvocation= ast.newMethodInvocation();
			formatInvocation.setExpression(ast.newName("String")); //$NON-NLS-1$
			formatInvocation.setName(ast.newSimpleName("format")); //$NON-NLS-1$

			List<Expression> arguments= formatInvocation.arguments();

			StringLiteral formatStringArgument= ast.newStringLiteral();
			formatStringArgument.setEscapedValue("\"" + formatString.append("\"").toString()); //$NON-NLS-1$ //$NON-NLS-2$
			arguments.add(formatStringArgument);

			arguments.addAll(formatArguments);

			rewrite.replace(infixExpression, formatInvocation, null);
		}

		private char stringFormatConversion(ITypeBinding type) {
			switch (type.getName()) {
				case "byte": //$NON-NLS-1$
				case "short": //$NON-NLS-1$
				case "int": //$NON-NLS-1$
				case "long": //$NON-NLS-1$
					return 'd';
				case "float": //$NON-NLS-1$
				case "double": //$NON-NLS-1$
					return 'f';
				case "char": //$NON-NLS-1$
					return 'c';
				default:
					return 's';
			}
		}

	}
}
