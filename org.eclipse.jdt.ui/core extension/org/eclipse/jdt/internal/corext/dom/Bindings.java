/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import org.eclipse.jdt.internal.corext.Assert;

public class Bindings {
	
	private Bindings() {
		// No instance
	}
	
	/**
	 * Checks if the two bindings are equals. First an identity check is
	 * made an then the key of the bindings are compared. 
	 * @param b1 first binding treated as <code>this</code>. So it must
	 *  not be <code>null</code>
	 * @param b2 the second binding.
	 * @return boolean
	 */
	public static boolean equals(IBinding b1, IBinding b2) {
		Assert.isNotNull(b1);
		if (b1 == b2)
			return true;
		if (b2 == null)
			return false;		
		String k1= b1.getKey();
		String k2= b2.getKey();
		if (k1 == null || k2 == null)
				return false;
		if (k1 != null)
			return k1.equals(k2);
		else
			return k2.equals(k1);
	}
	
	public static int hashCode(IBinding binding){
		Assert.isNotNull(binding);
		String key= binding.getKey();
		if (key == null)
			return binding.hashCode();
		return key.hashCode();
	}
		
	public static String asString(IBinding binding){
		if (binding instanceof IMethodBinding)
			return asString((IMethodBinding)binding);
		else if (binding instanceof ITypeBinding)
			return asString((ITypeBinding)binding);
		else if (binding instanceof IVariableBinding)
			return asString((IVariableBinding)binding);
		return binding.toString();
	}

	private static String asString(IVariableBinding variableBinding) {
		if (! variableBinding.isField())
			return variableBinding.toString();
		StringBuffer result= new StringBuffer();
		result.append(variableBinding.getDeclaringClass().getName());
		result.append(':');
		result.append(variableBinding.getName());				
		return result.toString();		
	}

	private static String asString(ITypeBinding type) {
		return type.getQualifiedName();
	}
		
	private static String asString(IMethodBinding method) {
		StringBuffer result= new StringBuffer();
		result.append(method.getDeclaringClass().getName());
		result.append(':');
		result.append(method.getName());
		result.append('(');
		ITypeBinding[] parameters= method.getParameterTypes();
		int lastComma= parameters.length - 1;
		for (int i= 0; i < parameters.length; i++) {
			ITypeBinding parameter= parameters[i];
			result.append(parameter.getName());
			if (i < lastComma)
				result.append(", "); //$NON-NLS-1$
		}
		result.append(')');
		return result.toString();
	}
	
	public static String getTypeQualifiedName(ITypeBinding type) {
		StringBuffer buffer= new StringBuffer();
		createName(type, false, buffer);
		return buffer.toString();
	}
	
	public static String getFullyQualifiedName(ITypeBinding type) {
		StringBuffer buffer= new StringBuffer();
		createName(type, true, buffer);
		return buffer.toString();		
	}	

	private static void createName(ITypeBinding type, boolean includePackage, StringBuffer buffer) {
		ITypeBinding baseType= type;
		if (type.isArray()) {
			baseType= type.getElementType();
		}
		if (!baseType.isPrimitive() && !baseType.isNullType()) {
			ITypeBinding declaringType= baseType.getDeclaringClass();
			if (declaringType != null) {
				createName(declaringType, includePackage, buffer);
				buffer.append('.');
			} else if (includePackage && !baseType.getPackage().isUnnamed()) {
				buffer.append(baseType.getPackage().getName());
				buffer.append('.');
			}
		}
		if (!baseType.isAnonymous()) {
			buffer.append(type.getName());
		} else {
			buffer.append("$local$"); //$NON-NLS-1$
		}
	}
	
	private static void createName(ITypeBinding type, boolean includePackage, List list) {
		ITypeBinding baseType= type;
		if (type.isArray()) {
			baseType= type.getElementType();
		}
		if (!baseType.isPrimitive() && !baseType.isNullType()) {
			ITypeBinding declaringType= baseType.getDeclaringClass();
			if (declaringType != null) {
				createName(declaringType, includePackage, list);
			} else if (includePackage && !baseType.getPackage().isUnnamed()) {
				String[] components= baseType.getPackage().getNameComponents();
				for (int i= 0; i < components.length; i++) {
					list.add(components[i]);
				}
			}
		}
		if (!baseType.isAnonymous()) {
			list.add(type.getName());
		} else {
			list.add("$local$"); //$NON-NLS-1$
		}		
	}	
	
	
	public static String getFullyQualifiedImportName(ITypeBinding type) {
		if (type.isArray())
			return getFullyQualifiedName(type.getElementType());
		else if (type.isAnonymous())
			return getFullyQualifiedImportName(type.getSuperclass());
		else
			return getFullyQualifiedName(type);
	}
	
	public static String[] getNameComponents(ITypeBinding type) {
		List result= new ArrayList(5);
		createName(type, false, result);
		return (String[]) result.toArray(new String[result.size()]);
	}
	
