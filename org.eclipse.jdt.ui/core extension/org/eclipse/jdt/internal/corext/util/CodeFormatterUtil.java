/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;

public class CodeFormatterUtil {

	/**
	 * Returns the indent of the given line.
	 * @param line the text line
	 */
	public static int getIndent(String line) {
		return getIndent(line, getTabWidth());
	}
	
	/**
	 * Returns the indent of the given line.
	 * @param line the text line
	 * @param tabWidth the width of the '\t' character.
	 */
	public static int getIndent(String line, int tabWidth) {
		int result= 0;
		int blanks= 0;
		int size= line.length();
		for (int i= 0; i < size; i++) {
			char c= line.charAt(i);
			switch (c) {
				case '\t':
					result++;
					blanks= 0;
					break;
				case ' ':
					blanks++;
					if (blanks == tabWidth) {
						result++;
						blanks= 0;
					}
					break;
				default:
					return result;
			}
		}
		return result;
	}
			
	/**
	 * Removes the given number of idents from the line. Asserts that the given line 
	 * has the requested number of indents. If <code>indentsToRemove <= 0</code>
	 * the line is returned.
	 */
	public static String removeIndent(String line, int indentsToRemove, int tabWidth) {
		if (line == null || indentsToRemove <= 0)
			return line;
			
		int start= 0;
		int indents= 0;
		int blanks= 0;
		int size= line.length();
		for (int i= 0; i < size; i++) {
			char c= line.charAt(i);
			switch (c) {
				case '\t':
					indents++;
					blanks= 0;
					break;
				case ' ':
					blanks++;
					if (blanks == tabWidth) {
						indents++;
						blanks= 0;
					}
					break;
				default:
					Assert.isTrue(false, "Line does not have requested number of indents"); //$NON-NLS-1$
			}
			if (indents == indentsToRemove) {
				start= i + 1;
				break;
			}	
		}
		if (start == size)
			return ""; //$NON-NLS-1$
		else
			return line.substring(start);
	}

	public static void removeIndentation(String[] lines, int tabWidth) {
		int l;
		// find indentation common to all lines
		int minIndent= Integer.MAX_VALUE; // very large
		for (l= 0; l < lines.length; l++) {
			int indent= getIndent(lines[l], tabWidth);
			if (indent < minIndent)
				minIndent= indent;
		}
		if (minIndent > 0)
			// remove this indent from all lines
			for (l= 0; l < lines.length; l++)
				lines[l]= removeIndent(lines[l], minIndent, tabWidth);
	}
	
	/**
	 * Removes any leading indents from the given string.
	 */
	public static String removeLeadingIndents(String line) {
		return removeLeadingIndents(line, getTabWidth());
	}
	 
	/**
	 * Removes any leading indents from the given string.
	 */
	public static String removeLeadingIndents(String line, int tabWidth) {
		int indents= getIndent(line, tabWidth);
		return removeIndent(line, indents, tabWidth);
	}
	 
	/**
	 * Creates a string that represents the given number of indents.
	 */
	public static String createIndentString(int indent) {
		ICodeFormatter formatter= ToolFactory.createCodeFormatter();
		return formatter.format("", indent, null, "");
	} 
	
	public static String createIndentString(String example) {
		return createIndentString(getIndent(example));
	}
	
	/**
	 * Removes any leading white spaces from the given string.
	 * The method returns a new string.
	 */
	public static String removeLeadingWhiteSpaces(String line) {
		int size= line.length();
		int start= 0;
		for (int i= 0; i < size; i++) {
			char c= line.charAt(i);
			if (c != '\t' && c != ' ') {
				start= i;
				break;
			}
		}
		return line.substring(start);
	}
	
	/**
	 * Returns <code>true</code> if the given string consists only of
	 * white spaces (e.g. space and '\t'). If the string is empty,
	 * <code>true</code> is returned.
	 */
	public static boolean containsOnlyWhiteSpaces(String line) {
		int size= line.length();
		for (int i= 0; i < size; i++) {
			char c= line.charAt(i);
			if (c != '\t' && c != ' ')
				return false;
		}
		return true;
	}
	
	public static int getTabWidth() {
		Hashtable options= JavaCore.getOptions();
		String result= (String)options.get("org.eclipse.jdt.core.formatter.tabulation.size"); //$NON-NLS-1$
		return Integer.parseInt(result);
	}
	
	public static String createMethodDeclaration(String signature, String[] body, String lineSeparator)  {
		final String dummy= signature + "{ x(); }"; //$NON-NLS-1$
		final String placeHolder= "x();"; //$NON-NLS-1$
		ICodeFormatter formatter= ToolFactory.createCodeFormatter();
		int placeHolderStart= dummy.indexOf(placeHolder);
		Assert.isTrue(placeHolderStart != -1, "Place holder not found in original statements"); //$NON-NLS-1$
		int[] positions= new int[] {placeHolderStart, placeHolderStart + placeHolder.length() - 1};
		String formattedCode= formatter.format(dummy, 0, positions, lineSeparator);
		TextBuffer buffer= TextBuffer.create(formattedCode);
		int tabWidth= getTabWidth();
		removeIndentation(body, tabWidth);
		String placeHolderLine= buffer.getLineContentOfOffset(positions[0]);
		String indent= placeHolderLine.substring(0, placeHolderLine.indexOf(placeHolder));
		StringBuffer result= new StringBuffer();
		result.append(formattedCode.substring(0, positions[0]));
		for (int i= 0; i < body.length; i++) {
			if (i > 0)
				result.append(indent);
			result.append(body[i]);
			if (i < body.length - 1)
				result.append(lineSeparator);
		}
		result.append(formattedCode.substring(positions[1] + 1));
		return result.toString();
	}
	
	public static int probeMethodSpacing(TextBuffer buffer, MethodDeclaration method) {
		MethodDeclaration[] methods= getSiblings(method);
		if (methods != null && methods.length >= 1) {
			int start;
			if (methods.length == 1)
				start= methods[0].getStartPosition();
			else
				start= methods[1].getStartPosition();
			int lineNumber= buffer.getLineOfOffset(start);
			int result= 0;
			while (lineNumber > 0) {
				lineNumber--;
				String line= buffer.getLineContent(lineNumber);
				if (containsOnlyWhiteSpaces(line)) {
					result++;
				} else {
					return result;
				}
			}
		}
		return 1;
	}
	
	private static MethodDeclaration[] getSiblings(MethodDeclaration method) {
		ASTNode parent= method.getParent();
		if (parent instanceof TypeDeclaration)
			return ((TypeDeclaration)parent).getMethods();
		if (parent instanceof AnonymousClassDeclaration) {
			List body= ((AnonymousClassDeclaration)parent).bodyDeclarations();
			List result= new ArrayList();
			for (Iterator iter= body.iterator(); iter.hasNext();) {
				Object element= iter.next();
				if (element instanceof MethodDeclaration)
					result.add(element);
			}
			return (MethodDeclaration[]) result.toArray(new MethodDeclaration[result.size()]);
		}
		return null;
	}
}
