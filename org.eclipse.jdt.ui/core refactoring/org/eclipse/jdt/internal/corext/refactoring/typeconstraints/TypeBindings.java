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
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.internal.corext.dom.Bindings;

/**
 * TODO: Should find out if this is the same functionality as Bindings.canAssign or Bindings.canCast
 */
public final class TypeBindings {
	
	private TypeBindings() {}
	
	private static boolean isNullBinding(ITypeBinding binding){
		return binding == null;
	}
	
	public static boolean isEqualTo(ITypeBinding binding, ITypeBinding otherBinding){
		if (isNullBinding(binding)) //null binding is not comparable to anybody
			return false;
		else
			return Bindings.equals(binding, otherBinding);
	}
	
	public static boolean isSubtypeBindingOf(ITypeBinding binding, ITypeBinding otherBinding){
		if (isNullBinding(binding) || isNullBinding(otherBinding)) //null binding is not comparable to anybodyO
			return false;
		return isSubtypeOf(binding, otherBinding);
	}

	private static boolean isSubtypeOf(ITypeBinding b1, ITypeBinding b2){
		if (b1.isNullType()) //null type is bottom
			return true;
		else if (b1.isPrimitive() || b2.isPrimitive()) //cannot compare these
			return false;
		else if (isThisType(b2, "java.lang.Object")) //$NON-NLS-1$
			return true;
		else if (Modifier.isFinal(b2.getModifiers()))
			return false;
		else if (b1.isArray()){
			if (b2.isArray())
				return isSubtypeOf(b1.getElementType(), b2.getElementType());
			else{
				return 	   isThisType(b2, "java.lang.Cloneable") //$NON-NLS-1$
						|| isThisType(b2, "java.io.Serializable"); //$NON-NLS-1$
			}
		} else if (b2.isArray())                       
			return false;
		else 
			return (getSuperTypes(b1).contains(b2));//TODO could optimize here - we just a yes or no
	}

	private static boolean isThisType(ITypeBinding binding, String qualifiedName){
		return binding.getQualifiedName().equals(qualifiedName);
	}

	/**
	 * returns a Set of ITypeBindings
	 */
	public static Set getSuperTypes(ITypeBinding type) {
		Set result= new HashSet();
		ITypeBinding superClass= type.getSuperclass();
		if (superClass != null){
			result.add(superClass);
			result.addAll(getSuperTypes(superClass));
		}
		ITypeBinding[] superInterfaces= type.getInterfaces();
		result.addAll(Arrays.asList(superInterfaces));
		for (int i= 0; i < superInterfaces.length; i++) {
			result.addAll(getSuperTypes(superInterfaces[i]));
		}
		return result;
	}
	
	public static String toString(ITypeBinding binding){
		if (isNullBinding(binding))
			return "<NULL BINDING>"; //$NON-NLS-1$
		return Bindings.asString(binding);
	}

	public static boolean isClassBinding(ITypeBinding typeBinding){
		return typeBinding != null && typeBinding.isClass();
	}
}
