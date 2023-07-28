/*******************************************************************************
 * Copyright (c) 2023 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.corext.refactoring.code.makestatic;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

/**
 *
 * The InstanceUsageRewriter class represents a utility class for the MakeStaticRefactoring class.
 * It is used for rewriting instance usages within a method.
 *
 * <p>
 * This class extends {@code ASTVisitor} and provides functionality to determine whether there is
 * access to instance variables or instance methods within the body of the method being visited.
 * </p>
 *
 * It also provides functionality to transform class instance creations.
 *
 * If an explicit super method invocation will be found by this visitor a status error will be
 * thrown.
 *
 */
public final class InstanceUsageRewriter extends ASTVisitor {

	/**
	 * Indicates whether there is access to instance variables or instance methods within the body
	 * of the method.
	 */
	public boolean fTargetMethodhasInstanceUsage;

	/**
	 * The name of the parameter that is used to access instance variables or instance methods after
	 * refactoring. For example "this" and "super" will be transformed to the value of fParamName.
	 */
	private final String fParamName;

	private final ASTRewrite fRewrite;

	private final AST fAst;

	private final MethodDeclaration fTargetMethodDeclaration;

	private FinalConditionsChecker fFinalConditionsChecker;


	/**
	 * Constructs a new InstanceUsageRewriter with the specified parameters.
	 *
	 * @param paramName The name of the parameter used to access instance variables or instance
	 *            methods.
	 * @param rewrite The ASTRewrite object used for rewriting the AST.
	 * @param ast The AST object representing the abstract syntax tree.
	 * @param finalConditionsChecker The FinalConditionsChecker instance used for final conditions
	 *            checking during the refactoring.
	 * @param methodDeclaration The MethodDeclaration object representing the target method being
	 *            refactored.
	 */
	public InstanceUsageRewriter(String paramName, ASTRewrite rewrite, AST ast, MethodDeclaration methodDeclaration, FinalConditionsChecker finalConditionsChecker) {
		fParamName= paramName;
		fRewrite= rewrite;
		fAst= ast;
		fTargetMethodDeclaration= methodDeclaration;
		fFinalConditionsChecker= finalConditionsChecker;
	}

	/**
	 * Gets the flag indicating whether the target method has instance usages or not.
	 *
	 * @return {@code true} if the target method has instance usages ("this" or "super"),
	 *         {@code false} otherwise.
	 *
	 */
	public boolean getTargetMethodhasInstanceUsage() {
		return fTargetMethodhasInstanceUsage;
	}

	/**
	 * Visits a SimpleName node and modifies its usage depending on the type of binding.
	 *
	 * @param node The SimpleName node being visited. It represents a simple name in the abstract
	 *            syntax tree (AST).
	 * @return {@code true} to continue visiting the children of this node, {@code false} otherwise.
	 */
	@Override
	public boolean visit(SimpleName node) {
		IBinding binding= node.resolveBinding();
		if (binding instanceof IVariableBinding) {
			modifyFieldUsage(node, binding);
		} else if (binding instanceof IMethodBinding) {
			modifyInstanceMethodUsage(node, binding);
		}
		return super.visit(node);
	}

	/**
	 * Visits a ThisExpression node and replaces it with a new expression based on the context. This
	 * method is used during the instance usage rewriting process to handle 'this' expressions,
	 * which represent the current instance reference, and replace them appropriately based on the
	 * context. If the 'this' expression is inside an anonymous class, it is left unchanged.
	 * Otherwise, it is replaced with a new expression to access instance variables or methods using
	 * the specified parameters.
	 *
	 * @param node The ThisExpression node being visited. It represents a 'this' expression in the
	 *            abstract syntax tree (AST).
	 *
	 * @return {@code true} to continue visiting the children of this node, {@code false} otherwise.
	 */
	@Override
	public boolean visit(ThisExpression node) {
		ASTNode parentNode= node.getParent();

		Name qualifier= node.getQualifier();

		if (qualifier != null) {
			IBinding qualifierBinding= qualifier.resolveBinding();
			ITypeBinding typeBinding= (ITypeBinding) qualifierBinding;
			if (isInsideAnonymousClass(typeBinding)) {
				return super.visit(node);
			}
		} else {
			if (parentIsAnonymousClass(parentNode)) {
				return super.visit(node);
			}
		}

		// 'this' keyword is not inside an anonymous class
		replaceThisExpression(node);

		return super.visit(node);
	}

	/**
	 * Visits a ClassInstanceCreation node and replaces it if it creates a non-static member
	 * instance. This method is used during the instance usage rewriting process to handle
	 * ClassInstanceCreation nodes, which represent the creation of new class instances using
	 * constructors. If the created instance is a non-static member (i.e., an inner class instance),
	 * the method sets the 'fTargetMethodhasInstanceUsage' flag to true and replaces the
	 * ClassInstanceCreation node with a new expression to access instance variables or methods.
	 *
	 * @param node The ClassInstanceCreation node being visited. It represents a class instance
	 *            creation in the abstract syntax tree (AST).
	 *
	 * @return {@code true} to continue visiting the children of this node, {@code false} otherwise.
	 */
	@Override
	public boolean visit(ClassInstanceCreation node) {
		ITypeBinding typeBinding= node.getType().resolveBinding();
		if (typeBinding != null && typeBinding.isMember() && !Modifier.isStatic(typeBinding.getModifiers())) {
			fTargetMethodhasInstanceUsage= true;
			replaceClassInstanceCreation(node);
		}
		return super.visit(node);
	}

