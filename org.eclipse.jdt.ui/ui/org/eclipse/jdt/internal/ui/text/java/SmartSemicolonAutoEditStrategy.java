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
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.texteditor.ITextEditorExtension2;
 

/**
 * Modifies <code>DocumentCommand</code>s inserting semicolons to place them
 * smartly, i.e. moving them to the end of a line if that is what the user expects.
 * 
 * <p>In practice, the semicolon (and the caret) is moved to the end of the line if it is typed
 * anywhere except for in a <code>for</code> statements definition. If the line contains a semicolon
 * after the current caret position, the cursor is moved after it.</p>
 * 
 * @see org.eclipse.jface.text.DocumentCommand
 */
public class SmartSemicolonAutoEditStrategy implements IAutoEditStrategy {

	/** String representation of a semicolon. */
	private static final String SEMICOLON= ";"; //$NON-NLS-1$
	/** Char representation of a semicolon. */
	private static final char SEMICHAR= ';';

	/**
	 * Creates a new SmartSemicolonAutoEditStrategy.
	 */
	public SmartSemicolonAutoEditStrategy() {
	}

	/*
	 * @see org.eclipse.jface.text.IAutoEditStrategy#customizeDocumentCommand(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.DocumentCommand)
	 */
	public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
		// 0: early pruning
		// also customize if <code>doit</code> is false (so it works in code completion situations)
//		if (!command.doit)
//			return;
		if (command.text == null || !command.text.equals(SEMICOLON))
			return;
		if (!JavaPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SMART_SEMICOLON))
			return;
		IWorkbenchPage page= JavaPlugin.getActivePage();
		if (page == null) return;
		IEditorPart part= page.getActiveEditor(); 
		if (!(part instanceof CompilationUnitEditor)) return;
		CompilationUnitEditor editor= (CompilationUnitEditor)part;
		if (!editor.isSmartTyping() || !editor.isEditable()) return;
		ITextEditorExtension2 extension= (ITextEditorExtension2)editor.getAdapter(ITextEditorExtension2.class);
		if (extension != null && !extension.validateEditorInputState()) return;
		
		// 1: find concerned line / position in java code, location in statement
		// TODO: use AST / Java project tree?
		int pos= command.offset;
		ITextSelection line;
		try {
			IRegion l= document.getLineInformationOfOffset(pos);
			line = new TextSelection(document, l.getOffset(), l.getLength());
		} catch (BadLocationException e) {
			return;
		}

		// 2: choose action based on findings (is for-Statement?)
		// TODO: adjust to use AST / Java project tree?
		// for now: compute the best position to insert the new semicolon
		// but never position before the current position!
		int semiPos= computeSemiPosition(line, pos);
		if (semiPos < pos)
			return;
		
		// 3: modify command
		command.offset= semiPos;
		// if the insert position is at an existing semicolon, we simply replace it
		// the new caret position is right after the inserted / replaced semicolon
		int lineSemiPos= semiPos-line.getOffset();
		if (line.getLength() > lineSemiPos && line.getText().charAt(lineSemiPos) == SEMICHAR) {
			command.length= 0;
			command.text= ""; //$NON-NLS-1$
			command.caretOffset= semiPos + 1;
		} else {
			command.length= 0;
			command.caretOffset= semiPos;
		}
		command.doit= false;
	}
	
	/**
	 * Determines whether the current line contains a for statement.
	 * Algorithm: any "for" word in the line is a positive, "for" contained in a string literal will
	 * produce a false positive 
	 * @param line the line where the change is being made
	 * @param offset the position of the caret
	 * @return <code>true</code> if <code>line</code> contains <code>for</code>, <code>false</code> otherwise
	 */
	protected boolean isForStatement(String line, int offset) {
		/* searching for (^|\s)for(\s|$) */
		int forPos= line.indexOf("for"); //$NON-NLS-1$
		if (forPos != -1) {
			if ((forPos == 0 || !Character.isJavaIdentifierPart(line.charAt(forPos-1)))
				&& (line.length() == forPos+3 || !Character.isJavaIdentifierPart(line.charAt(forPos+3))))
				return true;
		}
		return false;
	}

	/**
	 * Computes the next semicolon position in the current line.
	 * <p>Algorithm: If there is an existing semi after <code>offset</code>, take it; otherwise,
	 * move to the end of the line, except if we are in a <code>for</code> statement, then just
	 * stay in place.</p>
	 * @param line the line where the change is being made
	 * @param offset the position of the caret when a semicolon was typed
	 * @return the position where the semicolon should be inserted / replaced
	 */
	protected int computeSemiPosition(ITextSelection line, int offset) {
		int pos;
		String text= line.getText();
		if (text == null)
			return 0;
		
		// next semi position
		int semiPos= text.indexOf(SEMICHAR, offset-line.getOffset());
		
		if (semiPos == -1) {
			// for statement: stay in place
			if (isForStatement(text, offset)) {
				pos= offset;
			} else {
				// non-WS end of line
				int i;
				for (i= text.length(); i >= 1; i--) {
					if (!Character.isWhitespace(text.charAt(i-1)))
						break;
				}
				pos= line.getOffset() + i;
			}			 
		} else {
			pos= semiPos + line.getOffset();
		}
		return pos;
	}
}
