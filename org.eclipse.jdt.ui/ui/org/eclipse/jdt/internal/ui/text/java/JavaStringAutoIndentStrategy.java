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

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultAutoIndentStrategy;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.texteditor.ITextEditorExtension3;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.text.SmartBackspaceManager;
import org.eclipse.jdt.internal.ui.text.SmartBackspaceManager.UndoSpec;

/**
 * Auto indent strategy for java strings
 */
public class JavaStringAutoIndentStrategy extends DefaultAutoIndentStrategy {
	
	private String fPartitioning;
	
	/**
	 * The input string doesn't contain any line delimiter.
	 * 
	 * @param inputString the given input string
	 * @return the displayable string.
	 */
	private String displayString(String inputString, String indentation, String delimiter) {
		
		int length = inputString.length();
		StringBuffer buffer = new StringBuffer(length);
		java.util.StringTokenizer tokenizer = new java.util.StringTokenizer(inputString, "\n\r", true); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()){
	
			String token = tokenizer.nextToken();
			if (token.equals("\r")) { //$NON-NLS-1$
				buffer.append("\\r"); //$NON-NLS-1$
				if (tokenizer.hasMoreTokens()) {
					token = tokenizer.nextToken();
					if (token.equals("\n")) { //$NON-NLS-1$
						buffer.append("\\n"); //$NON-NLS-1$
						buffer.append("\" + " + delimiter); //$NON-NLS-1$
						buffer.append(indentation);
						buffer.append("\""); //$NON-NLS-1$
						continue;
					} else {
						buffer.append("\" + " + delimiter); //$NON-NLS-1$
						buffer.append(indentation);
						buffer.append("\""); //$NON-NLS-1$
					}
				} else {
					continue;
				}
			} else if (token.equals("\n")) { //$NON-NLS-1$
				buffer.append("\\n"); //$NON-NLS-1$
				buffer.append("\" + " + delimiter); //$NON-NLS-1$
				buffer.append(indentation);
				buffer.append("\""); //$NON-NLS-1$
				continue;
			}	
	
			StringBuffer tokenBuffer = new StringBuffer();
			for (int i = 0; i < token.length(); i++){ 
				char c = token.charAt(i);
				switch (c) {
					case '\r' :
						tokenBuffer.append("\\r"); //$NON-NLS-1$
						break;
					case '\n' :
						tokenBuffer.append("\\n"); //$NON-NLS-1$
						break;
					case '\b' :
						tokenBuffer.append("\\b"); //$NON-NLS-1$
						break;
					case '\t' :
						// keep tabs verbatim
						tokenBuffer.append("\t"); //$NON-NLS-1$
						break;
					case '\f' :
						tokenBuffer.append("\\f"); //$NON-NLS-1$
						break;
					case '\"' :
						tokenBuffer.append("\\\""); //$NON-NLS-1$
						break;
					case '\'' :
						tokenBuffer.append("\\'"); //$NON-NLS-1$
						break;
					case '\\' :
						tokenBuffer.append("\\\\"); //$NON-NLS-1$
						break;
					default :
						tokenBuffer.append(c);
				}
			}
			buffer.append(tokenBuffer);
		}
		return buffer.toString();
	}

	/**
	 * Creates a new Java string auto indent strategy for the given document partitioning.
	 * 
	 * @param partitioning the document partitioning
	 */
	public JavaStringAutoIndentStrategy(String partitioning) {
		super();
		fPartitioning= partitioning;
	}
	
	private boolean isLineDelimiter(IDocument document, String text) {
		String[] delimiters= document.getLegalLineDelimiters();
		if (delimiters != null)
			return TextUtilities.equals(delimiters, text) > -1;
		return false;
	}
	
	private String getLineIndentation(IDocument document, int offset) throws BadLocationException {

		// find start of line
		int adjustedOffset= (offset == document.getLength() ? offset  - 1 : offset);
		IRegion line= document.getLineInformationOfOffset(adjustedOffset);
		int start= line.getOffset();
					
		// find white spaces
		int end= findEndOfWhiteSpace(document, start, offset);
		
		return document.get(start, end - start);
	}

	private String getModifiedText(String string, String indentation, String delimiter) {		
		return displayString(string, indentation, delimiter);
	}

	private void javaStringIndentAfterNewLine(IDocument document, DocumentCommand command) throws BadLocationException {

		ITypedRegion partition= TextUtilities.getPartition(document, fPartitioning, command.offset, true);
		int offset= partition.getOffset();
		int length= partition.getLength();

		if (command.offset == offset + length && document.getChar(offset + length - 1) == '\"')
			return;

		String indentation= getLineIndentation(document, command.offset);
		String delimiter= TextUtilities.getDefaultLineDelimiter(document);

		IRegion line= document.getLineInformationOfOffset(offset);
		String string= document.get(line.getOffset(), offset - line.getOffset());
		if (string.trim().length() != 0)
			indentation += String.valueOf("\t\t"); //$NON-NLS-1$
		
		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		if (isLineDelimiter(document, command.text))
			command.text= "\" +" + command.text + indentation + "\"";  //$NON-NLS-1$//$NON-NLS-2$
		else if (command.text.length() > 1 && preferenceStore.getBoolean(PreferenceConstants.EDITOR_ESCAPE_STRINGS))
			command.text= getModifiedText(command.text, indentation, delimiter);		
	}
	
	private boolean isSmartMode() {
		IWorkbenchPage page= JavaPlugin.getActivePage();
		if (page != null)  {
			IEditorPart part= page.getActiveEditor(); 
			if (part instanceof ITextEditorExtension3) {
				ITextEditorExtension3 extension= (ITextEditorExtension3) part;
				return extension.getInsertMode() == ITextEditorExtension3.SMART_INSERT;
			}
		}
		return false;
	}

	/*
	 * @see org.eclipse.jface.text.IAutoIndentStrategy#customizeDocumentCommand(IDocument, DocumentCommand)
	 */
	public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
		try {
			if (command.length != 0 || command.text == null)
				return;

			IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
				
			if (preferenceStore.getBoolean(PreferenceConstants.EDITOR_WRAP_STRINGS) && isSmartMode()) {
				int origOffset= command.offset;
				int origLength= command.length;
				String origText= command.text == null ? new String() : command.text;
				javaStringIndentAfterNewLine(document, command);
				installSmartBackspace(document, command, origOffset, origLength, origText);
			}
				
		} catch (BadLocationException e) {
		}
	}
	
	/**
	 * Installs a smart backspace handler to undo the smart modification
	 * 
	 * @param d the document
	 * @param c the modified document command
	 * @param origOffset original offset of the command
	 * @param origLength original length of the command
	 * @param origText original text of the command
	 */
	private void installSmartBackspace(IDocument d, DocumentCommand c, int origOffset, int origLength, String origText) {
		if (!JavaPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SMART_BACKSPACE))
			return;
		
		if (origText == null)
			origText= new String();

		int newOffset= c.offset;
		int newLength= c.length;
		String newText= c.text;
		
		if (newLength != origLength || newOffset != origOffset || !origText.equals(newText)) {
			
			IWorkbenchPage page= JavaPlugin.getActivePage();
			if (page == null)
				return;
			IEditorPart part= page.getActiveEditor();
			if (!(part instanceof CompilationUnitEditor))
				return;
			CompilationUnitEditor editor= (CompilationUnitEditor)part;
			
			try {
				final SmartBackspaceManager manager= (SmartBackspaceManager) editor.getAdapter(SmartBackspaceManager.class);
				if (manager != null) {
					String deletedText= d.get(newOffset, newLength);
					// restore smart portion
					ReplaceEdit smart= new ReplaceEdit(newOffset, newText.length(), deletedText);
					// restore raw text
					ReplaceEdit raw= new ReplaceEdit(origOffset, origLength, origText);
					
					final UndoSpec s2= new UndoSpec(
							c.caretOffset != -1 ? c.caretOffset : newOffset + newText.length(),
							new Region(origOffset + origText.length(), 0),
							new TextEdit[] { smart, raw },
							3,
							null);
					manager.register(s2);
				}
			} catch (BadLocationException e) {
				JavaPlugin.log(e);
			} catch (MalformedTreeException e) {
				JavaPlugin.log(e);
			}
		}
	}
}
