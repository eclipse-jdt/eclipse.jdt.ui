/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;

import org.eclipse.jdt.internal.corext.Assert;
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
	
	private static MethodDeclaration getMethod(ReturnStatement returnStatement) {
		return (MethodDeclaration)ASTNodes.getParent(returnStatement, MethodDeclaration.class);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "[" + Bindings.asString(fMethodBinding) + "]_returnType";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (! super.equals(obj))
			return false;
		if (! (obj instanceof ReturnTypeVariable))
			return false;
		ReturnTypeVariable other= (ReturnTypeVariable)obj;
		return Bindings.equals(fMethodBinding, other.fMethodBinding);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return super.hashCode() ^ fMethodBinding.hashCode();
	}
}
