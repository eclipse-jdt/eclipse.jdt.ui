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

import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.internal.corext.Assert;

public final class ArrayType extends TType {
	private TType fItemType;
	private int fDimensions;
	
	private TType fErasure;

	protected ArrayType(TypeEnvironment environment) {
		super(environment);
	}

	protected void initialize(ITypeBinding binding, TType itemType) {
		Assert.isTrue(binding.isArray());
		super.initialize(binding);
		fItemType= itemType;
		fDimensions= binding.getDimensions();
		if (fItemType.isParameterizedType() || fItemType.isRawType()) {
			fErasure= getEnvironment().create(binding.getErasure());
		} else {
			fErasure= this;
		}
	}

	public int getElementType() {
		return ARRAY_TYPE;
	}
	
	public TType getErasure() {
		return fErasure;
	}
	
	public boolean doEquals(TType other) {
		ArrayType arrayType= (ArrayType)other;
		return fItemType.equals(arrayType.fItemType) && fDimensions == arrayType.fDimensions;
	}
	
	public int hashCode() {
		return fItemType.hashCode() << ARRAY_TYPE_SHIFT;
	}
	
	protected boolean doCanAssignTo(TType lhs) {
		switch (lhs.getElementType()) {
			case NULL_TYPE: return false;
			case VOID_TYPE: return false;
			case PRIMITIVE_TYPE: return false;
			
			case ARRAY_TYPE: return canAssignToArrayType((ArrayType)lhs);
			
			case GENERIC_TYPE: return false;
			case STANDARD_TYPE: return isArrayLhsCompatible(lhs);
			case PARAMETERIZED_TYPE: return false;
			case RAW_TYPE: return false;
			
			case UNBOUND_WILDCARD_TYPE:
			case EXTENDS_WILDCARD_TYPE: 
			case SUPER_WILDCARD_TYPE: 
				return ((WildcardType)lhs).checkAssignmentBound(this);
				
			case TYPE_VARIABLE: return false;
		}
		return false;
	}

	private boolean canAssignToArrayType(ArrayType lhs) {
		if (fDimensions == lhs.fDimensions) {
			// primitive type don't have any conversion for arrays.
			if (fItemType.getElementType() == PRIMITIVE_TYPE || lhs.fItemType.getElementType() == PRIMITIVE_TYPE)
				return fItemType.isTypeEquivalentTo(lhs.fItemType);
			return fItemType.canAssignTo(lhs.fItemType);
		}
		if (fDimensions < lhs.fDimensions)
			return false;
		return isArrayLhsCompatible(lhs.fItemType);
	}

	private boolean isArrayLhsCompatible(TType lhsElementType) {
		return lhsElementType.isJavaLangObject() || lhsElementType.isJavaLangCloneable() || lhsElementType.isJavaIoSerializable();
	}
	
	public String getPrettySignature() {
		StringBuffer result= new StringBuffer(fItemType.getPrettySignature());
		for (int i= 0; i < fDimensions; i++) {
			result.append("[]"); //$NON-NLS-1$
		}
		return result.toString();
	}
	
	public String getName() {
		StringBuffer result= new StringBuffer(fItemType.getName());
		for (int i= 0; i < fDimensions; i++) {
			result.append("[]"); //$NON-NLS-1$
		}
		return result.toString();
	}
}
