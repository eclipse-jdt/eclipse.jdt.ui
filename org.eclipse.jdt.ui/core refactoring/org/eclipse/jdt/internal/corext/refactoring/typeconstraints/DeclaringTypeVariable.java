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

import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import org.eclipse.jdt.internal.corext.dom.Bindings;

/**
 * Tells the type which declares the member.
 */
public class DeclaringTypeVariable extends ConstraintVariable{

	private final IBinding fMemberBinding;

	protected DeclaringTypeVariable(ITypeBinding memberTypeBinding) {
		super(memberTypeBinding.getDeclaringClass());
		fMemberBinding= memberTypeBinding;
	}

	protected DeclaringTypeVariable(IVariableBinding fieldBinding) {
		super(fieldBinding.getDeclaringClass());
		Assert.isTrue(fieldBinding.isField());
		fMemberBinding= fieldBinding;
	}

	protected DeclaringTypeVariable(IMethodBinding methodBinding) {
		super(methodBinding.getDeclaringClass());
		fMemberBinding= methodBinding;
	}

	@Override
	public String toString() {
		return "Decl(" + Bindings.asString(fMemberBinding) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public IBinding getMemberBinding() {
		return fMemberBinding;
	}
}
