/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Dmitry Stalnov (dstalnov@fusionone.com) - contributed fix for
 *       bug "inline method - doesn't handle implicit cast" (see
 *       https://bugs.eclipse.org/bugs/show_bug.cgi?id=24941).
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.PrimitiveType.Code;

/**
 * Helper class to check if objects are assignable to each other.
 */
public class TypeRules {

	/**
	 * Tests if a two primitive types are assign compatible
	 * @param toAssignCode The binding of the type to assign
	 * @param definedTypeCode The type of the object that is assigned
	 * @return boolean Returns true if definedType = typeToAssign is true
	 */
	public static boolean canAssignPrimitive(PrimitiveType.Code toAssignCode, PrimitiveType.Code definedTypeCode) {
		//	definedTypeCode = typeCodeToAssign;
		if (toAssignCode == definedTypeCode) {
			return true;
		}
		if (definedTypeCode == PrimitiveType.BOOLEAN || toAssignCode == PrimitiveType.BOOLEAN) {
			return false;
		}
		if (definedTypeCode == PrimitiveType.CHAR && toAssignCode == PrimitiveType.BYTE) {
			return false;
		}
		return getTypeOrder(definedTypeCode) > getTypeOrder(toAssignCode);
	}
		
	/**
	 * Tests if two types are assign compatible. Void types are never compatible.
	 * @param typeToAssign The binding of the type to assign
	 * @param definedType The type of the object that is assigned
	 * @return boolean Returns true if definedType = typeToAssign is true
	 */
	public static boolean canAssign(ITypeBinding typeToAssign, ITypeBinding definedType) {
		return typeToAssign.isAssignmentCompatible(definedType);
	}

	private static int getTypeOrder(Code type) {
		if (type == PrimitiveType.BYTE)
			return 2;
		if (type == PrimitiveType.CHAR)
			return 3;
		if (type == PrimitiveType.SHORT)
			return 3;
		if (type == PrimitiveType.INT)
			return 4;
		if (type == PrimitiveType.LONG)
			return 5;
		if (type == PrimitiveType.FLOAT)
			return 6;
		if (type == PrimitiveType.DOUBLE)
			return 7;
		return 0;
	}
	
	public static boolean isArrayCompatible(ITypeBinding definedType) {
		if (definedType.isTopLevel()) {
			if (definedType.isClass()) {
				return "Object".equals(definedType.getName()) && "java.lang".equals(definedType.getPackage().getName());  //$NON-NLS-1$//$NON-NLS-2$
			} else {
				String qualifiedName= definedType.getQualifiedName();
				return "java.io.Serializable".equals(qualifiedName) || "java.lang.Cloneable".equals(qualifiedName); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} 
		return false;
	}
	
	public static boolean isJavaLangObject(ITypeBinding definedType) {
		return definedType.isTopLevel() && definedType.isClass() && "Object".equals(definedType.getName()) && "java.lang".equals(definedType.getPackage().getName());  //$NON-NLS-1$//$NON-NLS-2$	
	}

	/**
	 * Tests if a two types are cast compatible
	 * @param castType The binding of the type to cast to
	 * @param bindingToCast The binding ef the expression to cast.
	 * @return boolean Returns true if (castType) bindingToCast is a valid cast expression (can be unnecessary, but not invalid).
	 */
	public static boolean canCast(ITypeBinding castType, ITypeBinding bindingToCast) {
		return bindingToCast.isCastCompatible(castType);
	}
	
}
