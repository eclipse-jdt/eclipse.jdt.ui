/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Muskalla - [quick fix] Create Method in void context should 'box' void. - https://bugs.eclipse.org/bugs/show_bug.cgi?id=107985
 *     Jens Reimann <jreimann@redhat.com> Bug 38201: [quick assist] Allow creating abstract method - https://bugs.eclipse.org/38201
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite.TypeLocation;

import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

import org.eclipse.jdt.internal.ui.text.correction.ModifierCorrectionSubProcessorCore;

public class NewMethodCorrectionProposal extends AbstractMethodCorrectionProposal {

	private static final String KEY_NAME= "name"; //$NON-NLS-1$
	private static final String KEY_TYPE= "type"; //$NON-NLS-1$

	private List<Expression> fArguments;

	private Map<ITypeBinding, ITypeBinding> fTypeMapping= new HashMap<>();

	//	invocationNode is MethodInvocation, ConstructorInvocation, SuperConstructorInvocation, ClassInstanceCreation, SuperMethodInvocation
	public NewMethodCorrectionProposal(String label, ICompilationUnit targetCU, ASTNode invocationNode,  List<Expression> arguments, ITypeBinding binding, int relevance, Image image) {
		super(label, targetCU, invocationNode, binding, relevance, image);
		fArguments= arguments;
		initializeTypeMapping(invocationNode);
	}

	private void initializeTypeMapping(ASTNode invocationNode) {
		if (invocationNode instanceof MethodInvocation) {
			MethodInvocation methodInvocation= (MethodInvocation)invocationNode;
			Expression methodExpression= methodInvocation.getExpression();
			if (methodExpression != null) {
				ITypeBinding expressionBinding= methodExpression.resolveTypeBinding();
				if (expressionBinding != null) {
					if (expressionBinding.isParameterizedType()) {
						ITypeBinding declaringClass= expressionBinding.getTypeDeclaration();
						ITypeBinding[] declaringClassTypeParameters= declaringClass.getTypeParameters();
						ITypeBinding[] typeArguments= expressionBinding.getTypeArguments();
						for (int i= 0; i < typeArguments.length; ++i) {
							fTypeMapping.put(typeArguments[i], declaringClassTypeParameters[i]);
						}
					}
				}
			}
		}
	}

	protected int evaluateModifiers(ASTNode targetTypeDecl) {
		if (getSenderBinding().isAnnotation() || getSenderBinding().isEnum()) {
			return 0;
		}
		boolean isTargetInterface= getSenderBinding().isInterface();
		if (isTargetInterface && !JavaModelUtil.is1d8OrHigher(getCompilationUnit().getJavaProject())) {
			// only abstract methods are allowed for interface present in less than Java 1.8
			return getInterfaceMethodModifiers(targetTypeDecl, true);
		}
		ASTNode invocationNode= getInvocationNode();
		if (invocationNode instanceof MethodInvocation) {
			int modifiers= 0;
			Expression expression= ((MethodInvocation)invocationNode).getExpression();
			if (expression != null) {
				if (expression instanceof Name && ((Name) expression).resolveBinding().getKind() == IBinding.TYPE) {
					modifiers |= Modifier.STATIC;
				}
			} else if (ASTResolving.isInStaticContext(invocationNode)) {
				modifiers |= Modifier.STATIC;
			}
			ASTNode node= ASTResolving.findParentType(invocationNode);
			boolean isParentInterface= node instanceof TypeDeclaration && ((TypeDeclaration) node).isInterface();
			if (isTargetInterface || isParentInterface) {
				if (expression == null && !targetTypeDecl.equals(node)) {
					modifiers|= Modifier.STATIC;
					if (isTargetInterface) {
						modifiers|= getInterfaceMethodModifiers(targetTypeDecl, false);
					} else {
						modifiers|= Modifier.PROTECTED;
					}
				} else if (modifiers == Modifier.STATIC) {
					modifiers= getInterfaceMethodModifiers(targetTypeDecl, false) | Modifier.STATIC;
				} else {
					modifiers= getInterfaceMethodModifiers(targetTypeDecl, true);
				}
			} else if (targetTypeDecl.equals(node)) {
				modifiers |= Modifier.PRIVATE;
			} else if (node instanceof AnonymousClassDeclaration && ASTNodes.isParent(node, targetTypeDecl)) {
				modifiers |= Modifier.PROTECTED;
				if (ASTResolving.isInStaticContext(node) && expression == null) {
					modifiers |= Modifier.STATIC;
				}
			} else {
				modifiers |= Modifier.PUBLIC;
			}
			return modifiers;
		}
		return Modifier.PUBLIC;
	}

