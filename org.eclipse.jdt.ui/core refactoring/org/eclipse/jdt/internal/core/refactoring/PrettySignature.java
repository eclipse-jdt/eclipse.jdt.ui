/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;

class PrettySignature {

	public static String getSignature(IJavaElement element) {
		if (element == null)
			return null;
		else
			switch (element.getElementType()) {
				case IJavaElement.METHOD:
					return getMethodSignature((IMethod)element);
				case IJavaElement.TYPE:
					return ((IType)element).getFullyQualifiedName();
				default:
					return element.getElementName();
			}
	}
	
	public static String getMethodSignature(IMethod method) {
		StringBuffer buffer= new StringBuffer();
		buffer.append(method.getDeclaringType().getFullyQualifiedName());
		buffer.append("::"); //$NON-NLS-1$
		buffer.append(getUnqualifiedMethodSignature(method));
		
		return buffer.toString();
	}

	public static String getUnqualifiedTypeSignature(IType type) {
		return type.getElementName();
	}
	
	public static String getUnqualifiedMethodSignature(IMethod method) {
		StringBuffer buffer= new StringBuffer();
		buffer.append(method.getElementName());
		buffer.append('(');
		
		String[] types= method.getParameterTypes();
		if (types.length > 0)
			buffer.append(Signature.getSimpleName(Signature.toString(types[0])));
		for (int i= 1; i < types.length; i++) {
			buffer.append(", "); //$NON-NLS-1$
			buffer.append(Signature.getSimpleName(Signature.toString(types[i])));
		}
		
		buffer.append(')');
		
		return buffer.toString();
	}	
}