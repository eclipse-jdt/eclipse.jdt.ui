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


public final class ParameterizedType extends HierarchyType {

	private GenericType fTypeDeclaration;
	private TType[] fTypeArguments;
	
	protected ParameterizedType(TypeEnvironment environment) {
		super(environment);
	}

	protected void initialize(ITypeBinding binding, IType javaElementType) {
		Assert.isTrue(binding.isParameterizedType());
		super.initialize(binding, javaElementType);
		TypeEnvironment environment= getEnvironment();
		fTypeDeclaration= (GenericType)environment.create(binding.getGenericType());
		ITypeBinding[] typeArguments= binding.getTypeArguments();
		fTypeArguments= new TType[typeArguments.length];
		for (int i= 0; i < typeArguments.length; i++) {
			fTypeArguments[i]= environment.create(typeArguments[i]);
		}
	}
	
	public int getElementType() {
		return PARAMETERIZED_TYPE;
	}

	public TType getTypeDeclaration() {
		return fTypeDeclaration;
	}
	
	public TType getErasure() {
		return fTypeDeclaration;
	}
	
	public boolean doEquals(TType type) {
		ParameterizedType other= (ParameterizedType)type;
		if (!fTypeDeclaration.equals(other.fTypeDeclaration))
			return false;
		if (fTypeArguments.length != other.fTypeArguments.length)
			return false;
		for (int i= 0; i < fTypeArguments.length; i++) {
			if (!fTypeArguments[i].equals(other.fTypeArguments[i]))
				return false;
		}
		return true;
	}
	
	public int hashCode() {
		int result= fTypeDeclaration.hashCode();
		for (int i= 0; i < fTypeArguments.length; i++) {
			result+= fTypeArguments[i].hashCode();
		}
		return result;
	}
	
	protected boolean doCanAssignTo(TType target) {
		int targetType= target.getElementType();
		switch (targetType) {
			case NULL_TYPE: return false;  
			case VOID_TYPE: return false;
			case PRIMITIVE_TYPE: return false;
			
			case ARRAY_TYPE: return false;
			
			case STANDARD_TYPE: return canAssignToStandardType((StandardType)target); 
			case GENERIC_TYPE: return false;
			case PARAMETERIZED_TYPE: return canAssignToParameterizedType((ParameterizedType)target);
			case RAW_TYPE: return canAssignToRawType((RawType)target);
			
			case UNBOUND_WILDCARD_TYPE:
			case SUPER_WILDCARD_TYPE:
			case EXTENDS_WILDCARD_TYPE: 
				return ((WildcardType)target).checkAssignmentBound(this);
				
			case TYPE_VARIABLE: return false;
		}
		return false;
	}
	
	protected boolean isTypeEquivalentTo(TType other) {
		int otherElementType= other.getElementType();
		if (otherElementType == RAW_TYPE || otherElementType == GENERIC_TYPE)
			return getErasure().isTypeEquivalentTo(other.getErasure());
		return super.isTypeEquivalentTo(other);
	}

	private boolean canAssignToRawType(RawType target) {
		return fTypeDeclaration.isSubType(target.getGenericType());
	}
	
	private boolean canAssignToParameterizedType(ParameterizedType target) {
		GenericType targetDeclaration= target.fTypeDeclaration;
		ParameterizedType sameSourceType= findSameDeclaration(targetDeclaration);
		if (sameSourceType == null)
			return false;
		TType[] targetArguments= target.fTypeArguments;
		TType[] sourceArguments= sameSourceType.fTypeArguments;
		if (targetArguments.length != sourceArguments.length)
			return false;
		for (int i= 0; i < sourceArguments.length; i++) {
			if (!targetArguments[i].checkTypeArgument(sourceArguments[i]))
				return false;
		}
		return true;
	}
	
	private ParameterizedType findSameDeclaration(GenericType targetDeclaration) {
		if (fTypeDeclaration.equals(targetDeclaration))
			return this;
		ParameterizedType result= null;
		TType type= getSuperclass();
		if (type != null && type.getElementType() == PARAMETERIZED_TYPE) {
			result= ((ParameterizedType)type).findSameDeclaration(targetDeclaration);
			if (result != null)
				return result;
		}
		TType[] interfaces= getInterfaces();
		for (int i= 0; i < interfaces.length; i++) {
			type= interfaces[i];
			if (type != null && type.getElementType() == PARAMETERIZED_TYPE) {
				result= ((ParameterizedType)type).findSameDeclaration(targetDeclaration);
				if (result != null)
					return result;
			}
		}
		return null;
	}
	
	public String getName() {
		StringBuffer result= new StringBuffer(getJavaElementType().getElementName());
		result.append("<"); //$NON-NLS-1$
		result.append(fTypeArguments[0].getName());
		for (int i= 1; i < fTypeArguments.length; i++) {
			result.append(", "); //$NON-NLS-1$
			result.append(fTypeArguments[i].getName());
		}
		result.append(">"); //$NON-NLS-1$
		return result.toString();
	}
	
	public String getPrettySignature() {
		StringBuffer result= new StringBuffer(getJavaElementType().getFullyQualifiedName('.'));
		result.append("<"); //$NON-NLS-1$
		result.append(fTypeArguments[0].getPrettySignature());
		for (int i= 1; i < fTypeArguments.length; i++) {
			result.append(", "); //$NON-NLS-1$
			result.append(fTypeArguments[i].getPrettySignature());
		}
		result.append(">"); //$NON-NLS-1$
		return result.toString();
	}
}
