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
package org.eclipse.jdt.internal.corext.javadoc;

 
import java.io.IOException;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;


public class JavaDocAccess {
	
	

	
	/**
	 * Gets a reader for an IMember's JavaDoc comment
	 * Returns null if the member does not contain a JavaDoc comment or
	 * if no source is available.
	 * @param allowInherited For methods with no comment, the comment of the overriden class
	 * is returned if <code>allowInherited</code> is <code>true</code>.
	 */
	public static JavaDocCommentReader getJavaDoc(IMember member, boolean allowInherited) throws JavaModelException {
		IBuffer buf= member.isBinary() ? member.getClassFile().getBuffer() : member.getCompilationUnit().getBuffer();
		if (buf == null) {
			// no source attachment found
			return null;
		}
		ISourceRange range= member.getSourceRange();
		int start= range.getOffset();
		int length= range.getLength();
		if (length > 0 && buf.getChar(start) == '/') {
			IScanner scanner= ToolFactory.createScanner(true, false, false, false);
			scanner.setSource(buf.getCharacters());
			scanner.resetTo(start, start + length - 1);
			try {
				int terminal= scanner.getNextToken();
				while (terminal == ITerminalSymbols.TokenNameCOMMENT_LINE || terminal == ITerminalSymbols.TokenNameCOMMENT_BLOCK)
					terminal= scanner.getNextToken();
				if (terminal == ITerminalSymbols.TokenNameCOMMENT_JAVADOC) {
					start= scanner.getCurrentTokenStartPosition();
					int end= scanner.getCurrentTokenEndPosition() + 1;
					return new JavaDocCommentReader(buf, start, end);
				}
			} catch (InvalidInputException ex) {
				// try if there is inherited Javadoc
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
	public static JavaDocCommentReader getJavaDoc(IMember member) throws JavaModelException {
		return getJavaDoc(member, false);
	}

	private static JavaDocCommentReader findDocInHierarchy(IType type, String name, String[] paramTypes, boolean isConstructor) throws JavaModelException {
		ITypeHierarchy hierarchy= type.newSupertypeHierarchy(null);
		IType[] superTypes= hierarchy.getAllSupertypes(type);
		for (int i= 0; i < superTypes.length; i++) {
			IMethod method= JavaModelUtil.findMethod(name, paramTypes, isConstructor, superTypes[i]);
			if (method != null) {
				JavaDocCommentReader reader= getJavaDoc(method, false);
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
	
	/**
	 * Returns a JavaDoc tags for a comment at the given location. <code>null</code> is returned if the openable
	 * has no source attached.
	 */
	public static JavaDocTag[] getJavaDocTags(IOpenable openable, int offset, int length) throws JavaModelException {
		IBuffer buffer= openable.getBuffer();
		if (buffer != null) {
			return JavaDocTag.createFromComment(new JavaDocCommentReader(buffer, offset, offset + length));
		}
		return null;
	}
		
	
	/**
	 * Returns a IMember's JavaDoc tags. <code>null</code> is returned if the member 
	 * does not contain a JavaDoc comment or if no source is available.
	 */
	public static JavaDocTag[] getJavaDocTags(IMember member, boolean allowInherited) throws JavaModelException {
		JavaDocCommentReader rd= getJavaDoc(member, allowInherited);
		if (rd != null)
			return JavaDocTag.createFromComment(rd);
		
		return null;
	}	
	

}
