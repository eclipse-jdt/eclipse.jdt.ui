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

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.Preferences;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;

import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.TokenScanner;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatter;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.WorkInProgressPreferencePage;



public class CodeFormatterUtil {
	
	public static boolean OLD_FUNC= true;
	public static boolean DEBUG= false;
		
	
	public static boolean useOldFormatter() {
		return !PreferenceConstants.getPreferenceStore().getBoolean(WorkInProgressPreferencePage.PREF_FORMATTER);
	}

	/**
	 * Creates a string that represents the given number of indents (can be spaces or tabs..)
	 */
	public static String createIndentString(int indent) {
		if (useOldFormatter()) {
			return old_formatter("", indent, null, "", null);  //$NON-NLS-1$//$NON-NLS-2$
		} else {
			String str= format(CodeFormatter.K_EXPRESSION, "x", indent, null, "", null); //$NON-NLS-1$ //$NON-NLS-2$
			return str.substring(0, str.indexOf('x'));
		}
	} 
		
	public static int getTabWidth() {
		Preferences preferences= JavaCore.getPlugin().getPluginPreferences();
		return preferences.getInt(JavaCore.FORMATTER_TAB_SIZE);
	}

	// transition code

	private static String old_formatter(String string, int indentationLevel, int[] positions, String lineSeparator, Map options) {
		ICodeFormatter formatter= ToolFactory.createDefaultCodeFormatter(options);
		return formatter.format(string, indentationLevel, positions, lineSeparator);
	}	

	/**
	 * Old API. Consider to use format2 (TextEdit)
	 */	
	public static String format(int kind, String string, int indentationLevel, int[] positions, String lineSeparator, Map options) {
		return format(kind, string, 0, string.length(), indentationLevel, positions, lineSeparator, options);
	}
	

	/**
	 * Old API. Consider to use format2 (TextEdit)
	 */	
	public static String format(int kind, String string, int offset, int length, int indentationLevel, int[] positions, String lineSeparator, Map options) {
		TextEdit edit= format2(kind, string, offset, length, indentationLevel, lineSeparator, options);
		if (edit == null) {
			JavaPlugin.logErrorMessage("formatter failed to format (null returned). Will use unformatted instead. kind: " + kind + ", string: " + string); //$NON-NLS-1$ //$NON-NLS-2$
			return string;
		}
		String formatted= getOldAPICompatibleResult(string, edit, indentationLevel, positions, lineSeparator, options);
		return formatted.substring(offset, formatted.length() - (string.length() - (offset + length)));
	}
	
