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

public final class StandardType extends HierarchyType {

	protected StandardType(TypeEnvironment environment) {
		super(environment);
	}
	
	public int getElementType() {
		return STANDARD_TYPE;
	}
	
	public boolean isJavaLangObject() {
		return "java.lang.Object".equals(getJavaElementType().getFullyQualifiedName()); //$NON-NLS-1$
	}
	
	public boolean isJavaLangCloneable() {
		return "java.lang.Cloneable".equals(getJavaElementType().getFullyQualifiedName()); //$NON-NLS-1$
	}
	
	public boolean isJavaIoSerializable() {
		return "java.io.Serializable".equals(getJavaElementType().getFullyQualifiedName()); //$NON-NLS-1$
	}
	
	public boolean doEquals(Type type) {
		return getJavaElementType().equals(((GenericType)type).getJavaElementType());
	}
	
	public int hashCode() {
		return getJavaElementType().hashCode();
	}
	
	protected boolean doCanAssignTo(Type target) {
		int targetType= target.getElementType();
		switch (targetType) {
			case NULL_TYPE: return false;
			case PRIMITIVE_TYPE: return canAssignToPrimitive((PrimitiveType)target);
			
			case ARRAY_TYPE: return false;
			
			case STANDARD_TYPE: return canAssignToStandardType((StandardType)target); 
			case GENERIC_TYPE: return false;
			case PARAMETERIZED_TYPE: return isSubType((HierarchyType)target);
			case RAW_TYPE: return isSubType((HierarchyType)target);
			
			case UNBOUND_WILDCARD_TYPE:
			case SUPER_WILDCARD_TYPE: 
			case EXTENDS_WILDCARD_TYPE: 
				return ((WildcardType)target).checkBound(this);
			
			case TYPE_VARIABLE: return false;
		}
		return false;
	}

	private boolean canAssignToPrimitive(PrimitiveType type) {
		PrimitiveType source= getEnvironment().createUnBoxed(this);
		return source != null && source.canAssignTo(type);
	}

	public String getPrettySignature() {
		return getJavaElementType().getFullyQualifiedName('.');
	}
}