	private int getInterfaceMethodModifiers(ASTNode targetTypeDecl, boolean createAbstractMethod) {
		// for interface and annotation members copy the modifiers from an existing member
		if (targetTypeDecl instanceof TypeDeclaration) {
			TypeDeclaration type= (TypeDeclaration) targetTypeDecl;
			MethodDeclaration[] methodDecls= type.getMethods();
			if (methodDecls.length > 0) {
				if (createAbstractMethod) {
					for (MethodDeclaration methodDeclaration : methodDecls) {
						IMethodBinding methodBinding= methodDeclaration.resolveBinding();
						if (methodBinding != null && JdtFlags.isAbstract(methodBinding)) {
							return methodDeclaration.getModifiers();
						}
					}
				}
				return methodDecls[0].getModifiers() & Modifier.PUBLIC;
			}
			List<BodyDeclaration> bodyDecls= type.bodyDeclarations();
			if (bodyDecls.size() > 0) {
				return bodyDecls.get(0).getModifiers() & Modifier.PUBLIC;
			}
		}
		return 0;
	}

	@Override
	protected void addNewModifiers(ASTRewrite rewrite, ASTNode targetTypeDecl, List<IExtendedModifier> modifiers) {
		modifiers.addAll(rewrite.getAST().newModifiers(evaluateModifiers(targetTypeDecl)));
		ModifierCorrectionSubProcessorCore.installLinkedVisibilityProposals(getLinkedProposalModel(), rewrite, modifiers, getSenderBinding().isInterface());
	}

	@Override
	protected boolean isConstructor() {
		ASTNode node= getInvocationNode();

		return node.getNodeType() != ASTNode.METHOD_INVOCATION && node.getNodeType() != ASTNode.SUPER_METHOD_INVOCATION;
	}

	@Override
	protected SimpleName getNewName(ASTRewrite rewrite) {
		ASTNode invocationNode= getInvocationNode();
		String name;
		if (invocationNode instanceof MethodInvocation) {
			name= ((MethodInvocation)invocationNode).getName().getIdentifier();
		} else if (invocationNode instanceof SuperMethodInvocation) {
			name= ((SuperMethodInvocation)invocationNode).getName().getIdentifier();
		} else {
			name= getSenderBinding().getName(); // name of the class
		}
		AST ast= rewrite.getAST();
		SimpleName newNameNode= ast.newSimpleName(name);
		addLinkedPosition(rewrite.track(newNameNode), false, KEY_NAME);

		ASTNode invocationName= getInvocationNameNode();
		if (invocationName != null && invocationName.getAST() == ast) { // in the same CU
			addLinkedPosition(rewrite.track(invocationName), true, KEY_NAME);
		}
		return newNameNode;
	}

	private ASTNode getInvocationNameNode() {
		ASTNode node= getInvocationNode();
		if (node instanceof MethodInvocation) {
			return ((MethodInvocation)node).getName();
		} else if (node instanceof SuperMethodInvocation) {
			return ((SuperMethodInvocation)node).getName();
		} else if (node instanceof ClassInstanceCreation) {
			Type type= ((ClassInstanceCreation)node).getType();
			while (type instanceof ParameterizedType) {
				type= ((ParameterizedType) type).getType();
			}
			return type;
		}
		return null;
	}

