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
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.CreationReference;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodReference;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.TypeMethodReference;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;

import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility2Core;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.JdtASTMatcher;
import org.eclipse.jdt.internal.corext.fix.helper.LambdaQueries;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.text.correction.QuickAssistProcessorUtil;

public class ConvertLambdaToMethodReferenceFixCore extends CompilationUnitRewriteOperationsFixCore {

	public ConvertLambdaToMethodReferenceFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation operation) {
		super(name, compilationUnit, operation);
	}

	public static ConvertLambdaToMethodReferenceFixCore createConvertLambdaToMethodReferenceFix(CompilationUnit compilationUnit, ASTNode node) {
		LambdaExpression lambda;
		if (node instanceof LambdaExpression) {
			lambda= (LambdaExpression) node;
		} else if (node.getLocationInParent() == LambdaExpression.BODY_PROPERTY) {
			lambda= (LambdaExpression) node.getParent();
		} else {
			lambda= ASTResolving.findEnclosingLambdaExpression(node);
			if (lambda == null) {
				return null;
			}
		}

		ASTNode lambdaBody= lambda.getBody();
		Expression exprBody;
		if (lambdaBody instanceof Block) {
			exprBody= LambdaQueries.getSingleExpressionFromLambdaBody((Block) lambdaBody);
		} else {
			exprBody= (Expression) lambdaBody;
		}
		exprBody= ASTNodes.getUnparenthesedExpression(exprBody);
		if (exprBody == null || !isValidLambdaReferenceToMethod(exprBody))
			return null;

		if (!ASTNodes.isParent(exprBody, node)
				&& !representsDefiningNode(node, exprBody)) {
			return null;
		}

		List<Expression> lambdaParameters= new ArrayList<>();
		for (VariableDeclaration param : (List<VariableDeclaration>) lambda.parameters()) {
			lambdaParameters.add(param.getName());
		}
		if (exprBody instanceof ClassInstanceCreation) {
			ClassInstanceCreation cic= (ClassInstanceCreation) exprBody;
			if (cic.getExpression() != null || cic.getAnonymousClassDeclaration() != null)
				return null;
			if (!matches(lambdaParameters, cic.arguments()))
				return null;
		} else if (exprBody instanceof ArrayCreation) {
			List<Expression> dimensions= ((ArrayCreation) exprBody).dimensions();
			if (dimensions.size() != 1)
				return null;
			if (!matches(lambdaParameters, dimensions))
				return null;
		} else if (exprBody instanceof SuperMethodInvocation) {
			SuperMethodInvocation superMethodInvocation= (SuperMethodInvocation) exprBody;
			IMethodBinding methodBinding= superMethodInvocation.resolveMethodBinding();
			if (methodBinding == null)
				return null;
			if (Modifier.isStatic(methodBinding.getModifiers())) {
				ITypeBinding invocationTypeBinding= ASTNodes.getInvocationType(superMethodInvocation, methodBinding, superMethodInvocation.getQualifier());
				if (invocationTypeBinding == null)
					return null;
			}
			if (!matches(lambdaParameters, superMethodInvocation.arguments()))
				return null;
		} else if (exprBody instanceof InstanceofExpression) {
			InstanceofExpression instanceofExpression= (InstanceofExpression) exprBody;
			if (instanceofExpression.getRightOperand().resolveBinding() == null)
				return null;
			if (!matches(lambdaParameters, List.of(instanceofExpression.getLeftOperand())))
				return null;
		} else { // MethodInvocation
			MethodInvocation methodInvocation= (MethodInvocation) exprBody;
			IMethodBinding methodBinding= methodInvocation.resolveMethodBinding();
			if (methodBinding == null)
				return null;

			Expression invocationExpr= methodInvocation.getExpression();
			if (Modifier.isStatic(methodBinding.getModifiers())) {
				ITypeBinding invocationTypeBinding= ASTNodes.getInvocationType(methodInvocation, methodBinding, invocationExpr);
				if (invocationTypeBinding == null)
					return null;
				if (!matches(lambdaParameters, methodInvocation.arguments()))
					return null;
			} else if ((lambda.parameters().size() - methodInvocation.arguments().size()) == 1) {
				if (invocationExpr == null)
					return null;
				ITypeBinding invocationTypeBinding= invocationExpr.resolveTypeBinding();
				if (invocationTypeBinding == null)
					return null;
				IMethodBinding lambdaMethodBinding= lambda.resolveMethodBinding();
				if (lambdaMethodBinding == null)
					return null;
				ITypeBinding firstParamType= lambdaMethodBinding.getParameterTypes()[0];
				if ((!Bindings.equals(invocationTypeBinding, firstParamType) && !Bindings.isSuperType(invocationTypeBinding, firstParamType))
						|| !JdtASTMatcher.doNodesMatch(lambdaParameters.get(0), invocationExpr)
						|| !matches(lambdaParameters.subList(1, lambdaParameters.size()), methodInvocation.arguments()))
					return null;
			} else if (!matches(lambdaParameters, methodInvocation.arguments())) {
				return null;
			}
		}

		String label= CorrectionMessages.QuickAssistProcessor_convert_to_method_reference;
		return new ConvertLambdaToMethodReferenceFixCore(label, compilationUnit, new ConvertLambdaToMethodReferenceProposalOperation(lambda, exprBody));
	}

	public static boolean matches(List<Expression> expected, List<Expression> toMatch) {
		if (toMatch.size() != expected.size())
			return false;
		for (int i= 0; i < toMatch.size(); i++) {
			if (!JdtASTMatcher.doNodesMatch(expected.get(i), toMatch.get(i)))
				return false;
		}
		return true;
	}

	public static boolean representsDefiningNode(ASTNode innerNode, ASTNode definingNode) {
		// Example: We want to enable the proposal when the method invocation node or
		// the method name is near the caret. But not when the caret is on an argument of the method invocation.
		if (innerNode == definingNode)
			return true;

		switch (definingNode.getNodeType()) {
			// types from isValidLambdaReferenceToMethod():
			case ASTNode.CLASS_INSTANCE_CREATION:
				return representsDefiningNode(innerNode, ((ClassInstanceCreation) definingNode).getType());
			case ASTNode.ARRAY_CREATION:
				return representsDefiningNode(innerNode, ((ArrayCreation) definingNode).getType());
			case ASTNode.SUPER_METHOD_INVOCATION:
				return innerNode == ((SuperMethodInvocation) definingNode).getName();
			case ASTNode.METHOD_INVOCATION:
				return innerNode == ((MethodInvocation) definingNode).getName();

			// subtypes of Type:
			case ASTNode.NAME_QUALIFIED_TYPE:
				return innerNode == ((NameQualifiedType) definingNode).getName();
			case ASTNode.QUALIFIED_TYPE:
				return innerNode == ((QualifiedType) definingNode).getName();
			case ASTNode.SIMPLE_TYPE:
				return innerNode == ((SimpleType) definingNode).getName();
			case ASTNode.ARRAY_TYPE:
				return representsDefiningNode(innerNode, ((ArrayType) definingNode).getElementType());
			case ASTNode.PARAMETERIZED_TYPE:
				return representsDefiningNode(innerNode, ((ParameterizedType) definingNode).getType());

			default:
				return false;
		}
	}

	public static boolean isValidLambdaReferenceToMethod(Expression expression) {
		return expression instanceof ClassInstanceCreation
				|| expression instanceof ArrayCreation
				|| expression instanceof SuperMethodInvocation
				|| expression instanceof MethodInvocation
				|| expression instanceof InstanceofExpression;
	}

	private static class ConvertLambdaToMethodReferenceProposalOperation extends CompilationUnitRewriteOperation {

		private LambdaExpression lambda;

		private Expression exprBody;

		public ConvertLambdaToMethodReferenceProposalOperation(LambdaExpression lambda, Expression exprBody) {
			this.lambda= lambda;
			this.exprBody= exprBody;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
			AST ast= cuRewrite.getAST();
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			ASTNode replacement;

			if (exprBody instanceof ClassInstanceCreation) {
				CreationReference creationReference= ast.newCreationReference();
				replacement= creationReference;

				ClassInstanceCreation cic= (ClassInstanceCreation) exprBody;
				Type type= cic.getType();
				if (type.isParameterizedType() && ((ParameterizedType) type).typeArguments().size() == 0) {
					type= ((ParameterizedType) type).getType();
				}
				creationReference.setType((Type) rewrite.createCopyTarget(type));
				creationReference.typeArguments().addAll(QuickAssistProcessorUtil.getCopiedTypeArguments(rewrite, cic.typeArguments()));
			} else if (exprBody instanceof ArrayCreation) {
				CreationReference creationReference= ast.newCreationReference();

				ArrayType arrayType= ((ArrayCreation) exprBody).getType();
				Type copiedElementType= (Type) rewrite.createCopyTarget(arrayType.getElementType());
				creationReference.setType(ast.newArrayType(copiedElementType, arrayType.getDimensions()));
				replacement= castMethodRefIfNeeded(cuRewrite, ast, creationReference);
			} else if (exprBody instanceof SuperMethodInvocation) {
				SuperMethodInvocation superMethodInvocation= (SuperMethodInvocation) exprBody;
				IMethodBinding methodBinding= superMethodInvocation.resolveMethodBinding();
				Name superQualifier= superMethodInvocation.getQualifier();

				if (Modifier.isStatic(methodBinding.getModifiers())) {
					TypeMethodReference typeMethodReference= ast.newTypeMethodReference();

					typeMethodReference.setName((SimpleName) rewrite.createCopyTarget(superMethodInvocation.getName()));
					ImportRewrite importRewrite= cuRewrite.getImportRewrite();
					ITypeBinding invocationTypeBinding= ASTNodes.getInvocationType(superMethodInvocation, methodBinding, superQualifier);
					typeMethodReference.setType(importRewrite.addImport(invocationTypeBinding.getTypeDeclaration().getErasure(), ast));
					typeMethodReference.typeArguments().addAll(QuickAssistProcessorUtil.getCopiedTypeArguments(rewrite, superMethodInvocation.typeArguments()));
					replacement= castMethodRefIfNeeded(cuRewrite, ast, typeMethodReference);
				} else {
					SuperMethodReference superMethodReference= ast.newSuperMethodReference();

					if (superQualifier != null) {
						superMethodReference.setQualifier((Name) rewrite.createCopyTarget(superQualifier));
					}
					superMethodReference.setName((SimpleName) rewrite.createCopyTarget(superMethodInvocation.getName()));
					superMethodReference.typeArguments().addAll(QuickAssistProcessorUtil.getCopiedTypeArguments(rewrite, superMethodInvocation.typeArguments()));
					replacement= castMethodRefIfNeeded(cuRewrite, ast, superMethodReference);
				}
			} else if (exprBody instanceof InstanceofExpression) {
				InstanceofExpression instanceofExpression= (InstanceofExpression) exprBody;
				ExpressionMethodReference expMethodReference= ast.newExpressionMethodReference();
				TypeLiteral typeLiteral= ast.newTypeLiteral();
				ImportRewrite importRewrite= cuRewrite.getImportRewrite();
				ITypeBinding instanceofTypeBinding= instanceofExpression.getRightOperand().resolveBinding();
				typeLiteral.setType(importRewrite.addImport(instanceofTypeBinding.getTypeDeclaration().getErasure(), ast));
				expMethodReference.setName(ast.newSimpleName("isInstance")); //$NON-NLS-1$
				expMethodReference.setExpression(typeLiteral);
				replacement= castMethodRefIfNeeded(cuRewrite, ast, expMethodReference);
			} else { // MethodInvocation
				MethodInvocation methodInvocation= (MethodInvocation) exprBody;
				IMethodBinding methodBinding= methodInvocation.resolveMethodBinding();
				Expression invocationQualifier= methodInvocation.getExpression();

				boolean isStaticMethod= Modifier.isStatic(methodBinding.getModifiers());
				boolean isTypeRefToInstanceMethod= methodInvocation.arguments().size() != lambda.parameters().size();

				if (isStaticMethod || isTypeRefToInstanceMethod) {
					TypeMethodReference typeMethodReference= ast.newTypeMethodReference();

					typeMethodReference.setName((SimpleName) rewrite.createCopyTarget(methodInvocation.getName()));
					ImportRewrite importRewrite= cuRewrite.getImportRewrite();
					ITypeBinding invocationTypeBinding= ASTNodes.getInvocationType(methodInvocation, methodBinding, invocationQualifier);
					invocationTypeBinding= StubUtility2Core.replaceWildcardsAndCaptures(invocationTypeBinding);
					ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(lambda, importRewrite);
					typeMethodReference.setType(importRewrite.addImport(invocationTypeBinding.getErasure(), ast, importRewriteContext, TypeLocation.OTHER));
					typeMethodReference.typeArguments().addAll(QuickAssistProcessorUtil.getCopiedTypeArguments(rewrite, methodInvocation.typeArguments()));
					replacement= castMethodRefIfNeeded(cuRewrite, ast, typeMethodReference);
				} else {
					ExpressionMethodReference exprMethodReference= ast.newExpressionMethodReference();

					exprMethodReference.setName((SimpleName) rewrite.createCopyTarget(methodInvocation.getName()));
					if (invocationQualifier != null) {
						exprMethodReference.setExpression((Expression) rewrite.createCopyTarget(invocationQualifier));
					} else {
						// check if method is in class scope or in super/nested class scope
						TypeDeclaration lambdaParentType= (TypeDeclaration) ASTResolving.findParentType(lambda);
						ITypeBinding lambdaMethodInvokingClass= lambdaParentType.resolveBinding();
						ITypeBinding lambdaMethodDeclaringClass= methodBinding.getDeclaringClass();

						ThisExpression newThisExpression= ast.newThisExpression();

						ITypeBinding nestedRootClass= getNestedRootClass(lambdaMethodInvokingClass);
						boolean isSuperClass= isSuperClass(lambdaMethodDeclaringClass, lambdaMethodInvokingClass);
						boolean isNestedClass= isNestedClass(lambdaMethodDeclaringClass, lambdaMethodInvokingClass);

						if (lambdaMethodDeclaringClass == lambdaMethodInvokingClass) {
							// use this::
						} else if (Modifier.isDefault(methodBinding.getModifiers())) {
							boolean nestedInterfaceClass= isNestedInterfaceClass(ast, lambdaMethodDeclaringClass, lambdaMethodInvokingClass);
							if (isNestedClass
									|| (nestedInterfaceClass && !isSuperClass)) {
								// Use this::
							} else if (!nestedInterfaceClass || (nestedRootClass != lambdaMethodInvokingClass)) {
								newThisExpression.setQualifier(ast.newName(nestedRootClass.getName()));
							}
						} else if (lambdaMethodDeclaringClass.isInterface()) {
							if (isSuperClass) {
								// use this::
							} else {
								newThisExpression.setQualifier(ast.newName(nestedRootClass.getName()));
							}
						} else if (isSuperClass) {
							// use this::
						} else {
							newThisExpression.setQualifier(ast.newName(nestedRootClass.getName()));
						}
						exprMethodReference.setExpression(newThisExpression);
					}
					exprMethodReference.typeArguments().addAll(QuickAssistProcessorUtil.getCopiedTypeArguments(rewrite, methodInvocation.typeArguments()));
					replacement= castMethodRefIfNeeded(cuRewrite, ast, exprMethodReference);
				}
			}

			ASTNodes.replaceButKeepComment(rewrite, lambda, replacement, null);
		}

		private ASTNode castMethodRefIfNeeded(final CompilationUnitRewrite cuRewrite, AST ast, Expression methodRef) {
			boolean needCast= false;
			ASTNode replacementNode= methodRef;
			if (lambda.getLocationInParent() == MethodInvocation.ARGUMENTS_PROPERTY) {
				MethodInvocation parent= (MethodInvocation) lambda.getParent();
				List<Expression> args= parent.arguments();
				IMethodBinding parentBinding= parent.resolveMethodBinding().getMethodDeclaration();
				if (parentBinding != null) {
					ITypeBinding parentTypeBinding= parentBinding.getDeclaringClass().getErasure();
					while (parentTypeBinding != null) {
						IMethodBinding[] parentTypeMethods= parentTypeBinding.getDeclaredMethods();
						for (IMethodBinding parentTypeMethod : parentTypeMethods) {
							if (parentTypeMethod.getName().equals(parentBinding.getName())
									&& parentTypeMethod.getParameterTypes().length == args.size()
									&& !parentTypeMethod.isEqualTo(parentBinding)) {
								needCast= true;
								break;
							}
						}
						if (!needCast) {
							parentTypeBinding= parentTypeBinding.getSuperclass();
						} else {
							break;
						}
					}
					if (needCast) {
						for (Expression arg : args) {
							if (arg == lambda) {
								CastExpression cast= ast.newCastExpression();
								cast.setExpression(methodRef);
								ITypeBinding argTypeBinding= arg.resolveTypeBinding();
								if (argTypeBinding == null) {
									return replacementNode;
								}
								ImportRewrite importRewriter= cuRewrite.getImportRewrite();
								Type argType= importRewriter.addImport(argTypeBinding, ast);
								cast.setType(argType);
								replacementNode= cast;
							}
						}
					}
				}
			}
			return replacementNode;
		}

		/*
		 * return TRUE if method declaration interface is super class of lambda declaration class
		 */
		public static boolean isNestedInterfaceClass(AST ast, ITypeBinding lambdaMethodDeclaringClass, ITypeBinding lambdaMethodInvokingClass) {
			ITypeBinding[] methodNarrowingTypes= ASTResolving.getRelaxingTypes(ast, lambdaMethodDeclaringClass);
			ITypeBinding[] lambdaNarrowingTypes= ASTResolving.getRelaxingTypes(ast, lambdaMethodInvokingClass);

			if (methodNarrowingTypes.length != 1) {
				return false;
			}
			ITypeBinding methodNarrowingType= methodNarrowingTypes[0];
			for (ITypeBinding lambdaNarrowingType : lambdaNarrowingTypes) {
				if (methodNarrowingType == lambdaNarrowingType) {
					return true;
				}
			}
			return false;
		}

		/*
		 * return TRUE if method declaration class is super class of lambda declaration class
		 */
		public static boolean isSuperClass(ITypeBinding methodDeclarationType, ITypeBinding lambdaDeclarationType) {
			ITypeBinding parent= lambdaDeclarationType.getSuperclass();
			while (parent != null) {
				if (parent == methodDeclarationType) {
					return true;
				}
				parent= parent.getSuperclass();
			}
			return false;
		}

		/*
		 * return TRUE if lambda declaration class is nested class of method declaration class
		 */
		public static boolean isNestedClass(ITypeBinding methodDeclarationType, ITypeBinding lambdaDeclarationType) {
			ITypeBinding parent= lambdaDeclarationType;
			while (parent.isNested()) {
				parent= parent.getDeclaringClass();
				if (parent == methodDeclarationType) {
					return true;
				}
			}
			return false;
		}

		public static ITypeBinding getNestedRootClass(ITypeBinding lambdaDeclarationType) {
			ITypeBinding parent= lambdaDeclarationType;
			while (parent.isNested()) {
				parent= parent.getDeclaringClass();
			}
			return parent;
		}
	}

}