	/**
	 * Old API. Consider to use format2 (TextEdit)
	 */	
	public static String format(ASTNode node, String string, int indentationLevel, int[] positions, String lineSeparator, Map options) {
		if (OLD_FUNC && useOldFormatter()) {
			return old_formatter(string, indentationLevel, positions, lineSeparator, options);
		}
		
		TextEdit edit= format2(node, string, indentationLevel, lineSeparator, options);
		if (edit == null) {
			JavaPlugin.logErrorMessage("formatter failed to format (null returned). Will use unformatted instead. node: " + node.getNodeType() + ", string: " + string); //$NON-NLS-1$ //$NON-NLS-2$
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
		
		int[] positionCopy= null;
		if (positions != null) {
			if (DEBUG) {
				positionCopy= (int[]) positions.clone();
			}
			for (int i= 0; i < positions.length; i++) {
				Position curr= p[i];
				positions[i]= curr.getOffset();
			}
		}			
		if (DEBUG) {
			String oldFormat= old_formatter(string, indentationLevel, positionCopy, lineSeparator, options);
			testSame(res, oldFormat, positions, positionCopy);
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
		
		if (useOldFormatter()) {
			return emulateNewWithOld(string, offset, length, indentationLevel, lineSeparator, options);
		} else {
			return new DefaultCodeFormatter(options).format(kind, string, offset, length, indentationLevel, lineSeparator);
			//return ToolFactory.createCodeFormatter(options).format(kind, string, offset, length, indentationLevel, lineSeparator);
		}	
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
		} else if (node instanceof Expression) {
			code= CodeFormatter.K_EXPRESSION;
			if (node instanceof Type) {
				suffix= " x;"; //$NON-NLS-1$
			}
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
		
	private static TextEdit emulateNewWithOld(String string, int offset, int length, int indentationLevel, String lineSeparator, Map options) {
		String formatted= old_formatter(string, indentationLevel, null, lineSeparator, options);
		
		IScanner origScanner= getTokenScanner(string);
		IScanner newScanner= getTokenScanner(formatted);
		
		TextEdit result= new MultiTextEdit();
		
		int end= offset + length;
		try {
			int origNextStart= 0; // original whitespace start
			int newNextStart= 0;
			do {
				int origTok= origScanner.getNextToken();
				int newTok= newScanner.getNextToken();
				Assert.isTrue(origTok == newTok);
				
				int origStart= origNextStart;
				int newStart= newNextStart;					
				int origEnd, newEnd;
				if (origTok == ITerminalSymbols.TokenNameEOF) {
					origEnd= string.length();
					newEnd= formatted.length();
					origNextStart= -1; // eof
				} else {
					origEnd= origScanner.getCurrentTokenStartPosition();
					newEnd= newScanner.getCurrentTokenStartPosition();
					origNextStart= origScanner.getCurrentTokenEndPosition() + 1;
					newNextStart= newScanner.getCurrentTokenEndPosition() + 1;
				}
				if (origStart >= offset && origEnd > offset && origStart < end && origEnd <= end) {
					if (isDifferent(string, origStart, origEnd, formatted, newStart, newEnd)) {
						ReplaceEdit edit= new ReplaceEdit(origStart, origEnd - origStart, formatted.substring(newStart, newEnd));
						result.addChild(edit);
					}
				}
				if (TokenScanner.isComment(origTok) && origEnd >= offset && origNextStart <= end) {
					char[] origComment= origScanner.getCurrentTokenSource();
					char[] newComment= newScanner.getCurrentTokenSource();
					if (!CharOperation.equals(origComment, newComment)) {
						commentDifferent(new String(origComment), new String(newComment), origEnd, result);
					}
				}
			} while (origNextStart != -1);
			if (indentationLevel > 0) {
				result.addChild(new InsertEdit(offset, createIndentString(indentationLevel)));
			}
			return result;
		} catch (MalformedTreeException e) {
			JavaPlugin.log(e);
			Assert.isTrue(false, e.getMessage());
		} catch (InvalidInputException e) {
			JavaPlugin.log(e);
			Assert.isTrue(false, e.getMessage());
		}
		return null;
	}
	
	private static void commentDifferent(String origComment, String newComment, int offset, TextEdit result) {
		DefaultLineTracker tracker1= new DefaultLineTracker();
		tracker1.set(origComment);
		DefaultLineTracker tracker2= new DefaultLineTracker();
		tracker2.set(newComment);
		
		ArrayList res= new ArrayList();
		
		int nLines= tracker2.getNumberOfLines();
		if (tracker1.getNumberOfLines() == nLines) {
			try {
				int start1= 0, start2= 0;
				for (int i= 0; i < nLines; i++) {
					IRegion region1= tracker1.getLineInformation(i);
					IRegion region2= tracker2.getLineInformation(i);
					int lineEnd1= region1.getOffset() + region1.getLength();
					int lineEnd2= region2.getOffset() + region2.getLength();
					int sameStart1= region1.getOffset();
					int sameStart2= lineEnd2 - region1.getLength();
					if (isDifferent(origComment, sameStart1, lineEnd1, newComment, sameStart2, lineEnd2)) {
						// should not happen: replace full line
						if (DEBUG) {
							System.out.println("Comment line changed: is: " + origComment.substring(sameStart1, lineEnd1) + ", was: " + newComment.substring(sameStart2, lineEnd2));  //$NON-NLS-1$//$NON-NLS-2$
						}
						res.add(new ReplaceEdit(start1 + offset, lineEnd1 - start1, newComment.substring(start2, lineEnd2)));
					} else {
						if (isDifferent(origComment, start1, sameStart1, newComment, start2, sameStart2)) {
							res.add(new ReplaceEdit(start1 + offset, sameStart1 - start1, newComment.substring(start2, sameStart2)));
						}
					}
					start1= lineEnd1;
					start2= lineEnd2;
				}
			} catch (BadLocationException e) {
				JavaPlugin.log(e);
				res.clear();
			}
		}
		if (res.isEmpty()) {
			// replace all
			result.addChild(new ReplaceEdit(offset, origComment.length(), newComment));
		} else {
			for (int i= 0; i < res.size(); i++) {
				result.addChild((TextEdit) res.get(i));
			}
		}
	}
	
		
	private static IScanner getTokenScanner(String str) {
		IScanner scanner= ToolFactory.createScanner(true, false, false, false);
		scanner.setSource(str.toCharArray());
		scanner.resetTo(0, str.length() - 1);
		return scanner;
	}

	private static boolean isDifferent(String old, int oldStart, int oldEnd, String formatted, int formStart, int formEnd) {
		if (oldEnd - oldStart != formEnd - formStart) {
			return true;
		}
		for (int i= oldStart, j= formStart; i < oldEnd; i++, j++) {
			if (old.charAt(i) != formatted.charAt(j)) {
				return true;
			}
		}
		return false;
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
	
	private static void testSame(String a, String b, int[] p1, int[] p2) {
		if (!a.equals(b)) {
			System.out.println("diff: " + a + ", " + b); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (p1 != null) {
			for (int i= 0; i < p2.length; i++) {
				if (p1[i] != p2[i]) {
					System.out.println("diff: " + p1[i] + ", " + p2[i]); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
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
