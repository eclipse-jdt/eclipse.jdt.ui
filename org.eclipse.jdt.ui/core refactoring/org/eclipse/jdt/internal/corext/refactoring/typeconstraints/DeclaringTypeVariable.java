/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints;

import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.Bindings;

public class DeclaringTypeVariable extends ConstraintVariable{
	
	private final IBinding fBinding;
	
	protected DeclaringTypeVariable(ITypeBinding memberTypeBinding) {
		super(memberTypeBinding.getDeclaringClass());
		fBinding= memberTypeBinding;
	}

	protected DeclaringTypeVariable(IVariableBinding fieldBinding) {
		super(fieldBinding.getDeclaringClass());
		Assert.isTrue(fieldBinding.isField());
		fBinding= fieldBinding;
	}

	protected DeclaringTypeVariable(IMethodBinding methodBinding) {
		super(methodBinding.getDeclaringClass());
		fBinding= methodBinding;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "Decl(" + Bindings.asString(fBinding) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
