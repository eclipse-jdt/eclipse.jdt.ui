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

import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.Preferences;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Position;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.ui.JavaPlugin;

public class CodeFormatterUtil {
			
	/**
	 * Creates a string that represents the given number of indents (can be spaces or tabs..)
	 */
	public static String createIndentString(int indent) {
		String str= format(CodeFormatter.K_EXPRESSION, "x", indent, null, "", (Map) null); //$NON-NLS-1$ //$NON-NLS-2$
		return str.substring(0, str.indexOf('x'));
	} 
		
	public static int getTabWidth() {
		Preferences preferences= JavaCore.getPlugin().getPluginPreferences();
		return preferences.getInt(JavaCore.FORMATTER_TAB_SIZE);
	}

	// transition code

	/**
	 * Old API. Consider to use format2 (TextEdit)
	 */	
	public static String format(int kind, String string, int indentationLevel, int[] positions, String lineSeparator, Map options) {
		return format(kind, string, 0, string.length(), indentationLevel, positions, lineSeparator, options);
	}
	
	public static String format(int kind, String string, int indentationLevel, int[] positions, String lineSeparator, IJavaProject project) {
		Map options= project != null ? project.getOptions(true) : null;
		return format(kind, string, 0, string.length(), indentationLevel, positions, lineSeparator, options);
	}
	

	/**
	 * Old API. Consider to use format2 (TextEdit)
	 */	
	public static String format(int kind, String string, int offset, int length, int indentationLevel, int[] positions, String lineSeparator, Map options) {
		TextEdit edit= format2(kind, string, offset, length, indentationLevel, lineSeparator, options);
		if (edit == null) {
			//JavaPlugin.logErrorMessage("formatter failed to format (no edit returned). Will use unformatted text instead. kind: " + kind + ", string: " + string); //$NON-NLS-1$ //$NON-NLS-2$
			return string.substring(offset, offset + length);
		}
		String formatted= getOldAPICompatibleResult(string, edit, indentationLevel, positions, lineSeparator, options);
		return formatted.substring(offset, formatted.length() - (string.length() - (offset + length)));
	}
	
	/**
	 * Old API. Consider to use format2 (TextEdit)
	 */	
	public static String format(ASTNode node, String string, int indentationLevel, int[] positions, String lineSeparator, Map options) {
		
		TextEdit edit= format2(node, string, indentationLevel, lineSeparator, options);
		if (edit == null) {
			//JavaPlugin.logErrorMessage("formatter failed to format (no edit returned). Will use unformatted text instead. node: " + node.getNodeType() + ", string: " + string); //$NON-NLS-1$ //$NON-NLS-2$
			return string;
		}
		return getOldAPICompatibleResult(string, edit, indentationLevel, positions, lineSeparator, options);
	}
	
	private static String getOldAPICompatibleResult(String string, TextEdit edit, int indentationLevel, int[] positions, String lineSeparator, Map options) {
		Position[] p= null;
		
		if (positions != null) {
			p= new Position[positions.length];
			for (int i= 0; i < positions.length; i++) {
				p[i]= new Position(positions[i], 0);
			}
		}
		String res= evaluateFormatterEdit(string, edit, p);
		
		if (positions != null) {
			for (int i= 0; i < positions.length; i++) {
				Position curr= p[i];
				positions[i]= curr.getOffset();
			}
		}			
		return res;
	}
	
	/**
	 * Evaluates the edit on the given string.
	 * @throws IllegalArgumentException If the positions are not inside the string, a
	 *  IllegalArgumentException is thrown.
	 */
	public static String evaluateFormatterEdit(String string, TextEdit edit, Position[] positions) {
		try {
			Document doc= createDocument(string, positions);
			edit.apply(doc, 0);
			if (positions != null) {
				for (int i= 0; i < positions.length; i++) {
					Assert.isTrue(!positions[i].isDeleted, "Position got deleted"); //$NON-NLS-1$
				}
			}
			return doc.get();
		} catch (BadLocationException e) {
			JavaPlugin.log(e); // bug in the formatter
			Assert.isTrue(false, "Fromatter created edits with wrong positions: " + e.getMessage()); //$NON-NLS-1$
		}
		return null;
	}
	
	/**
	 * Creates edits that describe how to format the given string. Returns <code>null</code> if the code could not be formatted for the given kind.
	 * @throws IllegalArgumentException If the offset and length are not inside the string, a
	 *  IllegalArgumentException is thrown.
	 */
	public static TextEdit format2(int kind, String string, int offset, int length, int indentationLevel, String lineSeparator, Map options) {
		if (offset < 0 || length < 0 || offset + length > string.length()) {
			throw new IllegalArgumentException("offset or length outside of string. offset: " + offset + ", length: " + length + ", string size: " + string.length());   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		}
		return ToolFactory.createCodeFormatter(options).format(kind, string, offset, length, indentationLevel, lineSeparator);
	}
	
	public static TextEdit format2(int kind, String string, int indentationLevel, String lineSeparator, Map options) {
		return format2(kind, string, 0, string.length(), indentationLevel, lineSeparator, options);
	}
	
