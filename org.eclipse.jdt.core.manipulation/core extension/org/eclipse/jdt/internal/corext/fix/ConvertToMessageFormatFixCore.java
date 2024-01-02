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
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public class ConvertToMessageFormatFixCore extends CompilationUnitRewriteOperationsFixCore {

	public ConvertToMessageFormatFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation operation) {
		super(name, compilationUnit, operation);
	}

	public static ConvertToMessageFormatFixCore createConvertToMessageFormatFix(CompilationUnit compilationUnit, ASTNode node) {
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

		return new ConvertToMessageFormatFixCore(CorrectionMessages.QuickAssistProcessor_convert_to_message_format, compilationUnit,
				new ConvertToMessageFormatProposalOperation(oldInfixExpression));
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

	private static class ConvertToMessageFormatProposalOperation extends CompilationUnitRewriteOperation {
		private InfixExpression infixExpression;


		public ConvertToMessageFormatProposalOperation(InfixExpression infixExpression) {
			this.infixExpression= infixExpression;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
			ICompilationUnit cu= cuRewrite.getCu();
			boolean is50OrHigher= JavaModelUtil.is50OrHigher(cu.getJavaProject());
			AST fAst= cuRewrite.getAST();

			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			CompilationUnit root= cuRewrite.getRoot();
			ImportRewrite importRewrite= cuRewrite.getImportRewrite();
			ContextSensitiveImportRewriteContext importContext= new ContextSensitiveImportRewriteContext(root, infixExpression.getStartPosition(), importRewrite);

			// collect operands
			List<Expression> operands= new ArrayList<>();
			collectInfixPlusOperands(infixExpression, operands);

			List<Expression> formatArguments= new ArrayList<>();
			StringBuilder formatString= new StringBuilder();
			int i= 0;
			for (Expression operand : operands) {
				if (operand instanceof StringLiteral) {
					String value= ((StringLiteral) operand).getEscapedValue();
					value= value.substring(1, value.length() - 1);
					value= value.replace("'", "''"); //$NON-NLS-1$ //$NON-NLS-2$
					formatString.append(value);
				} else {
					formatString.append("{").append(i).append("}"); //$NON-NLS-1$ //$NON-NLS-2$

					Expression argument;
					if (is50OrHigher) {
						argument= (Expression) rewrite.createCopyTarget(operand);
					} else {
						ITypeBinding binding= operand.resolveTypeBinding();
						argument= (Expression) rewrite.createCopyTarget(operand);

						if (binding.isPrimitive()) {
							ITypeBinding boxedBinding= Bindings.getBoxedTypeBinding(binding, fAst);
							if (boxedBinding != binding) {
								Type boxedType= importRewrite.addImport(boxedBinding, fAst, importContext);
								ClassInstanceCreation cic= fAst.newClassInstanceCreation();
								cic.setType(boxedType);
								cic.arguments().add(argument);
								argument= cic;
							}
						}
					}

					formatArguments.add(argument);
					i++;
				}
			}


			String messageType= importRewrite.addImport("java.text.MessageFormat", importContext); //$NON-NLS-1$

			MethodInvocation formatInvocation= fAst.newMethodInvocation();
			formatInvocation.setExpression(fAst.newName(messageType));
			formatInvocation.setName(fAst.newSimpleName("format")); //$NON-NLS-1$

			List<Expression> arguments= formatInvocation.arguments();

			StringLiteral formatStringArgument= fAst.newStringLiteral();
			formatStringArgument.setEscapedValue("\"" + formatString.append("\"").toString()); //$NON-NLS-1$ //$NON-NLS-2$
			arguments.add(formatStringArgument);

			if (is50OrHigher) {
				arguments.addAll(formatArguments);
			} else {
				ArrayCreation objectArrayCreation= fAst.newArrayCreation();

				Type objectType= fAst.newSimpleType(fAst.newSimpleName("Object")); //$NON-NLS-1$
				ArrayType arrayType= fAst.newArrayType(objectType);
				objectArrayCreation.setType(arrayType);

				ArrayInitializer arrayInitializer= fAst.newArrayInitializer();

				List<Expression> initializerExpressions= arrayInitializer.expressions();
				initializerExpressions.addAll(formatArguments);
				objectArrayCreation.setInitializer(arrayInitializer);

				arguments.add(objectArrayCreation);
			}

			rewrite.replace(infixExpression, formatInvocation, null);
		}
	}
}