	public static String[] getAllNameComponents(ITypeBinding type) {
		List result= new ArrayList(5);
		createName(type, true, result);
		return (String[]) result.toArray(new String[result.size()]);
	}
	
	public static ITypeBinding getTopLevelType(ITypeBinding type) {
		ITypeBinding parent= type.getDeclaringClass();
		while (parent != null) {
			type= parent;
			parent= type.getDeclaringClass();
		}
		return type;
	}
	
	/**
	 * Checks whether	the passed type binding is a runtime exception.
	 * 
	 * @return <code>true</code> if the passed type binding is a runtime exception;
	 * 	otherwise <code>false</code> is returned
	 */
	public static boolean isRuntimeException(ITypeBinding thrownException, AST ast) {
		if (thrownException == null || thrownException.isPrimitive() || thrownException.isArray())
			return false;
		
		ITypeBinding runTimeException= ast.resolveWellKnownType("java.lang.RuntimeException"); //$NON-NLS-1$
		while (thrownException != null) {
			if (runTimeException == thrownException)
				return true;
			thrownException= thrownException.getSuperclass();
		}
		return false;
	}
	
	/**
	 * Finds the field specified by <code>fieldName<code> in
	 * the given <code>type</code>. Returns <code>null</code> if no such field exits.
	 */
	public static IVariableBinding findFieldInType(ITypeBinding type, String fieldName) {
		if (type.isPrimitive())
			return null;
		IVariableBinding[] fields= type.getDeclaredFields();
		for (int i= 0; i < fields.length; i++) {
			IVariableBinding field= fields[i];
			if (field.getName().equals(fieldName))
				return field;
		}
		return null;
	}
		
	/**
	 * Finds the method specified by <code>methodName<code> and </code>parameters</code> in
	 * the given <code>type</code>. Returns <code>null</code> if no such method exits.
	 */
	public static IMethodBinding findMethodInType(ITypeBinding type, String methodName, ITypeBinding[] parameters) {
		if (type.isPrimitive())
			return null;
		IMethodBinding[] methods= type.getDeclaredMethods();
		for (int i= 0; i < methods.length; i++) {
			if (isEqualMethod(methods[i], methodName, parameters))
				return methods[i];
		}
		return null;
	}
	
	/**
	 * Finds the method specified by <code>methodName<code> and </code>parameters</code> in
	 * the type hierarchy denoted by the given type. Returns <code>null</code> if no such method
	 * exists.
	 */
	public static IMethodBinding findMethodInHierarchy(ITypeBinding type, String methodName, ITypeBinding parameters[]) {
		while (type != null) {
			IMethodBinding method= findMethodInType(type, methodName, parameters);
			if (method != null)
				return method;
			type= type.getSuperclass();
		}
		return null;
	}
	
	public static boolean isEqualMethod(IMethodBinding method, String methodName, ITypeBinding[] parameters) {
		if (!method.getName().equals(methodName))
			return false;
			
		ITypeBinding[] methodParameters= method.getParameterTypes();
		if (methodParameters.length != parameters.length)
			return false;
		for (int i= 0; i < parameters.length; i++) {
			if (parameters[i] != methodParameters[i])
				return false;
		}
		return true;
	}

	public static ITypeBinding findTypeInHierarchy(ITypeBinding hierarchyType, String fullyQualifiedTypeName) {
		if (hierarchyType.isArray() || hierarchyType.isPrimitive()) {
			return null;
		}
		if (fullyQualifiedTypeName.equals(hierarchyType.getQualifiedName())) {
			return hierarchyType;
		}
		ITypeBinding superClass= hierarchyType.getSuperclass();
		if (superClass != null) {
			ITypeBinding res= findTypeInHierarchy(superClass, fullyQualifiedTypeName);
			if (res != null) {
				return res;
			}
		}
		ITypeBinding[] superInterfaces= hierarchyType.getInterfaces();
		for (int i= 0; i < superInterfaces.length; i++) {
			ITypeBinding res= findTypeInHierarchy(superInterfaces[i], fullyQualifiedTypeName);
			if (res != null) {
				return res;
			}			
		}
		return null;
	}
	
	public static boolean findTypeInHierarchy(ITypeBinding hierarchyType, ITypeBinding type) {
		if (hierarchyType.isArray() || hierarchyType.isPrimitive()) {
			return false;
		}
		if (hierarchyType == type) {
			return true;
		}
		ITypeBinding superClass= hierarchyType.getSuperclass();
		if (superClass != null) {
			if (findTypeInHierarchy(superClass, type)) {
				return true;
			}
		}
		
		if (type.isInterface()) {
			ITypeBinding[] superInterfaces= hierarchyType.getInterfaces();
			for (int i= 0; i < superInterfaces.length; i++) {
				if (findTypeInHierarchy(superInterfaces[i], type)) {
					return true;
				}			
			}
		}
		return false;
	}		
	

}
