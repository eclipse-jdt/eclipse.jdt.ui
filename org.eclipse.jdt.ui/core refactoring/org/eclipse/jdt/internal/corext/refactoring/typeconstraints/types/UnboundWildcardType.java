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

public final class UnboundWildcardType extends WildcardType {

	protected UnboundWildcardType(TypeEnvironment environment) {
		super(environment);
	}

	public int getElementType() {
		return UNBOUND_WILDCARD_TYPE;
	}
	
	public TType getErasure() {
		return getEnvironment().getJavaLangObject();
	}
	
	protected boolean doCanAssignTo(TType lhs) {
		switch(lhs.getElementType()) {
			case STANDARD_TYPE:
				return ((StandardType)lhs).isJavaLangObject();
			case UNBOUND_WILDCARD_TYPE:
				return true;
			case SUPER_WILDCARD_TYPE:
			case EXTENDS_WILDCARD_TYPE:
				return ((WildcardType)lhs).getBound().isJavaLangObject();
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
			case UNBOUND_WILDCARD_TYPE:
			case EXTENDS_WILDCARD_TYPE: 
			case SUPER_WILDCARD_TYPE:
			case TYPE_VARIABLE:
				return true;
			default:
				return false;
		}
	}
	
	protected boolean checkAssignmentBound(TType rhs) {
		// unbound equals ? extends Object.
		return rhs.isNullType();
	}
	
	public String getName() {
		return "?"; //$NON-NLS-1$
	}
	
	public String getPrettySignature() {
		return getName();
	}
}
