/*******************************************************************************
 *  Copyright (c) 2020 Julian Honnen
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Julian Honnen <julian.honnen@vector.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
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
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposal;

class ConvertStringConcatenationProposals {

	private final IInvocationContext fContext;
	private final AST fAst;
	private final InfixExpression fOldInfixExpression;

	private ConvertStringConcatenationProposals(IInvocationContext context, AST ast, InfixExpression oldInfixExpression) {
		fContext= context;
		fAst= ast;
		fOldInfixExpression= oldInfixExpression;
	}

	public static boolean getProposals(IInvocationContext context, Collection<ICommandAccess> resultingCollections) {
		ASTNode node= context.getCoveringNode();
		BodyDeclaration parentDecl= ASTResolving.findParentBodyDeclaration(node);
		if (!(parentDecl instanceof MethodDeclaration) && !(parentDecl instanceof Initializer))
			return false;

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
		if (oldInfixExpression == null)
			return false;

		if (resultingCollections == null) {
			return true;
		}

		ConvertStringConcatenationProposals convertStringConcatenation= new ConvertStringConcatenationProposals(context, ast, oldInfixExpression);
		convertStringConcatenation.createProposals(resultingCollections);

		return true;
	}

	private void createProposals(Collection<ICommandAccess> resultingCollections) {
		ASTRewriteCorrectionProposal stringBufferProposal= getConvertToStringBufferProposal();
		resultingCollections.add(stringBufferProposal);

		ASTRewriteCorrectionProposal messageFormatProposal= getConvertToMessageFormatProposal();
		if (messageFormatProposal != null)
			resultingCollections.add(messageFormatProposal);

		ASTRewriteCorrectionProposal stringFormatProposal= getConvertToStringFormatProposal();
		if (stringFormatProposal != null)
			resultingCollections.add(stringFormatProposal);
	}

	private ASTRewriteCorrectionProposal getConvertToStringBufferProposal() {
		String bufferOrBuilderName;
		ICompilationUnit cu= fContext.getCompilationUnit();
		if (JavaModelUtil.is50OrHigher(cu.getJavaProject())) {
			bufferOrBuilderName= "StringBuilder"; //$NON-NLS-1$
		} else {
			bufferOrBuilderName= "StringBuffer"; //$NON-NLS-1$
		}

		ASTRewrite rewrite= ASTRewrite.create(fAst);

		SimpleName existingBuffer= getEnclosingAppendBuffer(fOldInfixExpression);

		String mechanismName= BasicElementLabels.getJavaElementName(existingBuffer == null ? bufferOrBuilderName : existingBuffer.getIdentifier());
		String label= Messages.format(CorrectionMessages.QuickAssistProcessor_convert_to_string_buffer_description, mechanismName);
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, IProposalRelevance.CONVERT_TO_STRING_BUFFER, image);
		proposal.setCommandId(QuickAssistProcessor.CONVERT_TO_STRING_BUFFER_ID);

		Statement insertAfter;
		String bufferName;

		String groupID= "nameId"; //$NON-NLS-1$
		ListRewrite listRewrite;

		Statement enclosingStatement= ASTResolving.findParentStatement(fOldInfixExpression);

		if (existingBuffer != null) {
			if (ASTNodes.isControlStatementBody(enclosingStatement.getLocationInParent())) {
				Block newBlock= fAst.newBlock();
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
			VariableDeclarationFragment frag= fAst.newVariableDeclarationFragment();
			// check if name is already in use and provide alternative
			List<String> fExcludedVariableNames= Arrays.asList(ASTResolving.getUsedVariableNames(fOldInfixExpression));

			SimpleType bufferType= fAst.newSimpleType(fAst.newName(bufferOrBuilderName));
			ClassInstanceCreation newBufferExpression= fAst.newClassInstanceCreation();

			String[] newBufferNames= StubUtility.getVariableNameSuggestions(NamingConventions.VK_LOCAL, cu.getJavaProject(), bufferOrBuilderName, 0, fExcludedVariableNames, true);
			bufferName= newBufferNames[0];

			SimpleName bufferNameDeclaration= fAst.newSimpleName(bufferName);
			frag.setName(bufferNameDeclaration);

			proposal.addLinkedPosition(rewrite.track(bufferNameDeclaration), true, groupID);
			for (String newBufferName : newBufferNames) {
				proposal.addLinkedPositionProposal(groupID, newBufferName, null);
			}


			newBufferExpression.setType(bufferType);
			frag.setInitializer(newBufferExpression);


			VariableDeclarationStatement bufferDeclaration= fAst.newVariableDeclarationStatement(frag);
			bufferDeclaration.setType(fAst.newSimpleType(fAst.newName(bufferOrBuilderName)));
			insertAfter= bufferDeclaration;

			Statement statement= ASTResolving.findParentStatement(fOldInfixExpression);
			if (ASTNodes.isControlStatementBody(statement.getLocationInParent())) {
				Block newBlock= fAst.newBlock();
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
		collectInfixPlusOperands(fOldInfixExpression, operands);

		Statement lastAppend= insertAfter;
		for (Expression operand : operands) {
			MethodInvocation appendIncovationExpression= fAst.newMethodInvocation();
			appendIncovationExpression.setName(fAst.newSimpleName("append")); //$NON-NLS-1$
			SimpleName bufferNameReference= fAst.newSimpleName(bufferName);

			// If there was an existing name, don't offer to rename it
			if (existingBuffer == null) {
				proposal.addLinkedPosition(rewrite.track(bufferNameReference), true, groupID);
			}

			appendIncovationExpression.setExpression(bufferNameReference);
			appendIncovationExpression.arguments().add(rewrite.createCopyTarget(operand));

			ExpressionStatement appendExpressionStatement= fAst.newExpressionStatement(appendIncovationExpression);
			if (lastAppend == null) {
				listRewrite.insertFirst(appendExpressionStatement, null);
			} else {
				listRewrite.insertAfter(appendExpressionStatement, lastAppend, null);
			}
			lastAppend= appendExpressionStatement;
		}

		if (existingBuffer != null) {
			proposal.setEndPosition(rewrite.track(lastAppend));
			if (insertAfter != null) {
				rewrite.remove(enclosingStatement, null);
			}
		} else {
			// replace old expression with toString
			MethodInvocation bufferToString= fAst.newMethodInvocation();
			bufferToString.setName(fAst.newSimpleName("toString")); //$NON-NLS-1$
			SimpleName bufferNameReference= fAst.newSimpleName(bufferName);
			bufferToString.setExpression(bufferNameReference);
			proposal.addLinkedPosition(rewrite.track(bufferNameReference), true, groupID);

			rewrite.replace(fOldInfixExpression, bufferToString, null);
			proposal.setEndPosition(rewrite.track(bufferToString));
		}

		return proposal;
	}

	private ASTRewriteCorrectionProposal getConvertToMessageFormatProposal() {

		ICompilationUnit cu= fContext.getCompilationUnit();
		boolean is50OrHigher= JavaModelUtil.is50OrHigher(cu.getJavaProject());

		ASTRewrite rewrite= ASTRewrite.create(fAst);
		CompilationUnit root= fContext.getASTRoot();
		ImportRewrite importRewrite= StubUtility.createImportRewrite(root, true);
		ContextSensitiveImportRewriteContext importContext= new ContextSensitiveImportRewriteContext(root, fOldInfixExpression.getStartPosition(), importRewrite);

		// collect operands
		List<Expression> operands= new ArrayList<>();
		collectInfixPlusOperands(fOldInfixExpression, operands);

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
					if (binding == null)
						return null;

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

		if (formatArguments.isEmpty())
			return null;

		String label= CorrectionMessages.QuickAssistProcessor_convert_to_message_format;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, IProposalRelevance.CONVERT_TO_MESSAGE_FORMAT, image);
		proposal.setCommandId(QuickAssistProcessor.CONVERT_TO_MESSAGE_FORMAT_ID);

		proposal.setImportRewrite(importRewrite);

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

		rewrite.replace(fOldInfixExpression, formatInvocation, null);

		return proposal;
	}

	private ASTRewriteCorrectionProposal getConvertToStringFormatProposal() {

		ICompilationUnit cu= fContext.getCompilationUnit();
		if (!JavaModelUtil.is50OrHigher(cu.getJavaProject()))
			return null;

		ASTRewrite rewrite= ASTRewrite.create(fAst);

		// collect operands
		List<Expression> operands= new ArrayList<>();
		collectInfixPlusOperands(fOldInfixExpression, operands);

		List<Expression> formatArguments= new ArrayList<>();
		StringBuilder formatString= new StringBuilder();
		for (Expression operand : operands) {
			if (operand instanceof StringLiteral) {
				String value= ((StringLiteral) operand).getEscapedValue();
				value= value.substring(1, value.length() - 1);
				formatString.append(value);
			} else {
				ITypeBinding binding= operand.resolveTypeBinding();
				if (binding == null)
					return null;

				formatString.append("%").append(stringFormatConversion(binding)); //$NON-NLS-1$
				formatArguments.add((Expression) rewrite.createCopyTarget(operand));
			}
		}

		if (formatArguments.isEmpty())
			return null;

		String label= CorrectionMessages.QuickAssistProcessor_convert_to_string_format;
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);

		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, cu, rewrite, IProposalRelevance.CONVERT_TO_MESSAGE_FORMAT, image);
		proposal.setCommandId(QuickAssistProcessor.CONVERT_TO_STRING_FORMAT_ID);

		MethodInvocation formatInvocation= fAst.newMethodInvocation();
		formatInvocation.setExpression(fAst.newName("String")); //$NON-NLS-1$
		formatInvocation.setName(fAst.newSimpleName("format")); //$NON-NLS-1$

		List<Expression> arguments= formatInvocation.arguments();

		StringLiteral formatStringArgument= fAst.newStringLiteral();
		formatStringArgument.setEscapedValue("\"" + formatString.append("\"").toString()); //$NON-NLS-1$ //$NON-NLS-2$
		arguments.add(formatStringArgument);

		arguments.addAll(formatArguments);

		rewrite.replace(fOldInfixExpression, formatInvocation, null);

		return proposal;
	}

	private static char stringFormatConversion(ITypeBinding type) {
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

}
