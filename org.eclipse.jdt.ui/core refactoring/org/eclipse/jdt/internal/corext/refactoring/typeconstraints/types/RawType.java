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

public final class RawType extends HierarchyType {
	
	private GenericType fGenericType;
	
	protected RawType(TypeEnvironment environment) {
		super(environment);
	}

	protected void initialize(ITypeBinding binding, IType javaElementType) {
		Assert.isTrue(binding.isRawType());
		super.initialize(binding, javaElementType);
		TypeEnvironment environment= getEnvironment();
		fGenericType= (GenericType)environment.create(binding.getGenericType());
	}
	
	public int getElementType() {
		return RAW_TYPE;
	}
	
	public boolean doEquals(Type type) {
		return getJavaElementType().equals(((GenericType)type).getJavaElementType());
	}
	
	public int hashCode() {
		return getJavaElementType().hashCode();
	}
	
	protected GenericType getGenericType() {
		return fGenericType;
	}
	
	public Type getErasure() {
		return fGenericType;
	}
	
	protected boolean doCanAssignTo(Type target) {
		int targetType= target.getElementType();
		switch (targetType) {
			case NULL_TYPE: return false;
			case PRIMITIVE_TYPE: return false;
			
			case ARRAY_TYPE: return false;
			
			case STANDARD_TYPE: return canAssignToStandardType((StandardType)target); 
			case GENERIC_TYPE: return false;
			case PARAMETERIZED_TYPE: return isSubType((ParameterizedType)target);
			case RAW_TYPE: return isSubType((HierarchyType)target);
			
			case UNBOUND_WILDCARD_TYPE:
			case SUPER_WILDCARD_TYPE:
			case EXTENDS_WILDCARD_TYPE: 
				return ((WildcardType)target).checkBound(this);
			
			case TYPE_VARIABLE: return false;
		}
		return false;
	}

	protected boolean isTypeEquivalentTo(Type other) {
		int otherElementType= other.getElementType();
		if (otherElementType == PARAMETERIZED_TYPE || otherElementType == GENERIC_TYPE)
			return getErasure().isTypeEquivalentTo(other.getErasure());
		return super.isTypeEquivalentTo(other);
	}

	public String getPrettySignature() {
		return getJavaElementType().getFullyQualifiedName('.');
	}
}
