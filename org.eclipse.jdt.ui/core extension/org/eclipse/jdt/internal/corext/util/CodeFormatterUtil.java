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

import org.eclipse.core.runtime.Preferences;

import org.eclipse.jdt.core.CodeFormatter;
import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;



public class CodeFormatterUtil {
	
	public static final int K_EXPRESSION = CodeFormatter.K_EXPRESSION;
	public static final int K_STATEMENTS = CodeFormatter.K_STATEMENTS;
	public static final int K_CLASS_BODY_DECLARATIONS = CodeFormatter.K_CLASS_BODY_DECLARATIONS;
	public static final int K_COMPILATION_UNIT = CodeFormatter.K_COMPILATION_UNIT;
		 
	/**
	 * Creates a string that represents the given number of indents.
	 */
	public static String createIndentString(int indent) {
		ICodeFormatter formatter= ToolFactory.createDefaultCodeFormatter(null);
		return formatter.format("", indent, null, ""); //$NON-NLS-1$ //$NON-NLS-2$
	} 
		
	public static int getTabWidth() {
		Preferences preferences= JavaCore.getPlugin().getPluginPreferences();
		return preferences.getInt(JavaCore.FORMATTER_TAB_SIZE);
	}
	
	public static String format(int kind, String string, int indentationLevel, int[] positions, String lineSeparator) {
		ICodeFormatter formatter= ToolFactory.createDefaultCodeFormatter(null);
		return formatter.format(string, indentationLevel, positions, lineSeparator);	
	}
	
	public static String format(String string, int start, int end, int indentationLevel, int[] positions, String lineSeparator) {
		ICodeFormatter formatter= ToolFactory.createDefaultCodeFormatter(null);
		return formatter.format(string.substring(start, end), indentationLevel, positions, lineSeparator);			
	}

	
}
