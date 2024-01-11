/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.text.correction.QuickAssistProcessorUtil;

public class AddMissingMethodDeclarationFixCore extends CompilationUnitRewriteOperationsFixCore {

	public AddMissingMethodDeclarationFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation operation) {
		super(name, compilationUnit, operation);
	}

	public static AddMissingMethodDeclarationFixCore createAddMissingMethodDeclaration(CompilationUnit compilationUnit, ASTNode node) {
		ExpressionMethodReference methodReferenceNode= node instanceof ExpressionMethodReference
				? (ExpressionMethodReference) node
				: ASTNodes.getParent(node, ExpressionMethodReference.class);
		if (methodReferenceNode == null) {
			return null;
		}

		TypeDeclaration typeDeclaration= ASTNodes.getParent(methodReferenceNode, TypeDeclaration.class);

		if (QuickAssistProcessorUtil.isTypeReferenceToInstanceMethod(methodReferenceNode)) {
			String methodReferenceQualifiedName= ((Name) methodReferenceNode.getExpression()).getFullyQualifiedName();
			String packageName= (compilationUnit.getPackage() != null) ? compilationUnit.getPackage().getName().getFullyQualifiedName() + '.' : ""; //$NON-NLS-1$
			String typeDeclarationName= packageName + typeDeclaration.getName().getFullyQualifiedName();
			if (!methodReferenceQualifiedName.equals(typeDeclarationName)
					&& !methodReferenceQualifiedName.equals(typeDeclaration.getName().getFullyQualifiedName())) {
				// only propose for references in same class
				return null;
			}
		}

		VariableDeclarationStatement variableDeclarationStatement= ASTNodes.getParent(methodReferenceNode, VariableDeclarationStatement.class);
		MethodInvocation methodInvocationNode= ASTNodes.getParent(methodReferenceNode, MethodInvocation.class);
		Assignment variableAssignment= ASTNodes.getParent(methodReferenceNode, Assignment.class);

		String label= Messages.format(CorrectionMessages.AddUnimplementedMethodReferenceOperation_AddMissingMethod_group,
				new String[] { methodReferenceNode.getName().getIdentifier(), typeDeclaration.getName().getIdentifier() });

		if ((variableAssignment != null || variableDeclarationStatement != null) && methodInvocationNode == null) {
			/*
			 * variable declaration
			 */
			Type type= null;
			ReturnType returnType= null;
			if (variableDeclarationStatement != null) {
				type= variableDeclarationStatement.getType();
				returnType= getReturnType(type);
			} else {
				Expression leftHandSide= variableAssignment.getLeftHandSide();
				ITypeBinding assignmentTypeBinding= leftHandSide.resolveTypeBinding();
				if (assignmentTypeBinding == null) {
					return null;
				}
				returnType= new ReturnType();
				returnType.binding= assignmentTypeBinding;
			}
			if (returnType.binding == null) {
				return null;
			}
			return new AddMissingMethodDeclarationFixCore(label, compilationUnit, new AddMissingMethodDeclarationProposalOperation(methodReferenceNode, returnType, null));
		} else {
			IMethodBinding methodBinding= methodInvocationNode == null ? null : methodInvocationNode.resolveMethodBinding();
			if (methodBinding == null) {
				return null;
			}
			List<ASTNode> arguments= methodInvocationNode.arguments();
			int index= -1;
			for (int i= 0; i < arguments.size(); i++) {
				ASTNode argNode= arguments.get(i);
				if (argNode.equals(methodReferenceNode)) {
					index= i;
					break;
				}
			}
			ITypeBinding[] parameterTypes= methodBinding.getParameterTypes();
			if (index >= parameterTypes.length) {
				// node not found
				return null;
			}
			return new AddMissingMethodDeclarationFixCore(label, compilationUnit, new AddMissingMethodDeclarationProposalOperation(methodReferenceNode, null, methodBinding));
		}
	}

	private static ReturnType getReturnType(Type variableType) {
		ReturnType returnType= new ReturnType();
		if (variableType instanceof ParameterizedType) {
			variableType= (Type) ((ParameterizedType) variableType).typeArguments().get(0);
			ITypeBinding returnTypeBinding= variableType.resolveBinding();
			if (returnTypeBinding != null) {
				if (returnTypeBinding.isCapture()) {
					returnType.binding= returnTypeBinding.getErasure();
				} else if (returnTypeBinding.isWildcardType()) {
					returnType.binding= returnTypeBinding.getBound();
				} else {
					returnType.binding= returnTypeBinding;
				}
			}
		}
		return returnType;
	}

	private static class ReturnType {
		public Type type;

		public ITypeBinding binding;
	}

	private static class AddMissingMethodDeclarationProposalOperation extends CompilationUnitRewriteOperation {


		private ExpressionMethodReference methodReferenceNode;

		private ReturnType returnType;

		private IMethodBinding methodBinding;

		public AddMissingMethodDeclarationProposalOperation(ExpressionMethodReference methodReferenceNode, ReturnType returnType, IMethodBinding methodBinding) {
			if (returnType == null && methodBinding == null) {
				throw new IllegalArgumentException("both returnType and methodBinding cannot be null."); //$NON-NLS-1$
			}

			this.methodReferenceNode= methodReferenceNode;
			this.returnType= returnType;
			this.methodBinding= methodBinding;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
			boolean addStaticModifier= false;
			TypeDeclaration typeDeclaration= ASTNodes.getParent(this.methodReferenceNode, TypeDeclaration.class);

			if (QuickAssistProcessorUtil.isTypeReferenceToInstanceMethod(methodReferenceNode)) {
				addStaticModifier= true;
			}

			AST ast= cuRewrite.getAST();
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			ListRewrite listRewrite= rewrite.getListRewrite(typeDeclaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
			ImportRewrite importRewrite= cuRewrite.getImportRewrite();

			VariableDeclarationStatement variableDeclarationStatement= ASTNodes.getParent(methodReferenceNode, VariableDeclarationStatement.class);
			MethodInvocation methodInvocationNode= ASTNodes.getParent(methodReferenceNode, MethodInvocation.class);
			Assignment variableAssignment= ASTNodes.getParent(methodReferenceNode, Assignment.class);

			if ((variableAssignment != null || variableDeclarationStatement != null) && methodInvocationNode == null) {
				returnType.type= importRewrite.addImport(returnType.binding, ast);

				MethodDeclaration newMethodDeclaration= ast.newMethodDeclaration();
				newMethodDeclaration.setName((SimpleName) rewrite.createCopyTarget(methodReferenceNode.getName()));
				newMethodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PRIVATE_KEYWORD));
				if (addStaticModifier) {
					newMethodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.STATIC_KEYWORD));
				}

				IMethodBinding functionalInterfaceMethod= variableDeclarationStatement == null
						? returnType.binding.getFunctionalInterfaceMethod()
						: variableDeclarationStatement.getType().resolveBinding().getFunctionalInterfaceMethod();
				if (functionalInterfaceMethod != null) {
					returnType.type= importRewrite.addImport(functionalInterfaceMethod.getReturnType(), ast);
					returnType.binding= functionalInterfaceMethod.getReturnType();
					ITypeBinding[] typeArguments= functionalInterfaceMethod.getParameterTypes();
					for (int i= 0; i < typeArguments.length; i++) {
						ITypeBinding iTypeBinding= typeArguments[i];
						SingleVariableDeclaration newSingleVariableDeclaration= ast.newSingleVariableDeclaration();
						newSingleVariableDeclaration.setName(ast.newSimpleName(iTypeBinding.getErasure().getName().toLowerCase() + (i + 1)));
						newSingleVariableDeclaration.setType(importRewrite.addImport(iTypeBinding.getErasure(), ast));
						newMethodDeclaration.parameters().add(newSingleVariableDeclaration);
					}
				}
				newMethodDeclaration.setReturnType2(returnType.type);
				Block newBlock= getNewReturnBlock(ast, returnType.binding);
				newMethodDeclaration.setBody(newBlock);
				listRewrite.insertLast(newMethodDeclaration, null);
			} else {
				List<ASTNode> arguments= methodInvocationNode.arguments();
				int index= -1;
				for (int i= 0; i < arguments.size(); i++) {
					ASTNode node= arguments.get(i);
					if (node.equals(methodReferenceNode)) {
						index= i;
						break;
					}
				}
				ITypeBinding[] parameterTypes= methodBinding.getParameterTypes();
				ITypeBinding[] typeArguments= methodBinding.getTypeArguments();
				ITypeBinding[] parameterTypesFunctionalInterface= parameterTypes[index].getFunctionalInterfaceMethod().getParameterTypes();
				ITypeBinding returnTypeBindingFunctionalInterface= parameterTypes[index].getFunctionalInterfaceMethod().getReturnType();
				MethodDeclaration newMethodDeclaration= ast.newMethodDeclaration();
				newMethodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PRIVATE_KEYWORD));
				if (addStaticModifier) {
					newMethodDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.STATIC_KEYWORD));
				}
				Type newReturnType= null;
				if (returnTypeBindingFunctionalInterface.isPrimitive()) {
					newReturnType= ast.newPrimitiveType(PrimitiveType.toCode(returnTypeBindingFunctionalInterface.getName()));
				} else {
					newReturnType= importRewrite.addImport(returnTypeBindingFunctionalInterface, ast);
					ITypeBinding[] typeParameters= typeDeclaration.resolveBinding().getTypeParameters();
					bIf: if (returnTypeBindingFunctionalInterface.isTypeVariable() || returnTypeBindingFunctionalInterface.isParameterizedType()) {
						for (ITypeBinding typeParameter : typeParameters) {
							// check if parameter type is a Type parameter of the class
							if (Bindings.equals(typeParameter, returnTypeBindingFunctionalInterface)) {
								break bIf;
							}
						}
						TypeParameter newTypeParameter= ast.newTypeParameter();
						newTypeParameter.setName(ast.newSimpleName(returnTypeBindingFunctionalInterface.getName()));
						addIfMissing(newMethodDeclaration, newTypeParameter);
					}
				}
				newMethodDeclaration.setName((SimpleName) rewrite.createCopyTarget(methodReferenceNode.getName()));
				newMethodDeclaration.setReturnType2(newReturnType);
				pLoop: for (int i= 0; i < parameterTypesFunctionalInterface.length; i++) {
					ITypeBinding parameterType2= parameterTypesFunctionalInterface[i];
					SingleVariableDeclaration newSingleVariableDeclaration= ast.newSingleVariableDeclaration();
					if (parameterType2.isCapture()) {
						newSingleVariableDeclaration.setName(ast.newSimpleName(parameterType2.getErasure().getName().toLowerCase() + (i + 1)));
						newSingleVariableDeclaration.setType(importRewrite.addImport(parameterType2.getErasure(), ast));
					} else {
						newSingleVariableDeclaration.setName(ast.newSimpleName(parameterType2.getName().toLowerCase() + (i + 1)));
						newSingleVariableDeclaration.setType(importRewrite.addImport(parameterType2, ast));
					}
					newMethodDeclaration.parameters().add(newSingleVariableDeclaration);
					ITypeBinding[] typeParameters= typeDeclaration.resolveBinding().getTypeParameters();
					if (parameterType2.isTypeVariable()) {
						// check if parameter type is a Type parameter of the class
						for (ITypeBinding typeParameter : typeParameters) {
							if (Bindings.equals(typeParameter, parameterType2)) {
								continue pLoop;
							}
						}

						TypeParameter newTypeParameter= ast.newTypeParameter();
						newTypeParameter.setName(ast.newSimpleName(importRewrite.addImport(parameterType2)));
						ITypeBinding[] typeBounds= parameterType2.getTypeBounds();
						for (ITypeBinding typeBound : typeBounds) {
							newTypeParameter.typeBounds().add(importRewrite.addImport(typeBound, ast));
						}
						addIfMissing(newMethodDeclaration, newTypeParameter);
					}
				}
				for (int i= 0; i < typeArguments.length; i++) {
					ITypeBinding typeArgument= typeArguments[i];
					SingleVariableDeclaration newSingleVariableDeclaration= ast.newSingleVariableDeclaration();
					newSingleVariableDeclaration.setName(ast.newSimpleName(typeArgument.getName().toLowerCase() + (i + 1)));
					newSingleVariableDeclaration.setType(importRewrite.addImport(typeArgument, ast));
					newMethodDeclaration.parameters().add(newSingleVariableDeclaration);
					if (typeArgument.isTypeVariable()) {
						TypeParameter newTypeParameter= ast.newTypeParameter();
						newTypeParameter.setName(ast.newSimpleName(importRewrite.addImport(typeArgument)));
						newMethodDeclaration.typeParameters().add(newTypeParameter);
					}
				}
				Block newBlock= getNewReturnBlock(ast, returnTypeBindingFunctionalInterface);
				newMethodDeclaration.setBody(newBlock);
				listRewrite.insertLast(newMethodDeclaration, null);

			}
		}

		private static void addIfMissing(MethodDeclaration methodDeclaration, TypeParameter newTypeParameter) {
			List<TypeParameter> typeParameters= methodDeclaration.typeParameters();
			for (TypeParameter typeParameter : typeParameters) {
				boolean equals= typeParameter.getName().getFullyQualifiedName().equals(newTypeParameter.getName().getFullyQualifiedName());
				if (equals) {
					return;
				}
			}
			typeParameters.add(newTypeParameter);
		}

		private static Block getNewReturnBlock(AST ast, ITypeBinding returnTypeBinding) {
			Block newBlock= ast.newBlock();
			if (!"void".equals(returnTypeBinding.getName())) { //$NON-NLS-1$
				ReturnStatement newReturnStatement= ast.newReturnStatement();
				String bName= returnTypeBinding.getBinaryName();
				if ("Z".equals(bName)) { //$NON-NLS-1$
					newReturnStatement.setExpression(ast.newBooleanLiteral(false));
				} else if (returnTypeBinding.isPrimitive()) {
					newReturnStatement.setExpression(ast.newNumberLiteral());
				} else if ("java.lang.String".equals(bName)) { //$NON-NLS-1$
					newReturnStatement.setExpression(ast.newStringLiteral());
				} else {
					newReturnStatement.setExpression(ast.newNullLiteral());
				}
				newBlock.statements().add(newReturnStatement);
			}
			return newBlock;
		}

	}
}
