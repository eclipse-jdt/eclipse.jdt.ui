/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

public class Invocations {

	public static List getArguments(ASTNode invocation) {
		switch(invocation.getNodeType()) {
			case ASTNode.METHOD_INVOCATION:
				return ((MethodInvocation)invocation).arguments();
			case ASTNode.SUPER_METHOD_INVOCATION:
				return ((SuperMethodInvocation)invocation).arguments();
			case ASTNode.CONSTRUCTOR_INVOCATION:
				return ((ConstructorInvocation)invocation).arguments();
			default:
				throw new IllegalArgumentException(invocation.toString());
		}
	}

	public static Expression getExpression(ASTNode invocation) {
		switch(invocation.getNodeType()) {
			case ASTNode.METHOD_INVOCATION:
				return ((MethodInvocation)invocation).getExpression();
			case ASTNode.SUPER_METHOD_INVOCATION:
			case ASTNode.CONSTRUCTOR_INVOCATION:
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

	public static IMethodBinding resolveBinding(ASTNode invocation) {
		switch(invocation.getNodeType()) {
			case ASTNode.METHOD_INVOCATION:
				return ((MethodInvocation)invocation).resolveMethodBinding();
			case ASTNode.SUPER_METHOD_INVOCATION:
				return ((SuperMethodInvocation)invocation).resolveMethodBinding();
			case ASTNode.CONSTRUCTOR_INVOCATION:
				return ((ConstructorInvocation)invocation).resolveConstructorBinding();
			default:
				throw new IllegalArgumentException(invocation.toString());
		}
	}

	public static boolean isResolvedTypeInferredFromExpectedType(Expression invocation) {
		switch(invocation.getNodeType()) {
			case ASTNode.METHOD_INVOCATION:
				return ((MethodInvocation) invocation).isResolvedTypeInferredFromExpectedType();
			case ASTNode.SUPER_METHOD_INVOCATION:
				return ((SuperMethodInvocation) invocation).isResolvedTypeInferredFromExpectedType();
			case ASTNode.CONSTRUCTOR_INVOCATION:
				return false;
			default:
				throw new IllegalArgumentException(invocation.toString());
		}
	}

	public static ChildListPropertyDescriptor getTypeArgumentsProperty(Expression invocation) {
		switch(invocation.getNodeType()) {
			case ASTNode.METHOD_INVOCATION:
				return MethodInvocation.TYPE_ARGUMENTS_PROPERTY;
			case ASTNode.SUPER_METHOD_INVOCATION:
				return SuperMethodInvocation.TYPE_ARGUMENTS_PROPERTY;
			case ASTNode.CONSTRUCTOR_INVOCATION:
				return ConstructorInvocation.TYPE_ARGUMENTS_PROPERTY;
			default:
				throw new IllegalArgumentException(invocation.toString());
		}
	}
}
