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

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextSelection;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.texteditor.ITextEditorExtension2;
import org.eclipse.ui.texteditor.ITextEditorExtension3;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
 

/**
 * Modifies <code>DocumentCommand</code>s inserting semicolons and opening braces to place them
 * smartly, i.e. moving them to the end of a line if that is what the user expects.
 * 
 * <p>In practice,  semicolons and braces (and the caret) are moved to the end of the line if they are typed
 * anywhere except for semicolons in a <code>for</code> statements definition. If the line contains a semicolon
 * or brace after the current caret position, the cursor is moved after it.</p>
 * 
 * @see org.eclipse.jface.text.DocumentCommand
 * @since 3.0
 */
public class SmartSemicolonAutoEditStrategy implements IAutoEditStrategy {

	/** String representation of a semicolon. */
	private static final String SEMICOLON= ";"; //$NON-NLS-1$
	/** Char representation of a semicolon. */
	private static final char SEMICHAR= ';';
	/** String represenattion of a opening brace. */
	private static final String BRACE= "{";  //$NON-NLS-1$
	/** Char representation of a opening brace */
	private static final char BRACECHAR= '{';


	private char fCharacter;
	

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
		
		if (command.text == null)
			return;
			
		if (command.text.equals(SEMICOLON))
			fCharacter= SEMICHAR;
		else if (command.text.equals(BRACE))
			fCharacter= BRACECHAR;
		else
			return;
		
		
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		if (fCharacter == SEMICHAR && !store.getBoolean(PreferenceConstants.EDITOR_SMART_SEMICOLON))
			return;
		if (fCharacter == BRACECHAR && !store.getBoolean(PreferenceConstants.EDITOR_SMART_OPENING_BRACE))
			return;
					
		IWorkbenchPage page= JavaPlugin.getActivePage();
		if (page == null) return;
		IEditorPart part= page.getActiveEditor(); 
		if (!(part instanceof CompilationUnitEditor)) return;
		CompilationUnitEditor editor= (CompilationUnitEditor)part;
		if (editor.getInsertMode() != ITextEditorExtension3.SMART_INSERT || !editor.isEditable()) return;
		ITextEditorExtension2 extension= (ITextEditorExtension2) editor.getAdapter(ITextEditorExtension2.class);
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
		// for now: compute the best position to insert the new character
		// but never position before the current position!
		int position= computeCharacterPosition(document, line, pos, fCharacter);
		if (position < pos)
			return;
		
		// 3: modify command
		command.offset= position;
		// if the character at the insert position is the same, we simply replace it
		// the new caret position is right after the inserted / replaced character
		int positionInLine= position - line.getOffset();
		if (line.getLength() > positionInLine && line.getText().charAt(positionInLine) == fCharacter) {
			command.length= 0;
			command.text= ""; //$NON-NLS-1$
			command.caretOffset= position + 1;
		} else {
			command.length= 0;
			command.caretOffset= position;
			if (fCharacter == BRACECHAR) {
				// TODO check for formatting options
				command.text= " " + BRACE;  //$NON-NLS-1$
			}
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
	private static boolean isForStatement(String line, int offset) {
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
	 * Computes the next position of the given character in the current line.
	 * <p>Algorithm: If the character exists after <code>offset</code>, take it; otherwise,
	 * move to the end of the line, except if we are looking for semicolon and we are in a
	 * <code>for</code> statement, then just stay in place.</p>
	 * 
	 * @param document the document
	 * @param line the line where the change is being made
	 * @param offset the position of the caret when a semicolon was typed
	 * @param character the character to look for
	 * @return the position where the semicolon should be inserted / replaced
	 */
	protected int computeCharacterPosition(IDocument document, ITextSelection line, int startOffset, char character) {
		
		String text= line.getText();
		if (text == null)
			return 0;
		
		int resultOffset= startOffset;
		
		int position= text.indexOf(character, startOffset - line.getOffset());
		if (position == -1) {
			
			if (character != SEMICHAR || !isForStatement(text, startOffset)) {
				
				resultOffset= line.getOffset() + text.length();
				
				try {
					
					ITypedRegion partition= document.getPartition(resultOffset);
					while (!IDocument.DEFAULT_CONTENT_TYPE.equals(partition.getType())) {
							resultOffset= partition.getOffset() -1;
							if (resultOffset <= startOffset)
								return startOffset;
							partition= document.getPartition(resultOffset);
					}
					
					while (resultOffset -1 > startOffset && Character.isWhitespace(document.getChar(resultOffset -1)))
						-- resultOffset;
					
				} catch (BadLocationException e) {
					return startOffset;
				}
			}
			
		} else {
			resultOffset= position + line.getOffset();
		}
		
		return resultOffset;
	}
}
