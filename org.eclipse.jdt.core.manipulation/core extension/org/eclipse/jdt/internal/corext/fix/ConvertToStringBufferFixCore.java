/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.BadLocationException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSElement;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSLine;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSScanner;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public class ConvertToStringBufferFixCore extends CompilationUnitRewriteOperationsFixCore {

	public ConvertToStringBufferFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation operation) {
		super(name, compilationUnit, operation);
	}

	public static ConvertToStringBufferFixCore createConvertToStringBufferFix(CompilationUnit compilationUnit, ASTNode node) {
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

		String bufferOrBuilderName;
		SimpleName existingBuffer= getEnclosingAppendBuffer(oldInfixExpression);
		if (JavaModelUtil.is50OrHigher(compilationUnit.getTypeRoot().getJavaProject())) {
			bufferOrBuilderName= "StringBuilder"; //$NON-NLS-1$
		} else {
			bufferOrBuilderName= "StringBuffer"; //$NON-NLS-1$
		}
		var mechanismName= BasicElementLabels.getJavaElementName(existingBuffer == null ? bufferOrBuilderName : existingBuffer.getIdentifier());
		var label= Messages.format(CorrectionMessages.QuickAssistProcessor_convert_to_string_buffer_description, mechanismName);
		return new ConvertToStringBufferFixCore(label, compilationUnit, new ConvertToStringBufferProposalOperation(oldInfixExpression));

	}

	/**
	 * Checks
	 * <ul>
	 * <li>whether the given infix expression is the argument of a StringBuilder#append() or
	 * StringBuffer#append() invocation, and</li>
	 * <li>the append method is called on a simple variable, and</li>
	 * <li>the invocation occurs in a statement (not as nested expression)</li>
	 * </ul>
	 *
	 * @param infixExpression the infix expression
	 * @return the name of the variable we were appending to, or <code>null</code> if not matching
	 */
	private static SimpleName getEnclosingAppendBuffer(InfixExpression infixExpression) {
		if (infixExpression.getLocationInParent() == MethodInvocation.ARGUMENTS_PROPERTY) {
			MethodInvocation methodInvocation= (MethodInvocation) infixExpression.getParent();

			// ..not in an expression.. (e.g. not sb.append("high" + 5).append(6);)
			if (methodInvocation.getParent() instanceof Statement) {

				// ..of a function called append:
				if ("append".equals(methodInvocation.getName().getIdentifier())) { //$NON-NLS-1$
					Expression expression= methodInvocation.getExpression();

					// ..and the append is being called on a Simple object:
					if (expression instanceof SimpleName) {
						IBinding binding= ((SimpleName) expression).resolveBinding();
						if (binding instanceof IVariableBinding) {
							String typeName= ((IVariableBinding) binding).getType().getQualifiedName();

							// And the object's type is a StringBuilder or StringBuffer:
							if ("java.lang.StringBuilder".equals(typeName) || "java.lang.StringBuffer".equals(typeName)) { //$NON-NLS-1$ //$NON-NLS-2$
								return (SimpleName) expression;
							}
						}
					}
				}
			}
		}
		return null;
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

	private static class ConvertToStringBufferProposalOperation extends CompilationUnitRewriteOperation {
		private InfixExpression oldInfixExpression;

		public ConvertToStringBufferProposalOperation(InfixExpression oldInfixExpression) {
			this.oldInfixExpression= oldInfixExpression;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
			String bufferOrBuilderName;
			ICompilationUnit cu= cuRewrite.getCu();
			AST ast= cuRewrite.getAST();
			if (JavaModelUtil.is50OrHigher(cu.getJavaProject())) {
				bufferOrBuilderName= "StringBuilder"; //$NON-NLS-1$
			} else {
				bufferOrBuilderName= "StringBuffer"; //$NON-NLS-1$
			}

			ASTRewrite rewrite= cuRewrite.getASTRewrite();

			SimpleName existingBuffer= getEnclosingAppendBuffer(oldInfixExpression);

			Statement insertAfter;
			String bufferName;

			String groupID= "nameId"; //$NON-NLS-1$
			ListRewrite listRewrite;

			Statement enclosingStatement= ASTResolving.findParentStatement(oldInfixExpression);

			if (existingBuffer != null) {
				if (ASTNodes.isControlStatementBody(enclosingStatement.getLocationInParent())) {
					Block newBlock= ast.newBlock();
					listRewrite= rewrite.getListRewrite(newBlock, Block.STATEMENTS_PROPERTY);
					insertAfter= null;
					rewrite.replace(enclosingStatement, newBlock, null);
				} else {
					listRewrite= rewrite.getListRewrite(enclosingStatement.getParent(), (ChildListPropertyDescriptor) enclosingStatement.getLocationInParent());
					insertAfter= enclosingStatement;
				}

				bufferName= existingBuffer.getIdentifier();

			} else {
				// create buffer
				VariableDeclarationFragment frag= ast.newVariableDeclarationFragment();
				// check if name is already in use and provide alternative
				List<String> fExcludedVariableNames= Arrays.asList(ASTResolving.getUsedVariableNames(oldInfixExpression));

				SimpleType bufferType= ast.newSimpleType(ast.newName(bufferOrBuilderName));
				ClassInstanceCreation newBufferExpression= ast.newClassInstanceCreation();

				String[] newBufferNames= StubUtility.getVariableNameSuggestions(NamingConventions.VK_LOCAL, cu.getJavaProject(), bufferOrBuilderName, 0, fExcludedVariableNames, true);
				bufferName= newBufferNames[0];

				SimpleName bufferNameDeclaration= ast.newSimpleName(bufferName);
				frag.setName(bufferNameDeclaration);

				LinkedProposalPositionGroupCore pg= linkedModel.getPositionGroup(groupID, true);
				pg.addPosition(rewrite.track(bufferNameDeclaration), true);
				for (String newBufferName : newBufferNames) {
					pg.addProposal(newBufferName, 10);
				}


				newBufferExpression.setType(bufferType);
				frag.setInitializer(newBufferExpression);


				VariableDeclarationStatement bufferDeclaration= ast.newVariableDeclarationStatement(frag);
				bufferDeclaration.setType(ast.newSimpleType(ast.newName(bufferOrBuilderName)));
				insertAfter= bufferDeclaration;

				Statement statement= ASTResolving.findParentStatement(oldInfixExpression);
				if (ASTNodes.isControlStatementBody(statement.getLocationInParent())) {
					Block newBlock= ast.newBlock();
					listRewrite= rewrite.getListRewrite(newBlock, Block.STATEMENTS_PROPERTY);
					listRewrite.insertFirst(bufferDeclaration, null);
					listRewrite.insertLast(rewrite.createMoveTarget(statement), null);
					rewrite.replace(statement, newBlock, null);
				} else {
					listRewrite= rewrite.getListRewrite(statement.getParent(), (ChildListPropertyDescriptor) statement.getLocationInParent());
					listRewrite.insertBefore(bufferDeclaration, statement, null);
				}
			}

			List<Expression> operands= new ArrayList<>();
			collectInfixPlusOperands(oldInfixExpression, operands);

			Statement lastAppend= insertAfter;
			int tagsCount= 0;
			CompilationUnit compilationUnit= (CompilationUnit)oldInfixExpression.getRoot();
			for (Expression operand : operands) {
				boolean tagged= false;
				NLSLine nlsLine= scanCurrentLine(cu, operand);
				if (nlsLine != null) {
					for (NLSElement element : nlsLine.getElements()) {
						if (element.getPosition().getOffset() == compilationUnit.getColumnNumber(operand.getStartPosition())) {
							if (element.hasTag()) {
								tagged= true;
								++tagsCount;
							}
						}
					}
				}
				ExpressionStatement appendExpressionStatement= null;
				if (tagged) {
					String appendCall= bufferName + ".append(" + operand + "); //$NON-NLS-1$"; //$NON-NLS-1$ //$NON-NLS-2$
					appendExpressionStatement= (ExpressionStatement)rewrite.createStringPlaceholder(appendCall, ASTNode.EXPRESSION_STATEMENT);
				} else {
					MethodInvocation appendIncovationExpression= ast.newMethodInvocation();
					appendIncovationExpression.setName(ast.newSimpleName("append")); //$NON-NLS-1$
					SimpleName bufferNameReference= ast.newSimpleName(bufferName);

					// If there was an existing name, don't offer to rename it
					if (existingBuffer == null) {
						linkedModel.getPositionGroup(groupID, true).addPosition(rewrite.track(bufferNameReference), true);
					}

					appendIncovationExpression.setExpression(bufferNameReference);
					appendIncovationExpression.arguments().add(rewrite.createCopyTarget(operand));

					appendExpressionStatement= ast.newExpressionStatement(appendIncovationExpression);
				}
				if (lastAppend == null) {
					listRewrite.insertFirst(appendExpressionStatement, null);
				} else {
					listRewrite.insertAfter(appendExpressionStatement, lastAppend, null);
				}
				lastAppend= appendExpressionStatement;
			}

			if (existingBuffer != null) {
				linkedModel.setEndPosition(rewrite.track(lastAppend));
				if (insertAfter != null) {
					rewrite.remove(enclosingStatement, null);
				}
			} else {
				// replace old expression with toString

				MethodInvocation bufferToString= ast.newMethodInvocation();
				bufferToString.setName(ast.newSimpleName("toString")); //$NON-NLS-1$
				SimpleName bufferNameReference= ast.newSimpleName(bufferName);
				bufferToString.setExpression(bufferNameReference);
				linkedModel.getPositionGroup(groupID, true).addPosition(rewrite.track(bufferNameReference), true);

				if (tagsCount > 0) {
					ASTNodes.replaceAndRemoveNLSByCount(rewrite, oldInfixExpression, bufferToString.toString().replaceAll(",", ", "), tagsCount, null, cuRewrite); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					rewrite.replace(oldInfixExpression, bufferToString, null);
				}
				linkedModel.setEndPosition(rewrite.track(bufferToString));
			}
		}

		private static NLSLine scanCurrentLine(ICompilationUnit cu, Expression exp) {
			CompilationUnit cUnit= (CompilationUnit)exp.getRoot();
			int startLine= cUnit.getLineNumber(exp.getStartPosition());
			int startLinePos= cUnit.getPosition(startLine, 0);
			int endOfLine= cUnit.getPosition(startLine + 1, 0);
			NLSLine[] lines;
			try {
				lines= NLSScanner.scan(cu.getBuffer().getText(startLinePos, endOfLine - startLinePos));
				if (lines.length > 0) {
					return lines[0];
				}
			} catch (IndexOutOfBoundsException | JavaModelException | InvalidInputException | BadLocationException e) {
				// fall-through
			}
			return null;
		}

	}
}