	@Override
	protected Type getNewMethodType(ASTRewrite rewrite, ImportRewriteContext importRewriteContext) throws CoreException {
		ASTNode node= getInvocationNode();
		AST ast= rewrite.getAST();

		Type newTypeNode= null;
		ITypeBinding[] otherProposals= null;

		if (node.getParent() instanceof MethodInvocation) {
			MethodInvocation parent= (MethodInvocation) node.getParent();
			if (parent.getExpression() == node) {
				ITypeBinding[] bindings= ASTResolving.getQualifierGuess(node.getRoot(), parent.getName().getIdentifier(), parent.arguments(), getSenderBinding());
				if (bindings.length > 0) {
					ITypeBinding firstBinding= getClassTypeParameterBinding(bindings[0]);
					newTypeNode= getImportRewrite().addImport(firstBinding, ast, importRewriteContext, TypeLocation.RETURN_TYPE);
					otherProposals= bindings;
				}
			}
		}
		if (newTypeNode == null) {
			ITypeBinding binding= ASTResolving.guessBindingForReference(node);
			if (binding != null && binding.isWildcardType()) {
				binding= ASTResolving.normalizeWildcardType(binding, false, ast);
			}
			if (binding != null) {
				binding= getClassTypeParameterBinding(binding);
				newTypeNode= getImportRewrite().addImport(binding, ast, importRewriteContext, TypeLocation.RETURN_TYPE);
			} else {
				ASTNode parent= node.getParent();
				if (parent instanceof ExpressionStatement) {
					newTypeNode= ast.newPrimitiveType(PrimitiveType.VOID);
				} else {
					newTypeNode= org.eclipse.jdt.internal.ui.text.correction.ASTResolving.guessTypeForReference(ast, node);
					if (newTypeNode == null) {
						newTypeNode= ast.newSimpleType(ast.newSimpleName("Object")); //$NON-NLS-1$
					}
				}
			}
		}

		addLinkedPosition(rewrite.track(newTypeNode), false, KEY_TYPE);
		if (otherProposals != null) {
			for (ITypeBinding otherProposal : otherProposals) {
				addLinkedPositionProposal(KEY_TYPE, otherProposal);
			}
		}

		return newTypeNode;
	}

	@Override
	protected void addNewParameters(ASTRewrite rewrite, List<String> takenNames, List<SingleVariableDeclaration> params, ImportRewriteContext context) throws CoreException {
		AST ast= rewrite.getAST();

		List<Expression> arguments= fArguments;

		for (int i= 0; i < arguments.size(); i++) {
			Expression elem= arguments.get(i);
			SingleVariableDeclaration param= ast.newSingleVariableDeclaration();

			// argument type
			String argTypeKey= "arg_type_" + i; //$NON-NLS-1$
			Type type= evaluateParameterType(ast, elem, argTypeKey, context);
			param.setType(type);

			// argument name
			String argNameKey= "arg_name_" + i; //$NON-NLS-1$
			String name= evaluateParameterName(takenNames, elem, type, argNameKey);
			param.setName(ast.newSimpleName(name));

			params.add(param);

			addLinkedPosition(rewrite.track(param.getType()), false, argTypeKey);
			addLinkedPosition(rewrite.track(param.getName()), false, argNameKey);
		}
	}

	private Type evaluateParameterType(AST ast, Expression elem, String key, ImportRewriteContext context) {
		ITypeBinding binding= Bindings.normalizeTypeBinding(elem.resolveTypeBinding());
		if (binding != null && binding.isWildcardType()) {
			binding= ASTResolving.normalizeWildcardType(binding, true, ast);
		}
		if (binding != null) {
			binding= getClassTypeParameterBinding(binding);
			for (ITypeBinding typeProposal : ASTResolving.getRelaxingTypes(ast, binding)) {
				addLinkedPositionProposal(key, typeProposal);
			}
			return getImportRewrite().addImport(binding, ast, context, TypeLocation.PARAMETER);
		}
		return ast.newSimpleType(ast.newSimpleName("Object")); //$NON-NLS-1$
	}

	private ITypeBinding getClassTypeParameterBinding(ITypeBinding binding) {
		ITypeBinding[] typeParameters= new ITypeBinding[0];
		ITypeBinding declaringClassBinding= binding.getDeclaringClass();
		if (declaringClassBinding != null && declaringClassBinding.isGenericType()) {
			typeParameters= declaringClassBinding.getTypeParameters();
		} else {
			IMethodBinding methodBinding= binding.getDeclaringMethod();
			if (methodBinding != null) {
				typeParameters= methodBinding.getTypeParameters();
			}
		}
		for (ITypeBinding typeParameter : typeParameters) {
			if (typeParameter.isEqualTo(binding)) {
				if (fTypeMapping.containsKey(binding)) {
					return fTypeMapping.get(binding);
				}
			}
		}
		return binding;
	}

	private String evaluateParameterName(List<String> takenNames, Expression argNode, Type type, String key) {
		IJavaProject project= getCompilationUnit().getJavaProject();
		String[] names= StubUtility.getVariableNameSuggestions(NamingConventions.VK_PARAMETER, project, type, argNode, takenNames);
		for (String name : names) {
			addLinkedPositionProposal(key, name, null);
		}
		String favourite= names[0];
		takenNames.add(favourite);
		return favourite;
	}

