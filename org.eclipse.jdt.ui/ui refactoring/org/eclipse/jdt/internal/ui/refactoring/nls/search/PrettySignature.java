/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.nls.search;

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
					return getFullyQualifiedName((IType)element);
				default:
					return element.getElementName();
			}
	}
	
	public static String getMethodSignature(IMethod method) {
		StringBuffer buffer= new StringBuffer();
		buffer.append(getFullyQualifiedName(method.getDeclaringType()));
		buffer.append('.');
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
			buffer.append(Signature.toString(types[0]));
		for (int i= 1; i < types.length; i++) {
			buffer.append(", "); //$NON-NLS-1$
			buffer.append(Signature.toString(types[i]));
		}
		
		buffer.append(')');
		
		return buffer.toString();
	}	

	public static String getUnqualifiedType(String typeName) {
		if (typeName == null)
			return null;
		int lastDotIndex= typeName.lastIndexOf('.');
		if (lastDotIndex < 0)
			return typeName;
		if (lastDotIndex > typeName.length() - 1)
			return ""; //$NON-NLS-1$
		return typeName.substring(lastDotIndex + 1);
	}
	/**
	 * Returns the fully qualified name of the given type using '.' as separators.
	 * This is a replace to IType.getFullyQualifiedTypeName
	 * which uses '$' as separators. As '$' is also a valid character in an id
	 * this is ambiguous. Hoping for a fix in JavaCore (1GCFUNT)
	 * 
	 * XXX: Copied from JavaModelUtility
	 */
	public static String getFullyQualifiedName(IType type) {
		// XXX: Copied from JavaModelUtility
		StringBuffer buf= new StringBuffer();
		String packName= type.getPackageFragment().getElementName();
		if (packName.length() > 0) {
			buf.append(packName);
			buf.append('.');
		}
		getTypeQualifiedName(type, buf);
		return buf.toString();
	}
	/* 
	 * XXX: Copied from JavaModelUtility
	 */
	private static void getTypeQualifiedName(IType type, StringBuffer buf) {
		IType outerType= type.getDeclaringType();
		if (outerType != null) {
			getTypeQualifiedName(outerType, buf);
			buf.append('.');
		}
		buf.append(type.getElementName());
	}	
}