package org.eclipse.jdt.internal.corext.util;

import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.util.CharOperation;

/**
 * Helper class to deal with compiler bindings.
 */
public class Bindings {

	private Bindings() {
		// No instance
	}
	
	/**
	 * Finds the method specified by <code>methodName<code> and </code>parameters</code> in
	 * the given <code>type</code>. Returns <code>null</code> if no such method exits.
	 */
	public static MethodBinding findMethodInType(ReferenceBinding type, char[] methodName, TypeBinding[] parameters) {
		MethodBinding[] methods= type.methods();
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
	public static MethodBinding findMethodInHierarchy(ReferenceBinding type, char[] methodName, TypeBinding parameters[]) {
		while (type != null) {
			MethodBinding method= findMethodInType(type, methodName, parameters);
			if (method != null)
				return method;
			type= type.superclass();
		}
		return null;
	}
	
	public static String makeFullyQualifiedName(char[] packageName, char[] qualifiedTypeName) {
		if (packageName == null || packageName.length == 0)
			return new String(qualifiedTypeName);
		StringBuffer buffer= new StringBuffer();
		buffer.append(packageName);
		buffer.append('.'); //$NON-NLS-1$
		buffer.append(qualifiedTypeName);
		return buffer.toString();
	}
	
	
	private static boolean isEqualMethod(MethodBinding method, char[] methodName, TypeBinding[] parameters) {
		if (!CharOperation.equals(methodName, method.selector))
			return false;
			
		TypeBinding[] methodParameters= method.parameters;
		if (methodParameters.length != parameters.length)
			return false;
		for (int i= 0; i < parameters.length; i++) {
			if (parameters[i] != methodParameters[i])
				return false;
		}
		return true;
	}
}