	@Override
	protected void addNewExceptions(ASTRewrite rewrite, List<Type> exceptions, ImportRewriteContext context) throws CoreException {
	}

	private void getTypeParameters(ITypeBinding binding, Set<ITypeBinding> typeParametersFound) {
		if (!getClassTypeParameterBinding(binding).isEqualTo(binding)) {
			return;
		}
		IMethodBinding methodBinding= binding.getDeclaringMethod();
		if (methodBinding != null) {
			ITypeBinding[] typeParameters= methodBinding.getTypeParameters();
			for (ITypeBinding typeParameter : typeParameters) {
				if (typeParameter.isEqualTo(binding)) {
					typeParametersFound.add(typeParameter);
					ITypeBinding[] typeBounds= typeParameter.getTypeBounds();
					for (ITypeBinding typeBound : typeBounds) {
						getTypeParameters(typeBound, typeParametersFound);
					}
				}
			}
		} else {
			ITypeBinding declaringClassBinding= binding.getDeclaringClass();
			if (declaringClassBinding != null) {
				ASTNode invocationNode= getInvocationNode();
				if (invocationNode instanceof MethodInvocation
						&& ((MethodInvocation)invocationNode).getExpression() != null
						&& !(((MethodInvocation)invocationNode).getExpression() instanceof ThisExpression)) {
					ITypeBinding[] typeParameters= declaringClassBinding.getTypeParameters();
					for (ITypeBinding typeParameter : typeParameters) {
						if (typeParameter.isEqualTo(binding)) {
							typeParametersFound.add(typeParameter);
							ITypeBinding[] typeBounds= typeParameter.getTypeBounds();
							for (ITypeBinding typeBound : typeBounds) {
								getTypeParameters(typeBound, typeParametersFound);
							}
						}
					}
				}
			}
		}
	}

	@Override
	protected void addNewTypeParameters(ASTRewrite rewrite, List<String> takenNames, List<TypeParameter> params,
			ImportRewriteContext context) throws CoreException {
		AST ast= rewrite.getAST();
		ASTNode node= getInvocationNode();
		Set<ITypeBinding> typeParametersFound= new LinkedHashSet<>();

		ITypeBinding returnTypeBinding= null;

		if (!isConstructor()) {
			if (node.getParent() instanceof MethodInvocation) {
				MethodInvocation parent= (MethodInvocation) node.getParent();
				if (parent.getExpression() == node) {
					ITypeBinding[] bindings= ASTResolving.getQualifierGuess(node.getRoot(), parent.getName().getIdentifier(), parent.arguments(), getSenderBinding());
					if (bindings.length > 0) {
						returnTypeBinding= bindings[0];
					}
				}
			}
			if (returnTypeBinding == null) {
				ITypeBinding binding= ASTResolving.guessBindingForReference(node);
				if (binding != null && binding.isWildcardType()) {
					binding= ASTResolving.normalizeWildcardType(binding, false, ast);
				}
				returnTypeBinding= binding;
			}

			if (returnTypeBinding != null) {
				getTypeParameters(returnTypeBinding, typeParametersFound);
			}
		}

		List<Expression> arguments= fArguments;

		for (int i= 0; i < arguments.size(); i++) {
			Expression elem= arguments.get(i);
			ITypeBinding binding= Bindings.normalizeTypeBinding(elem.resolveTypeBinding());
			if (binding != null && binding.isWildcardType()) {
				binding= ASTResolving.normalizeWildcardType(binding, true, ast);
			}
			if (binding != null) {
				getTypeParameters(binding, typeParametersFound);
			}
		}

		for (ITypeBinding m : typeParametersFound) {
			TypeParameter newTypeParameter= ast.newTypeParameter();
			newTypeParameter.setName(ast.newSimpleName(m.getName()));
			params.add(newTypeParameter);
			ITypeBinding[] bounds= m.getTypeBounds();
			List<Type> newTypeBounds= newTypeParameter.typeBounds();
			for (ITypeBinding bound : bounds) {
				Type t= getImportRewrite().addImport(bound, ast, context, TypeLocation.TYPE_PARAMETER);
				newTypeBounds.add(t);
			}
		}
	}
}