	/**
	 * Visits a SuperMethodInvocation node and checks for any super method invocation as part of the
	 * final conditions check. This method is used during the instance usage rewriting process to
	 * handle SuperMethodInvocation nodes, which represent method invocations using the 'super'
	 * keyword. Any super method invocations found will be reported as a fatal error.
	 *
	 * @param node The SuperMethodInvocation node being visited.
	 * @return {@code true} to continue visiting the children of this node, {@code false} otherwise.
	 */
	@Override
	public boolean visit(SuperMethodInvocation node) {
		fFinalConditionsChecker.checkNodeIsNoSuperMethodInvocation();
		return super.visit(node);
	}

	private void modifyFieldUsage(SimpleName node, IBinding binding) {
		IVariableBinding variableBinding= (IVariableBinding) binding;

		//Check if we are inside a anonymous class
		ITypeBinding declaringClass= variableBinding.getDeclaringClass();
		if (isInsideAnonymousClass(declaringClass)) {
			return;
		}

		if (variableBinding.isField() && !Modifier.isStatic(variableBinding.getModifiers())) {
			ASTNode parent= node.getParent();

			//this ensures only the leftmost SimpleName or QualifiedName gets changed see "testConcatenatedFieldAccessAndQualifiedNames"
			if (isConcatenatedFieldAccessOrQualifiedName(node, parent)) {
				return;
			}

			replaceFieldAccess(node, parent);

			fTargetMethodhasInstanceUsage= true;
		}
	}

	private void modifyInstanceMethodUsage(SimpleName node, IBinding binding) {
		IMethodBinding methodBinding= (IMethodBinding) binding;
		ITypeBinding declaringClass= methodBinding.getDeclaringClass();
		//Check if we are inside a anonymous class
		if (isInsideAnonymousClass(declaringClass)) {
			return;
		}

		if (!Modifier.isStatic(methodBinding.getModifiers())) {
			fTargetMethodhasInstanceUsage= true;

			fFinalConditionsChecker.checkIsNotRecursive(node, fTargetMethodDeclaration);

			replaceMethodInvocation(node);
		}
	}

	private boolean isInsideAnonymousClass(ITypeBinding declaringClass) {
		if (declaringClass != null) {
			boolean isLocal= declaringClass.isLocal();
			boolean isAnonymous= declaringClass.isAnonymous();
			boolean isMember= declaringClass.isMember();
			boolean isNested= declaringClass.isNested();
			boolean isTopLevel= declaringClass.isTopLevel();
			if (!isTopLevel || isNested || isAnonymous || isLocal || isMember) {
				return true;
			}
		}
		return false;
	}

	private boolean isConcatenatedFieldAccessOrQualifiedName(SimpleName node, ASTNode parent) {
		if (parent instanceof FieldAccess) {
			FieldAccess fieldAccess= (FieldAccess) parent;
			if (fieldAccess.getExpression() != node) {
				return true;
			}
		} else if (parent instanceof QualifiedName) {
			QualifiedName qualifiedName= (QualifiedName) parent;
			if (qualifiedName.getQualifier() != node) {
				return true;
			}
		}
		return false;
	}

	private void replaceFieldAccess(SimpleName node, ASTNode parent) {
		FieldAccess replacement= fAst.newFieldAccess();
		replacement.setExpression(fAst.newSimpleName(fParamName));
		replacement.setName(fAst.newSimpleName(node.getIdentifier()));

		fFinalConditionsChecker.checkMethodNotUsingSuperFieldAccess(parent);

		if (parent instanceof SuperFieldAccess) {
			//Warning is needed to inform user of possible changes in semantics when SuperFieldAccess is found
			fRewrite.replace(parent, replacement, null);
		} else {
			fRewrite.replace(node, replacement, null);
		}
	}

	private void replaceMethodInvocation(SimpleName node) {
		ASTNode parent= node.getParent();
		SimpleName replacementExpression= fAst.newSimpleName(fParamName);
		if (parent instanceof MethodInvocation) {
			MethodInvocation methodInvocation= (MethodInvocation) parent;
			Expression optionalExpression= methodInvocation.getExpression();

			if (optionalExpression == null) {
				fRewrite.set(methodInvocation, MethodInvocation.EXPRESSION_PROPERTY, replacementExpression, null);
			}
		}
	}

	private boolean parentIsAnonymousClass(ASTNode parentNode) {
		while (parentNode != null) {
			if (parentNode instanceof AnonymousClassDeclaration) {
				// 'this' keyword is inside an anonymous class, skip
				return true;
			}
			parentNode= parentNode.getParent();
		}
		return false;
	}

	private void replaceThisExpression(ThisExpression node) {
		fTargetMethodhasInstanceUsage= true;
		SimpleName replacement= fAst.newSimpleName(fParamName);
		fRewrite.replace(node, replacement, null);
	}

	private void replaceClassInstanceCreation(ClassInstanceCreation node) {
		ClassInstanceCreation replacement= fAst.newClassInstanceCreation();
		replacement.setType((Type) ASTNode.copySubtree(fAst, node.getType()));
		replacement.setExpression(fAst.newSimpleName(fParamName));
		for (Object arg : node.arguments()) {
			replacement.arguments().add(ASTNode.copySubtree(fAst, (Expression) arg));
		}
		fRewrite.replace(node, replacement, null);
	}
}
