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
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.Preferences;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Position;

import org.eclipse.jdt.core.CodeFormatter;
import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.ui.JavaPlugin;



public class CodeFormatterUtil {
	
	public static boolean OLD_FORMATTER= true;
	public static boolean OLD_FUNC= true;
	public static boolean DEBUG= false;
	
	private static boolean BUG_43437= true;
	
	public static final int K_UNKNOWN= -1;
	public static final int K_EXPRESSION = CodeFormatter.K_EXPRESSION;
	public static final int K_STATEMENTS = CodeFormatter.K_STATEMENTS;
	public static final int K_CLASS_BODY_DECLARATIONS = CodeFormatter.K_CLASS_BODY_DECLARATIONS;
	public static final int K_COMPILATION_UNIT = CodeFormatter.K_COMPILATION_UNIT;
		 
	private static final String POS_CATEGORY= "myCategory"; //$NON-NLS-1$
	
	
	/**
	 * Creates a string that represents the given number of indents (can be spaces or tabs..)
	 */
	public static String createIndentString(int indent) {
		if (OLD_FORMATTER) {
			return old_formatter("", indent, null, "", null);  //$NON-NLS-1$//$NON-NLS-2$
		} else {
			String str= format(K_EXPRESSION, "x", indent, null, "", null); //$NON-NLS-1$ //$NON-NLS-2$
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
	 * @deprecated Port to <code>format2</code>
	 */
	public static String format(int kind, String string, int indentationLevel, int[] positions, String lineSeparator, Map options) {
		return format(kind, string, 0, string.length(), indentationLevel, positions, lineSeparator, options);
	}
	
	
	/**
	 * @deprecated Port to <code>format2</code>
	 */	
	public static String format(int kind, String string, int start, int end, int indentationLevel, int[] positions, String lineSeparator, Map options) {
		if (OLD_FUNC) {
			return old_formatter(string, indentationLevel, positions, lineSeparator, options);
		}
		
		int offset= start;
		int length= end - start;
		
		try {
			// new edit api
			
			Document doc= createDocument(string);

			Position[] stored= initPositions(doc, positions);
			TextEdit edit= format2(kind, string, offset, length, indentationLevel, lineSeparator, options);
			if (edit != null) {
				edit.apply(doc);
				readPositions(stored, positions);
				return doc.get();
			}
			JavaPlugin.logErrorMessage("format returns null-edit"); //$NON-NLS-1$
		} catch (MalformedTreeException e) {
			JavaPlugin.log(e);
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		} catch (BadPositionCategoryException e) {
			JavaPlugin.log(e);
		}
		return string;
	}
	
	/**
	 * @deprecated Port to <code>format2</code>
	 */	
	public static String format(ASTNode node, String string, int indentationLevel, int[] positions, String lineSeparator, Map options) {
		if (OLD_FUNC) {
			return old_formatter(string, indentationLevel, positions, lineSeparator, options);
		}
		
		// emulate the old functionality with edits
		try {		
			Document doc= createDocument(string);
			Position[] stored= initPositions(doc, positions);
			
			TextEdit edit= format2(node, string, indentationLevel, lineSeparator, options);
			if (edit != null) {
				edit.apply(doc);
				
				int[] positionCopy= null;
				if (DEBUG && positions != null) {
					positionCopy= (int[]) positions.clone();
				}
				
				readPositions(stored, positions);
				String res= doc.get();
				
				if (DEBUG) {
					String oldFormat= old_formatter(string, indentationLevel, positionCopy, lineSeparator, options);
					testSame(res, oldFormat, positions, positionCopy);
				}
				return res;
			}
			JavaPlugin.logErrorMessage("format returns null-edit"); //$NON-NLS-1$
		} catch (MalformedTreeException e) {
			JavaPlugin.log(e);
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		} catch (BadPositionCategoryException e) {
			JavaPlugin.log(e);
		}
		return string;
	}
	

	
	
	public static TextEdit format2(int kind, String string, int offset, int length, int indentationLevel, String lineSeparator, Map options) {
		if (OLD_FORMATTER) {
			return emulateNewWithOld(string, offset, length, indentationLevel, lineSeparator, options);
		} else {
			// TODO: new API
		}
		return null;
		
	}
	
	public static TextEdit format2(int kind, String string, int indentationLevel, String lineSeparator, Map options) {
		return format2(kind, string, 0, string.length(), indentationLevel, lineSeparator, options);
	}
	
	/**
	 * Format the source whose kind is described by the passed AST node. This AST node is only used for type tests. It can be
	 * a dummy node with no content.
	 */
	public static TextEdit format2(ASTNode node, String str, int indentationLevel, String lineSeparator, Map options) {		

		int code;
		String prefix= ""; //$NON-NLS-1$
		String suffix= ""; //$NON-NLS-1$
		if (node instanceof CompilationUnit) {
			code= CodeFormatterUtil.K_COMPILATION_UNIT;
		} else if (node instanceof BodyDeclaration) {
			code= CodeFormatterUtil.K_CLASS_BODY_DECLARATIONS;
		} else if (node instanceof SwitchCase) {
			prefix = "switch(1) {"; //$NON-NLS-1$
			suffix = "}"; //$NON-NLS-1$
			code= CodeFormatterUtil.K_STATEMENTS;
		} else if (node instanceof Statement) {
			code= CodeFormatterUtil.K_STATEMENTS;
		} else if (node instanceof VariableDeclarationExpression) {
			code = CodeFormatterUtil.K_STATEMENTS;
			suffix = ";"; //$NON-NLS-1$
		} else if (node instanceof Expression) {
			code= CodeFormatterUtil.K_EXPRESSION;
			if (BUG_43437 && node instanceof StringLiteral) {
				return new InsertEdit(0, createIndentString(indentationLevel));
			}		
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
			prefix= "new A()"; //$NON-NLS-1$
			code= CodeFormatterUtil.K_STATEMENTS;
			suffix= ";"; //$NON-NLS-1$
		} else {
			Assert.isTrue(false, "Node type not covered: " + node.getClass().getName()); //$NON-NLS-1$
			return null;
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
	
	private static IScanner getTokenScanner(String str) {
		IScanner scanner= ToolFactory.createScanner(true, false, false, false);
		scanner.setSource(str.toCharArray());
		scanner.resetTo(0, str.length());
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
	
	private static Document createDocument(String string) {
		Document doc= new Document(string);
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
		return doc;
	}

	private static Position[] initPositions(Document doc, int[] positions) throws BadLocationException, BadPositionCategoryException {
		if (positions != null) {
			Position[] stored= new Position[positions.length];
			for (int i= 0; i < positions.length; i++) {
				stored[i]= new Position(positions[i], 0);
				doc.addPosition(POS_CATEGORY, stored[i]);
			}
			return stored;
		}
		return null;
	}

	private static void readPositions(Position[] stored, int[] positions) {
		if (positions != null) {
			for (int i= 0; i < positions.length; i++) {
				Position curr= stored[i];
				Assert.isTrue(!curr.isDeleted);
				positions[i]= curr.getOffset();
			}
		}
	}
	
}
