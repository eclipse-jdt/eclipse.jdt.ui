/*
 * (c) Copyright IBM Corp. 2000, 2001, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.dom;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;

public class Bindings {
	
	private Bindings() {
		// No instance
	}
	
	public static String asString(IMethodBinding method) {
		StringBuffer result= new StringBuffer(method.getName());
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
		if (type.isPrimitive())
			return type.getName();
		StringBuffer buffer= new StringBuffer();
		createName(buffer, type);
		return buffer.toString();
	}
	
	public static String getFullyQualifiedName(ITypeBinding type) {
		if (type.isPrimitive())
			return type.getName();
		StringBuffer buffer= new StringBuffer();
		if (!type.getPackage().isUnnamed()) {
			buffer.append(type.getPackage().getName());
			buffer.append('.');
		}
		createName(buffer, type);
		return buffer.toString();		
	}
	
	public static String getFullyQualifiedImportName(ITypeBinding type) {
		if (type.isArray())
			return getFullyQualifiedName(type.getElementType());
		else	
			return getFullyQualifiedName(type);
	}
	
	public static String[] getNameComponents(ITypeBinding type) {
		if (type.isPrimitive())
			return new String[] { type.getName() };
		List result= new ArrayList(3);
		while(type != null) {
			if (type.isAnonymous())
				result.add(0, "$local$"); //$NON-NLS-1$
			else
				result.add(0, type.getName());
			type= type.getDeclaringClass();
		}
		return (String[]) result.toArray(new String[result.size()]);
	}
	
	public static String[] getAllNameComponents(ITypeBinding type) {
		String[] typeComponents= getNameComponents(type);
		if (type.isPrimitive())
			return typeComponents;
		IPackageBinding pack= type.getPackage();
		if (pack.isUnnamed())
			return typeComponents;
		String[] packComponents= pack.getNameComponents();
		String[] result= new String[packComponents.length + typeComponents.length];
		System.arraycopy(packComponents, 0, result, 0, packComponents.length);
		System.arraycopy(typeComponents, 0, result, packComponents.length, typeComponents.length);
		return result;
	}
	
	public static ITypeBinding getTopLevelType(ITypeBinding type) {
		ITypeBinding parent= type.getDeclaringClass();
		while (parent != null) {
			type= parent;
			parent= type.getDeclaringClass();
		}
		return type;
	}
	
	public static Type createType(ITypeBinding binding, AST ast) {
		if (binding.isPrimitive()) {
			PrimitiveType.Code code= null;
			String name= binding.getName();
			if (name.equals("int")) //$NON-NLS-1$
				code= PrimitiveType.INT;
			else if (name.equals("char")) //$NON-NLS-1$
				code= PrimitiveType.CHAR;
			else if (name.equals("boolean")) //$NON-NLS-1$
				code= PrimitiveType.BOOLEAN;
			else if (name.equals("short")) //$NON-NLS-1$
				code= PrimitiveType.SHORT;
			else if (name.equals("long")) //$NON-NLS-1$
				code= PrimitiveType.LONG;
			else if (name.equals("float")) //$NON-NLS-1$
				code= PrimitiveType.FLOAT;
			else if (name.equals("double")) //$NON-NLS-1$
				code= PrimitiveType.DOUBLE;
			else if (name.equals("byte")) //$NON-NLS-1$
				code= PrimitiveType.BYTE;
			else if (name.equals("void")) //$NON-NLS-1$
				code= PrimitiveType.VOID;
			return ast.newPrimitiveType(code);
		} else if (binding.isArray()) {
			Type elementType= createType(binding.getElementType(), ast);
			return ast.newArrayType(elementType, binding.getDimensions());
		} else {
			return ast.newSimpleType(ast.newName(Bindings.getAllNameComponents(binding)));
		}
	}

	/**
	 * Checks whether	the passed type binding is a runtime exception.
	 * 
	 * @return <code>true</code> if the passed type binding is a runtime exception;
	 * 	otherwise <code>false</code> is returned
	 */
	public static boolean isRuntimeException(ITypeBinding thrownException, AST ast) {
		if (thrownException == null || thrownException.isPrimitive())
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
	
	private static boolean isEqualMethod(IMethodBinding method, String methodName, ITypeBinding[] parameters) {
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
	
	private static void createName(StringBuffer buffer, ITypeBinding type) {
		ITypeBinding declaringType= type.getDeclaringClass();
		if (declaringType != null) {
			createName(buffer, declaringType);
			buffer.append('.');
		}
		buffer.append(type.getName());
	}
}
