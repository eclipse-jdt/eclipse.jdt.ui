/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

import org.eclipse.jdt.internal.corext.Assert;

public class Invocations {

	public static List getArguments(ASTNode invocation) {
		switch(invocation.getNodeType()) {
			case ASTNode.METHOD_INVOCATION:
				return ((MethodInvocation)invocation).arguments();
			case ASTNode.SUPER_METHOD_INVOCATION:
				return ((SuperMethodInvocation)invocation).arguments();
			default:
				Assert.isTrue(false, "Should not happen."); //$NON-NLS-1$
				return null;
		}
	}

	public static Expression getExpression(ASTNode invocation) {
		switch(invocation.getNodeType()) {
			case ASTNode.METHOD_INVOCATION:
				return ((MethodInvocation)invocation).getExpression();
			case ASTNode.SUPER_METHOD_INVOCATION:
				return null;
			default:
				Assert.isTrue(false, "Should not happen."); //$NON-NLS-1$
				return null;
		}
	}
	
	public static boolean isInvocation(ASTNode node) {
		int type= node.getNodeType();
		return type == ASTNode.METHOD_INVOCATION || type == ASTNode.SUPER_METHOD_INVOCATION;
	}
	
	public static SimpleName getName(ASTNode invocation) {
		switch(invocation.getNodeType()) {
			case ASTNode.METHOD_INVOCATION:
				return ((MethodInvocation)invocation).getName();
			case ASTNode.SUPER_METHOD_INVOCATION:
				return ((SuperMethodInvocation)invocation).getName();
			default:
				Assert.isTrue(false, "Should not happen."); //$NON-NLS-1$
				return null;
		}
	}
}
