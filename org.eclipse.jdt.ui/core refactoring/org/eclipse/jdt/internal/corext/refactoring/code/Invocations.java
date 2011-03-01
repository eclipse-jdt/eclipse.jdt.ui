/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dmitry Stalnov (dstalnov@fusionone.com) - contributed fixes for:
 *       o Allow 'this' constructor to be inlined
 *         (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=38093)
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

public class Invocations {

	public static List<Expression> getArguments(ASTNode invocation) {
		switch (invocation.getNodeType()) {
			case ASTNode.METHOD_INVOCATION:
				return ((MethodInvocation)invocation).arguments();
			case ASTNode.SUPER_METHOD_INVOCATION:
				return ((SuperMethodInvocation)invocation).arguments();
				
			case ASTNode.CONSTRUCTOR_INVOCATION:
				return ((ConstructorInvocation)invocation).arguments();
			case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
				return ((SuperConstructorInvocation)invocation).arguments();
				
			case ASTNode.CLASS_INSTANCE_CREATION:
				return ((ClassInstanceCreation)invocation).arguments();
			case ASTNode.ENUM_CONSTANT_DECLARATION:
				return ((EnumConstantDeclaration)invocation).arguments();
				
			default:
				throw new IllegalArgumentException(invocation.toString());
		}
	}

	public static ChildListPropertyDescriptor getArgumentsProperty(ASTNode invocation) {
		switch (invocation.getNodeType()) {
			case ASTNode.METHOD_INVOCATION:
				return MethodInvocation.ARGUMENTS_PROPERTY;
			case ASTNode.SUPER_METHOD_INVOCATION:
				return SuperMethodInvocation.ARGUMENTS_PROPERTY;
				
			case ASTNode.CONSTRUCTOR_INVOCATION:
				return ConstructorInvocation.ARGUMENTS_PROPERTY;
			case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
				return SuperConstructorInvocation.ARGUMENTS_PROPERTY;
				
			case ASTNode.CLASS_INSTANCE_CREATION:
				return ClassInstanceCreation.ARGUMENTS_PROPERTY;
			case ASTNode.ENUM_CONSTANT_DECLARATION:
				return EnumConstantDeclaration.ARGUMENTS_PROPERTY;
				
			default:
				throw new IllegalArgumentException(invocation.toString());
		}
	}
	
	public static Expression getExpression(ASTNode invocation) {
		switch (invocation.getNodeType()) {
			case ASTNode.METHOD_INVOCATION:
				return ((MethodInvocation)invocation).getExpression();
			case ASTNode.SUPER_METHOD_INVOCATION:
				return null;
				
			case ASTNode.CONSTRUCTOR_INVOCATION:
				return null;
			case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
				return ((SuperConstructorInvocation)invocation).getExpression();
				
			case ASTNode.CLASS_INSTANCE_CREATION:
				return ((ClassInstanceCreation)invocation).getExpression();
			case ASTNode.ENUM_CONSTANT_DECLARATION:
				return null;
				
			default:
				throw new IllegalArgumentException(invocation.toString());
		}
	}

	public static boolean isInvocation(ASTNode node) {
		int type= node.getNodeType();
		return type == ASTNode.METHOD_INVOCATION || type == ASTNode.SUPER_METHOD_INVOCATION ||
			type == ASTNode.CONSTRUCTOR_INVOCATION;
	}
	
	public static boolean isInvocationWithArguments(ASTNode node) {
		switch (node.getNodeType()) {
			case ASTNode.METHOD_INVOCATION:
			case ASTNode.SUPER_METHOD_INVOCATION:
				
			case ASTNode.CONSTRUCTOR_INVOCATION:
			case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
				
			case ASTNode.CLASS_INSTANCE_CREATION:
			case ASTNode.ENUM_CONSTANT_DECLARATION:
				return true;
				
			default:
				return false;
		}
	}

	public static IMethodBinding resolveBinding(ASTNode invocation) {
		switch (invocation.getNodeType()) {
			case ASTNode.METHOD_INVOCATION:
				return ((MethodInvocation)invocation).resolveMethodBinding();
			case ASTNode.SUPER_METHOD_INVOCATION:
				return ((SuperMethodInvocation)invocation).resolveMethodBinding();
				
			case ASTNode.CONSTRUCTOR_INVOCATION:
				return ((ConstructorInvocation)invocation).resolveConstructorBinding();
			case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
				return ((SuperConstructorInvocation)invocation).resolveConstructorBinding();
				
			case ASTNode.CLASS_INSTANCE_CREATION:
				return ((ClassInstanceCreation)invocation).resolveConstructorBinding();
			case ASTNode.ENUM_CONSTANT_DECLARATION:
				return ((EnumConstantDeclaration)invocation).resolveConstructorBinding();
				
			default:
				throw new IllegalArgumentException(invocation.toString());
		}
	}

	public static boolean isResolvedTypeInferredFromExpectedType(Expression invocation) {
		switch (invocation.getNodeType()) {
			case ASTNode.METHOD_INVOCATION:
				return ((MethodInvocation) invocation).isResolvedTypeInferredFromExpectedType();
			case ASTNode.SUPER_METHOD_INVOCATION:
				return ((SuperMethodInvocation) invocation).isResolvedTypeInferredFromExpectedType();
				
			case ASTNode.CONSTRUCTOR_INVOCATION:
			case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
				
			case ASTNode.CLASS_INSTANCE_CREATION:
			case ASTNode.ENUM_CONSTANT_DECLARATION:
				return false;
			default:
				throw new IllegalArgumentException(invocation.toString());
		}
	}

	public static ChildListPropertyDescriptor getTypeArgumentsProperty(Expression invocation) {
		switch (invocation.getNodeType()) {
			case ASTNode.METHOD_INVOCATION:
				return MethodInvocation.TYPE_ARGUMENTS_PROPERTY;
			case ASTNode.SUPER_METHOD_INVOCATION:
				return SuperMethodInvocation.TYPE_ARGUMENTS_PROPERTY;
				
			case ASTNode.CONSTRUCTOR_INVOCATION:
				return ConstructorInvocation.TYPE_ARGUMENTS_PROPERTY;
			case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
				return SuperConstructorInvocation.TYPE_ARGUMENTS_PROPERTY;
				
			case ASTNode.CLASS_INSTANCE_CREATION:
				return ClassInstanceCreation.TYPE_ARGUMENTS_PROPERTY;
				
			case ASTNode.ENUM_CONSTANT_DECLARATION:
			default:
				throw new IllegalArgumentException(invocation.toString());
		}
	}
}
