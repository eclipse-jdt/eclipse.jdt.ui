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
package org.eclipse.jdt.internal.corext.util;

import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;

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
		ICodeFormatter formatter= ToolFactory.createCodeFormatter(null);
		return formatter.format("", indent, null, ""); //$NON-NLS-1$ //$NON-NLS-2$
	} 
	
	public static String createIndentString(String example) {
		return createIndentString(computeIndent(example));
	}
	
	public static int getTabWidth() {
		try {
			return Integer.parseInt((String)JavaCore.getOptions().get(JavaCore.FORMATTER_TAB_SIZE));
		} catch (NumberFormatException e) {
			return 4;
		}
	}	
}
