package org.eclipse.jdt.internal.corext.javadoc;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.io.IOException;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;


public class JavaDocAccess {
	
	private static int findCommentEnd(IBuffer buffer, int start, int end) {
		for (int i= start; i < end; i++) {
			char ch= buffer.getChar(i);
			if (ch == '*' && (i + 1 < end) && buffer.getChar(i + 1) == '/') {
				return i + 2;
			}
		}
		return -1;
	}	

	/**
	 * Gets a reader for an IMember's JavaDoc comment
	 * Returns null if the member does not contain a JavaDoc comment or
	 * if no source is available.
	 * @param allowInherited For methods with no comment, the comment of the overriden class
	 * is returned if <code>allowInherited</code> is <code>true</code>.
	 */
	public static SingleCharReader getJavaDoc(IMember member, boolean allowInherited) throws JavaModelException {
		IBuffer buf= member.isBinary() ? member.getClassFile().getBuffer() : member.getCompilationUnit().getBuffer();
		if (buf == null) {
			// no source attachment found
			return null;
		}
		ISourceRange range= member.getSourceRange();
		int start= range.getOffset();
		int length= range.getLength();
		if (length >= 5 && buf.getChar(start) == '/'
			&& buf.getChar(start + 1) == '*' && buf.getChar(start + 2) == '*') {

			int end= findCommentEnd(buf, start + 3, start + length);
			if (end != -1) {
				return new JavaDocCommentReader(buf, start, end);
			}
		}
		
		if (allowInherited && (member.getElementType() == IJavaElement.METHOD)) {
			IMethod method= (IMethod) member;
			return findDocInHierarchy(method.getDeclaringType(), method.getElementName(), method.getParameterTypes(), method.isConstructor());
		}
		return null;
	}

	/**
	 * Gets a reader for an IMember's JavaDoc comment
	 * Returns null if the member does not contain a JavaDoc comment or
	 * if no source is available.
	 */
	public static SingleCharReader getJavaDoc(IMember member) throws JavaModelException {
		return getJavaDoc(member, false);
	}

	private static SingleCharReader findDocInHierarchy(IType type, String name, String[] paramTypes, boolean isConstructor) throws JavaModelException {
		ITypeHierarchy hierarchy= type.newSupertypeHierarchy(null);
		IType[] superTypes= hierarchy.getAllSupertypes(type);
		for (int i= 0; i < superTypes.length; i++) {
			IMethod method= JavaModelUtil.findMethod(name, paramTypes, isConstructor, superTypes[i]);
			if (method != null) {
				SingleCharReader reader= getJavaDoc(method, false);
				if (reader != null) {
					return reader;
				}
			}
		}
		return null;
	}		

	
	/**
	 * Gets a text content for an IMember's JavaDoc comment
	 * Returns null if the member does not contain a JavaDoc comment or
	 * if no source is available.
	 */
	public static String getJavaDocTextString(IMember member, boolean allowInherited) throws JavaModelException {
		try {
			SingleCharReader rd= getJavaDoc(member, allowInherited);
			if (rd != null)
				return rd.getString();
				
		} catch (IOException e) {
			throw new JavaModelException(e, IStatus.ERROR);
		}
		
		return null;
	}	
		

}