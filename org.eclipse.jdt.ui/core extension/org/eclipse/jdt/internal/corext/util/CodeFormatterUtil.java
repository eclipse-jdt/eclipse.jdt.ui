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

import java.util.Map;

import org.eclipse.core.runtime.Preferences;

import org.eclipse.jdt.core.CodeFormatter;
import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.ASTNode;



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

	/**
	 * @deprecated use format(kind, string, indentationLevel, positions, lineSeparator, null);
	 */
	public static String format(int kind, String string, int indentationLevel, int[] positions, String lineSeparator) {
		return format(kind, string, indentationLevel, positions, lineSeparator, null);
	}

	
	public static String format(int kind, String string, int indentationLevel, int[] positions, String lineSeparator, Map options) {
		ICodeFormatter formatter= ToolFactory.createDefaultCodeFormatter(options);
		return formatter.format(string, indentationLevel, positions, lineSeparator);	
	}
		
	public static String format(String string, int start, int end, int indentationLevel, int[] positions, String lineSeparator, Map options) {
		ICodeFormatter formatter= ToolFactory.createDefaultCodeFormatter(options);
		return formatter.format(string.substring(start, end), indentationLevel, positions, lineSeparator);			
	}
	
	public static String format(ASTNode node, String str, int indentationLevel, int[] positions, String lineSeparator, Map options) {
		ICodeFormatter formatter= ToolFactory.createDefaultCodeFormatter(options);
		return formatter.format(str, indentationLevel, positions, lineSeparator);	
	
		/*	
		int code;
		String prefix= null;
		String suffix= null;
		if (node instanceof CompilationUnit) {
			code= CodeFormatterUtil.K_COMPILATION_UNIT;
		} else if (node instanceof BodyDeclaration) {
			code= CodeFormatterUtil.K_CLASS_BODY_DECLARATIONS;
		} else if (node instanceof Statement) {
			code= CodeFormatterUtil.K_STATEMENTS;
		} else if (node instanceof Expression) {
			code= CodeFormatterUtil.K_EXPRESSION;
		} else if (node instanceof Type) {
			suffix= " x;"; //$NON-NLS-1$
			code= CodeFormatterUtil.K_STATEMENTS;
		} else if (node instanceof SingleVariableDeclaration) {
			suffix= ";"; //$NON-NLS-1$
			code= CodeFormatterUtil.K_STATEMENTS;			
		} else if (node instanceof VariableDeclarationFragment) {
			prefix= "A "; //$NON-NLS-1$
			suffix= ";"; //$NON-NLS-1$
			code= CodeFormatterUtil.K_STATEMENTS;
		} else if (node instanceof PackageDeclaration || node instanceof ImportDeclaration) {
			suffix= "\nclass A {}"; //$NON-NLS-1$
			code= CodeFormatterUtil.K_COMPILATION_UNIT;
		} else if (node instanceof Javadoc) {
			suffix= "void foo();"; //$NON-NLS-1$
			code= CodeFormatterUtil.K_CLASS_BODY_DECLARATIONS;
		} else if (node instanceof CatchClause) {
			prefix= "try {}"; //$NON-NLS-1$
			code= CodeFormatterUtil.K_STATEMENTS;
		} else if (node instanceof AnonymousClassDeclaration) {
			prefix= "try {}"; //$NON-NLS-1$
			code= CodeFormatterUtil.K_STATEMENTS;
		}
		return null;*/
	}	
	

	
}
