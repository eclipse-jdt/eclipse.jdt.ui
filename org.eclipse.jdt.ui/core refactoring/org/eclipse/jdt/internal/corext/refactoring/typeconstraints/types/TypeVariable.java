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


public final class TypeVariable extends TType {
	
	private TType[] fBounds;
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
		fBounds= new TType[bounds.length];
		for (int i= 0; i < bounds.length; i++) {
			fBounds[i]= getEnvironment().create(bounds[i]);
		}
	}
	
	public int getElementType() {
		return TYPE_VARIABLE;
	}
	
	public TType getErasure() {
		return fBounds[0].getErasure();
	}
	
	/* package */ TType getLeftMostBound() {
		return fBounds[0];
	}
	
	public boolean doEquals(TType type) {
		return fJavaTypeParameter.equals(((TypeVariable)type).fJavaTypeParameter);
	}
	
	public int hashCode() {
		return fJavaTypeParameter.hashCode();
	}
	
	protected boolean doCanAssignTo(TType lhs) {
		switch (lhs.getElementType()) {
			case NULL_TYPE: 
			case VOID_TYPE: return false;
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
				return ((WildcardType)lhs).checkAssignmentBound(this);
				
			case TYPE_VARIABLE: 
				return doExtends((TypeVariable)lhs);
		}
		return false;
	}
	
	protected boolean checkAssignmentBound(TType rhs) {
		for (int i= 0; i < fBounds.length; i++) {
			if (rhs.canAssignTo(fBounds[i]))
				return true;
		}
		return false;
	}
	
	private boolean canAssignOneBoundTo(TType lhs) {
		for (int i= 0; i < fBounds.length; i++) {
			if (fBounds[i].canAssignTo(lhs))
				return true;
		}
		return false;
	}
	
	private boolean doExtends(TypeVariable other) {
		for (int i= 0; i < fBounds.length; i++) {
			TType bound= fBounds[i];
			if (other.equals(bound) || (bound.getElementType() == TYPE_VARIABLE && ((TypeVariable)bound).doExtends(other)))
				return true;
		}
		return false;
	}
	
	public String getName() {
		return fJavaTypeParameter.getElementName();
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
