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

public final class ExtendsWildcardType extends WildcardType {

	protected ExtendsWildcardType(TypeEnvironment environment) {
		super(environment);
	}

	public int getElementType() {
		return EXTENDS_WILDCARD_TYPE;
	}
	
	public TType getErasure() {
		return fBound.getErasure();
	}
	
	protected boolean doCanAssignTo(TType lhs) {
		switch (lhs.getElementType()) {
			case ARRAY_TYPE: 
			case STANDARD_TYPE:  
			case PARAMETERIZED_TYPE:
			case RAW_TYPE: 
				return getBound().canAssignTo(lhs);
			
			case UNBOUND_WILDCARD_TYPE: 
				return true;
			case SUPER_WILDCARD_TYPE: 
			case EXTENDS_WILDCARD_TYPE: 
				return ((WildcardType)lhs).checkAssignmentBound(getBound());
			
			case TYPE_VARIABLE: 
				return ((TypeVariable)lhs).checkAssignmentBound(getBound());
				
			default:
				return false;
		}
	}
	
	protected boolean checkTypeArgument(TType rhs) {
		switch(rhs.getElementType()) {
			case ARRAY_TYPE:
			case STANDARD_TYPE:
			case PARAMETERIZED_TYPE:
			case RAW_TYPE:
				return rhs.canAssignTo(getBound());
				
			case UNBOUND_WILDCARD_TYPE:
				return getBound().isJavaLangObject();
			case EXTENDS_WILDCARD_TYPE: 
				return ((ExtendsWildcardType)rhs).getBound().canAssignTo(getBound());
			case SUPER_WILDCARD_TYPE:
				return getBound().isJavaLangObject();
				
			case TYPE_VARIABLE:
				return rhs.canAssignTo(getBound());
				
			default:
				return false;
		}
	}
	
	protected boolean checkAssignmentBound(TType rhs) {
		// ? extends Number is a set of all subtyes of number and number.
		// so the only thing that can be assigned is null since null is
		// a sub type of everything
		return rhs.isNullType();
	}
	
	public String getName() {
		return internalGetName("extends"); //$NON-NLS-1$
	}
	
	public String getPrettySignature() {
		return internalGetPrettySignature("extends"); //$NON-NLS-1$
	}
}
