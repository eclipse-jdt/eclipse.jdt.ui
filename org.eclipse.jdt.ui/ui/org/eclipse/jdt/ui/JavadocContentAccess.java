/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui;
 
import java.io.IOException;
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
 * <p>
 * This class is not intended to be subclassed or instantiated by clients.
 * </p>
 *
 * @since 3.1
 */
public class JavadocContentAccess {
	
	private JavadocContentAccess() {
		// do not instantiate
	}
	
	/**
	 * Gets a reader for an IMember's Javadoc comment content from the source attachment.
	 * The content does contain only the text from the comment without the Javadoc leading star characters.
	 * Returns <code>null</code> if the member does not contain a Javadoc comment or if no source is available.
	 * @param member The member to get the Javadoc of.
	 * @param allowInherited For methods with no (Javadoc) comment, the comment of the overridden class
	 * is returned if <code>allowInherited</code> is <code>true</code>.
	 * @return Returns a reader for the Javadoc comment content or <code>null</code> if the member
	 * does not contain a Javadoc comment or if no source is available
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
					JavaDocCommentReader reader= new JavaDocCommentReader(buf, docOffset, docEnd);
					if (!containsOnlyInheritDoc(reader, length)) {
						reader.reset();
						return reader;
					}
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
	 * Checks whether the given reader only returns
	 * the inheritDoc tag.
	 * 
	 * @param reader the reader
	 * @param length the length of the underlying content
	 * @return <code>true</code> if the reader only returns the inheritDoc tag
	 * @since 3.2
	 */
	private static boolean containsOnlyInheritDoc(Reader reader, int length) {
		char[] content= new char[length];
		try {
			reader.read(content, 0, length);
		} catch (IOException e) {
			return false;
		}
		return new String(content).trim().equals("{@inheritDoc}"); //$NON-NLS-1$
		
	}
	
	/**
	 * Gets a reader for an IMember's Javadoc comment content from the source attachment.
	 * and renders the tags in HTML. 
	 * Returns <code>null</code> if the member does not contain a Javadoc comment or if no source is available.
	 * @param member The member to get the Javadoc of.
	 * @param allowInherited For methods with no (Javadoc) comment, the comment of the overridden class
	 * is returned if <code>allowInherited</code> is <code>true</code>.
	 * @return Returns a reader for the Javadoc comment content in HTML or <code>null</code> if the member
	 * does not contain a Javadoc comment or if no source is available
	 * @throws JavaModelException is thrown when the elements javadoc can not be accessed
	 */
	public static Reader getHTMLContentReader(IMember member, boolean allowInherited) throws JavaModelException {
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
