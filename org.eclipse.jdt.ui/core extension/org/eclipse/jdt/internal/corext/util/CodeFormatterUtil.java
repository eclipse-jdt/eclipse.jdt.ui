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

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;

public class CodeFormatterUtil {

	/**
	 * Returns the indent of the given line.
	 * @param line the text line
	 */
	public static int computeIndent(String line) {
		return Strings.computeIndent(line, getTabWidth());
	}
	
	public static void removeIndentation(String[] lines) {
		Strings.trimIndentation(lines, getTabWidth());
	}
	
	/**
	 * Removes any leading indents from the given string.
	 */
	public static String trimIndents(String line) {
		return Strings.trimIndents(line, getTabWidth());
	}
	 
	/**
	 * Creates a string that represents the given number of indents.
	 */
	public static String createIndentString(int indent) {
		ICodeFormatter formatter= ToolFactory.createCodeFormatter();
		return formatter.format("", indent, null, ""); //$NON-NLS-1$ //$NON-NLS-2$
	} 
	
	public static String createIndentString(String example) {
		return createIndentString(computeIndent(example));
	}
	
	public static int getTabWidth() {
		Hashtable options= JavaCore.getOptions();
		try {
			String result= (String)options.get(JavaCore.FORMATTER_TAB_SIZE);
			return Integer.parseInt(result);
		} catch (NumberFormatException e) {
			return 4;
		}
	}	
}
