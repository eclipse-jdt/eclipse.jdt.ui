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
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.Assert;



public class CodeFormatterUtil {
	
	private static boolean OLD_FORMATTER= true;
	
	
	public static final int K_EXPRESSION = CodeFormatter.K_EXPRESSION;
	public static final int K_STATEMENTS = CodeFormatter.K_STATEMENTS;
	public static final int K_CLASS_BODY_DECLARATIONS = CodeFormatter.K_CLASS_BODY_DECLARATIONS;
	public static final int K_COMPILATION_UNIT = CodeFormatter.K_COMPILATION_UNIT;
		 
	/**
	 * Creates a string that represents the given number of indents (can be spaces or tabs..)
	 */
	public static String createIndentString(int indent) {
		if (OLD_FORMATTER) {
			return old_format("", indent, null, "", null);  //$NON-NLS-1$//$NON-NLS-2$
		} else {
			//TODO
			return ""; //$NON-NLS-1$
		}
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

	// facade API to allow switching between old and new code formatter
	
	public static String format(int kind, String string, int indentationLevel, int[] positions, String lineSeparator, Map options) {
		if (OLD_FORMATTER) {
			return old_format(string, indentationLevel, positions, lineSeparator, options);
		} else {
			//TODO
			return string;
		}	
	}
		


	public static String format(String string, int start, int end, int indentationLevel, int[] positions, String lineSeparator, Map options) {
		return format(K_COMPILATION_UNIT, string, start, end, indentationLevel, positions, lineSeparator, options);
	}
	
	// suggested functionality
	
	public static String format(int kind, String string, int start, int end, int indentationLevel, int[] positions, String lineSeparator, Map options) {
		if (OLD_FORMATTER) {
			return emulateFormatSubstring(string, start, end, indentationLevel, positions, lineSeparator, options);
		} else {
			//TODO
			return string;
		}
	}
	
	// transition code

	private static String old_format(String string, int indentationLevel, int[] positions, String lineSeparator, Map options) {
		ICodeFormatter formatter= ToolFactory.createDefaultCodeFormatter(options);
		return formatter.format(string, indentationLevel, positions, lineSeparator);
	}	
	
	/*
	 * emulate the fomat substring with the old formatter
	 */ 
	private static String emulateFormatSubstring(String string, int start, int end, int indentationLevel, int[] positions, String lineSeparator, Map options) {
		int inclEnd= end - 1;
		
		// sort 'start' and 'end' into the existing position array
		
		int[] newPositions= (positions != null) ? new int[positions.length + 2] : new int[2];
		int k= 0, i= 0;
		if (positions != null) {
			while (i < positions.length && positions[i] <= start) {
				newPositions[k++]= positions[i++];
			}
		}
		int startIndex= k;
		newPositions[k++]= start;
		if (positions != null) {
			while (i < positions.length && positions[i] < inclEnd) {
				newPositions[k++]= positions[i++];
			}
		}		
		int endIndex= k;
		newPositions[k++]= inclEnd;
		if (positions != null) {
			while (i < positions.length) {
				newPositions[k++]= positions[i++];   
			}
		}
		
		String formatted= old_format(string, indentationLevel, newPositions, lineSeparator, options);
		
		int newStartPos= newPositions[startIndex];
		int newEndPos= newPositions[endIndex] + 1; // incl. end 
		
		// update the positions array
		
		if (positions != null) {
			i= 0;
			int startDiff= newStartPos - start;
			int endDiff= newEndPos - end - startDiff;
			for (k= 0; k < newPositions.length; k++) {
				if (k < startIndex) {
					i++; // no change
				} else if (k > startIndex && k < endIndex) {
					positions[i++]= newPositions[k] - startDiff;
				} else if (k > endIndex) {
					int val= positions[i] + endDiff;
					positions[i++]= val;
				}
			}
		}
		
		return string.substring(0, start) + formatted.substring(newStartPos, newEndPos) + string.substring(end);
	}
	
	// common functionality
	
	/**
	 * Format the source whose kind is described by the passed AST node. This AST node is only used for type tests. It can be
	 * a dummy node with no content.
	 */
	public static String format(ASTNode node, String str, int indentationLevel, int[] positions, String lineSeparator, Map options) {		
		int code;
		String prefix= ""; //$NON-NLS-1$
		String suffix= ""; //$NON-NLS-1$
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
			if (OLD_FORMATTER) {
				// fix for bug with positions in old formatter
				return old_format(str, indentationLevel, positions, lineSeparator, options);
			}
			
			suffix= "void foo();"; //$NON-NLS-1$
			code= CodeFormatterUtil.K_CLASS_BODY_DECLARATIONS;
		} else if (node instanceof CatchClause) {
			prefix= "try {}"; //$NON-NLS-1$
			code= CodeFormatterUtil.K_STATEMENTS;
		} else if (node instanceof AnonymousClassDeclaration) {
			prefix= "new A()"; //$NON-NLS-1$
			code= CodeFormatterUtil.K_STATEMENTS;
		} else {
			Assert.isTrue(false, "Node type not covered: " + node.getClass().getName()); //$NON-NLS-1$
			return null;
		}
		if (prefix.length() + suffix.length() == 0) {
			return format(code, str, indentationLevel, positions, lineSeparator, options);
		} else {
			String concatStr= prefix + str + suffix;
			int strStart= prefix.length();
			int strEnd= concatStr.length() - suffix.length();
			if (positions != null) {
				for (int i= 0; i < positions.length; i++) {
					positions[i] += strStart;
				}
			}
			String formatted= format(code, concatStr, strStart, strEnd, indentationLevel, positions, lineSeparator, options);
			if (positions != null) {
				for (int i= 0; i < positions.length; i++) {
					positions[i] -= strStart;
				} 
			}			
			return formatted.substring(strStart, formatted.length() - suffix.length());
		}
	}	
	

	
}
