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
package org.eclipse.jdt.ui;
 
import java.io.Reader;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.corext.dom.TokenScanner;
import org.eclipse.jdt.internal.corext.javadoc.JavaDocCommentReader;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.text.javadoc.JavaDoc2HTMLTextReader;

/**
 * Helper needed get the content of a Javadoc comment. 
 *
 * @since 3.1
 */
public class JavadocContentAccess {
	
	/**
	 * Gets a reader for an IMember's Javadoc comment content from the source attachment.
	 * The content does contain only the text from the comment without the Javadoc leading star characters.
	 * Returns null if the member does not contain a Javadoc comment or if no source is available.
	 * @param member The member to get the Javadoc of.
	 * @param allowInherited For methods with no (Javadoc) comment, the comment of the overridden class
	 * is returned if <code>allowInherited</code> is <code>true</code>.
	 * @throws JavaModelException is thrown when the elements javadoc can not be accessed
	 */
	public static Reader getContentReader(IMember member, boolean allowInherited) throws JavaModelException {
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
				int docOffset= -1;
				int docEnd= -1;
				
				int terminal= scanner.getNextToken();
				while (TokenScanner.isComment(terminal)) {
					if (terminal == ITerminalSymbols.TokenNameCOMMENT_JAVADOC) {
						docOffset= scanner.getCurrentTokenStartPosition();
						docEnd= scanner.getCurrentTokenEndPosition() + 1;
					}
					terminal= scanner.getNextToken();
				}
				if (docOffset != -1) {
					return new JavaDocCommentReader(buf, docOffset, docEnd);
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
	 * Gets a reader for an IMember's Javadoc comment content from the source attachment.
	 * and renders the tags in HTML. 
	 * Returns null if the member does not contain a Javadoc comment or if no source is available.
	 * @param member The member to get the Javadoc of.
	 * @param allowInherited For methods with no (Javadoc) comment, the comment of the overridden class
	 * is returned if <code>allowInherited</code> is <code>true</code>.
	 * @throws JavaModelException is thrown when the elements javadoc can not be accessed
	 */
	public Reader getHTMLContentReader(IMember member, boolean allowInherited) throws JavaModelException {
		Reader contentReader= getContentReader(member, allowInherited);
		if (contentReader != null) {
			return new JavaDoc2HTMLTextReader(contentReader);
		}
		return null;
	}

	private static Reader findDocInHierarchy(IType type, String name, String[] paramTypes, boolean isConstructor) throws JavaModelException {
		ITypeHierarchy hierarchy= type.newSupertypeHierarchy(null);
		IType[] superTypes= hierarchy.getAllSupertypes(type);
		for (int i= 0; i < superTypes.length; i++) {
			IMethod method= JavaModelUtil.findMethod(name, paramTypes, isConstructor, superTypes[i]);
			if (method != null) {
				Reader reader= getContentReader(method, false);
				if (reader != null) {
					return reader;
				}
			}
		}
		return null;
	}		

}
