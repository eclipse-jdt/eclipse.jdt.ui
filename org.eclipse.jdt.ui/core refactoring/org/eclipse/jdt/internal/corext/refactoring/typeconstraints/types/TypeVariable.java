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

import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.internal.corext.Assert;


public final class TypeVariable extends Type {
	
	private Type[] fBounds;
	private ITypeParameter fJavaTypeParameter;
	
	protected TypeVariable(TypeEnvironment environment) {
		super(environment);
	}

	protected void initialize(ITypeBinding binding, ITypeParameter javaTypeParameter) {
		Assert.isTrue(binding.isTypeVariable());
		super.initialize(binding);
		Assert.isNotNull(javaTypeParameter);
		fJavaTypeParameter= javaTypeParameter;
		ITypeBinding[] bounds= binding.getTypeBounds();
		fBounds= new Type[bounds.length];
		for (int i= 0; i < bounds.length; i++) {
			fBounds[i]= getEnvironment().create(bounds[i]);
		}
	}
	
	public int getElementType() {
		return TYPE_VARIABLE;
	}
	
	public boolean doEquals(Type type) {
		return fJavaTypeParameter.equals(((TypeVariable)type).fJavaTypeParameter);
	}
	
	public int hashCode() {
		return fJavaTypeParameter.hashCode();
	}
	
	protected boolean doCanAssignTo(Type lhs) {
		switch (lhs.getElementType()) {
			case NULL_TYPE: 
			case PRIMITIVE_TYPE:
				
			case ARRAY_TYPE: return false;
			
			case GENERIC_TYPE: return false;
			case STANDARD_TYPE: 
			case PARAMETERIZED_TYPE:
			case RAW_TYPE:
				return canAssignOneBoundTo(lhs);

			case UNBOUND_WILDCARD_TYPE:
			case EXTENDS_WILDCARD_TYPE:
			case SUPER_WILDCARD_TYPE:
				return ((WildcardType)lhs).checkBound(this);
				
			case TYPE_VARIABLE: 
				return doExtends((TypeVariable)lhs);
		}
		return false;
	}
	
	private boolean canAssignOneBoundTo(Type lhs) {
		for (int i= 0; i < fBounds.length; i++) {
			if (fBounds[i].canAssignTo(lhs))
				return true;
		}
		return false;
	}
	
	private boolean doExtends(TypeVariable other) {
		for (int i= 0; i < fBounds.length; i++) {
			Type bound= fBounds[i];
			if (other.equals(bound) || (bound.getElementType() == TYPE_VARIABLE && ((TypeVariable)bound).doExtends(other)))
				return true;
		}
		return false;
	}
	
	public String getPrettySignature() {
		StringBuffer result= new StringBuffer(fJavaTypeParameter.getElementName());
		if (fBounds.length > 0) {
			result.append(" extends "); //$NON-NLS-1$
			result.append(fBounds[0].getPrettySignature());
			for (int i= 1; i < fBounds.length; i++) {
				result.append(", "); //$NON-NLS-1$
				result.append(fBounds[i].getPrettySignature());
			}
		}
		return result.toString();
	}
}
