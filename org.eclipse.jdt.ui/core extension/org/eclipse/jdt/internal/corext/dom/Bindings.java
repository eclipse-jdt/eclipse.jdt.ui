/*
 * (c) Copyright IBM Corp. 2000, 2001, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

public class Bindings {
	private Bindings() {
	}
	
	public static String toString(IMethodBinding method) {
		StringBuffer result= new StringBuffer(method.getName());
		result.append('(');
		ITypeBinding[] parameters= method.getParameterTypes();
		int lastComma= parameters.length - 1;
		for (int i= 0; i < parameters.length; i++) {
			ITypeBinding parameter= parameters[i];
			result.append(parameter.getName());
			if (i < lastComma)
				result.append(", ");
		}
		result.append(')');
		return result.toString();
	}
}