	/**
	 * Creates edits that describe how to format the given string. Returns <code>null</code> if the code could not be formatted for the given kind.
	 * @throws IllegalArgumentException If the offset and length are not inside the string, a
	 *  IllegalArgumentException is thrown.
	 */
	public static TextEdit format2(ASTNode node, String str, int indentationLevel, String lineSeparator, Map options) {
		int code;
		String prefix= ""; //$NON-NLS-1$
		String suffix= ""; //$NON-NLS-1$
		if (node instanceof Statement) {
			code= CodeFormatter.K_STATEMENTS;
			if (node.getNodeType() == ASTNode.SWITCH_CASE) {
				prefix= "switch(1) {"; //$NON-NLS-1$
				suffix= "}"; //$NON-NLS-1$
				code= CodeFormatter.K_STATEMENTS;
			}
		} else if (node instanceof Expression && node.getNodeType() != ASTNode.VARIABLE_DECLARATION_EXPRESSION) {
			code= CodeFormatter.K_EXPRESSION;
		} else {
			switch (node.getNodeType()) {
				case ASTNode.METHOD_DECLARATION:
				case ASTNode.TYPE_DECLARATION:
				case ASTNode.FIELD_DECLARATION:
				case ASTNode.INITIALIZER:
					code= CodeFormatter.K_CLASS_BODY_DECLARATIONS;
					break;
				case ASTNode.ARRAY_TYPE:
				case ASTNode.PRIMITIVE_TYPE:
				case ASTNode.SIMPLE_TYPE:
					suffix= " x;"; //$NON-NLS-1$
					code= CodeFormatter.K_EXPRESSION;
					break;
				case ASTNode.COMPILATION_UNIT:
					code= CodeFormatter.K_COMPILATION_UNIT;
					break;
				case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
				case ASTNode.SINGLE_VARIABLE_DECLARATION:
					suffix= ";"; //$NON-NLS-1$
					code= CodeFormatter.K_STATEMENTS;
					break;
				case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
					prefix= "A "; //$NON-NLS-1$
					suffix= ";"; //$NON-NLS-1$
					code= CodeFormatter.K_STATEMENTS;
					break;			
				case ASTNode.PACKAGE_DECLARATION:
				case ASTNode.IMPORT_DECLARATION:
					suffix= "\nclass A {}"; //$NON-NLS-1$
					code= CodeFormatter.K_COMPILATION_UNIT;
					break;
				case ASTNode.JAVADOC:
					suffix= "void foo();"; //$NON-NLS-1$
					code= CodeFormatter.K_CLASS_BODY_DECLARATIONS;
					break;
				case ASTNode.CATCH_CLAUSE:
					prefix= "try {}"; //$NON-NLS-1$
					code= CodeFormatter.K_STATEMENTS;
					break;
				case ASTNode.ANONYMOUS_CLASS_DECLARATION:
					prefix= "new A()"; //$NON-NLS-1$
					suffix= ";"; //$NON-NLS-1$
					code= CodeFormatter.K_STATEMENTS;
					break;
				default:
					Assert.isTrue(false, "Node type not covered: " + node.getClass().getName()); //$NON-NLS-1$
					return null;
			}
		}
		
		String concatStr= prefix + str + suffix;
		TextEdit edit= format2(code, concatStr, prefix.length(), str.length(), indentationLevel, lineSeparator, options);
		if (prefix.length() > 0) {
			edit= shifEdit(edit, prefix.length());
		}		
		return edit;
	}	
			
	private static TextEdit shifEdit(TextEdit oldEdit, int diff) {
		TextEdit newEdit;
		if (oldEdit instanceof ReplaceEdit) {
			ReplaceEdit edit= (ReplaceEdit) oldEdit;
			newEdit= new ReplaceEdit(edit.getOffset() - diff, edit.getLength(), edit.getText());
		} else if (oldEdit instanceof InsertEdit) {
			InsertEdit edit= (InsertEdit) oldEdit;
			newEdit= new InsertEdit(edit.getOffset() - diff,  edit.getText());
		} else if (oldEdit instanceof DeleteEdit) {
			DeleteEdit edit= (DeleteEdit) oldEdit;
			newEdit= new DeleteEdit(edit.getOffset() - diff,  edit.getLength());
		} else if (oldEdit instanceof MultiTextEdit) {
			newEdit= new MultiTextEdit();			
		} else {
			return null; // not supported
		}
		TextEdit[] children= oldEdit.getChildren();
		for (int i= 0; i < children.length; i++) {
			TextEdit shifted= shifEdit(children[i], diff);
			if (shifted != null) {
				newEdit.addChild(shifted);
			}
		}
		return newEdit;
	}
		
	private static Document createDocument(String string, Position[] positions) throws IllegalArgumentException {
		Document doc= new Document(string);
		try {
			if (positions != null) {
				final String POS_CATEGORY= "myCategory"; //$NON-NLS-1$
				
				doc.addPositionCategory(POS_CATEGORY);
				doc.addPositionUpdater(new DefaultPositionUpdater(POS_CATEGORY) {
					protected boolean notDeleted() {
						if (fOffset < fPosition.offset && (fPosition.offset + fPosition.length < fOffset + fLength)) {
							fPosition.offset= fOffset + fLength; // deleted positions: set to end of remove
							return false;
						}
						return true;
					}
				});
				for (int i= 0; i < positions.length; i++) {
					try {
						doc.addPosition(POS_CATEGORY, positions[i]);
					} catch (BadLocationException e) {
						throw new IllegalArgumentException("Position outside of string. offset: " + positions[i].offset + ", length: " + positions[i].length + ", string size: " + string.length());   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
					}
				}
			}
		} catch (BadPositionCategoryException cannotHappen) {
			// can not happen: category is correctly set up
		}
		return doc;
	}
	
}
