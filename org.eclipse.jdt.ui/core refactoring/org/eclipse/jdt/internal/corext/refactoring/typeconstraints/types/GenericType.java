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
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.internal.corext.Assert;

public final class GenericType extends HierarchyType {
	
	private Type[] fTypeParameters;
	
	protected GenericType(TypeEnvironment environment) {
		super(environment);
	}

	protected void initialize(ITypeBinding binding, IType javaElementType) {
		Assert.isTrue(binding.isGenericType());
		super.initialize(binding, javaElementType);
		TypeEnvironment environment= getEnvironment();
		ITypeBinding[] typeParameters= binding.getTypeParameters();
		fTypeParameters= new Type[typeParameters.length];
		for (int i= 0; i < typeParameters.length; i++) {
			fTypeParameters[i]= environment.create(typeParameters[i]);
		}
	}
	
	public int getElementType() {
		return GENERIC_TYPE;
	}
	
	public boolean doEquals(Type type) {
		return getJavaElementType().equals(((GenericType)type).getJavaElementType());
	}
	
	public int hashCode() {
		return getJavaElementType().hashCode();
	}
	
	protected boolean doCanAssignTo(Type type) {
		return false;
	}
	
	protected boolean isTypeEquivalentTo(Type other) {
		int otherElementType= other.getElementType();
		if (otherElementType == RAW_TYPE || otherElementType == PARAMETERIZED_TYPE)
			return getErasure().isTypeEquivalentTo(other.getErasure());
		return super.isTypeEquivalentTo(other);
	}
	
	public String getPrettySignature() {
		StringBuffer result= new StringBuffer(getJavaElementType().getFullyQualifiedName('.'));
		result.append("<"); //$NON-NLS-1$
		result.append(fTypeParameters[0].getPrettySignature());
		for (int i= 1; i < fTypeParameters.length; i++) {
			result.append(", "); //$NON-NLS-1$
			result.append(fTypeParameters[i].getPrettySignature());
		}
		result.append(">"); //$NON-NLS-1$
		return result.toString();
	}
}
