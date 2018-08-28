/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;

public class ReturnTypeVariable extends ConstraintVariable{

	private final IMethodBinding fMethodBinding;

	public ReturnTypeVariable(ReturnStatement returnStatement) {
		this(getMethod(returnStatement).resolveBinding());
		Assert.isNotNull(returnStatement);
	}

	public ReturnTypeVariable(IMethodBinding methodBinding) {
		super(methodBinding.getReturnType());
		fMethodBinding= methodBinding;
	}

	public static MethodDeclaration getMethod(ReturnStatement returnStatement) {
		return ASTNodes.getParent(returnStatement, MethodDeclaration.class);
	}

	@Override
	public String toString() {
		return "[" + Bindings.asString(fMethodBinding) + "]_returnType"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public IMethodBinding getMethodBinding() {
		return fMethodBinding;
	}

}
