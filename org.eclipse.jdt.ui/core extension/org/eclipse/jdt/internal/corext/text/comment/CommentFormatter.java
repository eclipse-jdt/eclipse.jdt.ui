/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.corext.text.comment;


import java.util.Map;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TypedPosition;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.formatter.CodeFormatter;


/**
 * A comment formatter.
 * 
 * @since 3.1
 */
public class CommentFormatter extends CodeFormatter {

	/** Text measurement */
	private ITextMeasurement fTextMeasurement;

	/** Preferences */
	private Map fPreferences;

	/** Last formatted document's hash-code. */
	private int fLastDocumentHash;
	
	/** Last formatted document header's hash-code. */
	private int fLastHeaderHash;
	
	/** End of the first class or interface token in the last document. */
	private int fLastMainTokenEnd= -1;
	
	/** End of the header in the last document. */
	private int fLastDocumentsHeaderEnd;

	/**
	 * Initialize the comment formatter with the given text measurement and preferences.
	 * 
	 * @param textMeasurement the text measurement
	 * @param preferences the preferences
	 */
	public CommentFormatter(ITextMeasurement textMeasurement, Map preferences) {
		fTextMeasurement= textMeasurement;
		fPreferences= preferences;
	}

	/*
	 * @see org.eclipse.jdt.core.formatter.CodeFormatter#format(int, java.lang.String, int, int, int, java.lang.String)
	 */
	public TextEdit format(int kind, String source, int offset, int length, int indentationLevel, String lineSeparator) {
		String type= JavaPartitions.getPartitionType(kind);
		if (type != null)
			return format(new Document(source), new TypedPosition(offset, length, type), indentationLevel, lineSeparator);
		return null;
	}

	/**
	 * Compute a text edit for formatting the given partition in the given
	 * document with the given indentation level and line delimiter.
	 * 
	 * @param document the document
	 * @param partition the partition
	 * @param indentationLevel the indentation level, {@link CommentRegion#INFER_INDENTATION} for inferring it automatically from the context
	 * @param lineDelimiter the line delimiter
	 * @return the text edit for formatting
	 * @since 3.1
	 */
	private TextEdit format(IDocument document, TypedPosition partition, int indentationLevel, String lineDelimiter) {
		String trueProperty= Boolean.toString(true);
		final boolean isFormattingComments= trueProperty.equals(fPreferences.get(CommentFormatterPreferenceConstants.FORMATTER_COMMENT_FORMAT));
		final boolean isFormattingHeader= trueProperty.equals(fPreferences.get(CommentFormatterPreferenceConstants.FORMATTER_COMMENT_FORMATHEADER));
		
		int documentsHeaderEnd= computeHeaderEnd(document);
		
		TextEdit edit= null;
		if (isFormattingComments && (isFormattingHeader || partition.offset >= documentsHeaderEnd)) {
			
			final CommentRegion region= CommentObjectFactory.createRegion(document, partition, lineDelimiter, fPreferences, fTextMeasurement);
			edit= region.format(indentationLevel);
		}
		return edit;
	}

	/**
	 * Returns the end offset for the document's header.
	 * 
	 * @param document the document
	 * @return the header's end offset
	 */
	private int computeHeaderEnd(IDocument document) {
		if (document == null)
			return -1;
		
		try {
			if (fLastMainTokenEnd >= 0 && document.hashCode() == fLastDocumentHash && fLastMainTokenEnd < document.getLength() && document.get(0, fLastMainTokenEnd).hashCode() == fLastHeaderHash)
				return fLastDocumentsHeaderEnd;
		} catch (BadLocationException e) {
			// should not happen -> recompute
		}
		
		IScanner scanner= ToolFactory.createScanner(true, false, false, false);
		scanner.setSource(document.get().toCharArray());

		try {
			int offset= -1;
			boolean foundComment= false;
			int terminal= scanner.getNextToken();
			while (terminal != ITerminalSymbols.TokenNameEOF && !(terminal == ITerminalSymbols.TokenNameclass || terminal == ITerminalSymbols.TokenNameinterface || (foundComment && (terminal == ITerminalSymbols.TokenNameimport || terminal == ITerminalSymbols.TokenNamepackage)))) {
				
				if (terminal == ITerminalSymbols.TokenNameCOMMENT_JAVADOC)
					offset= scanner.getCurrentTokenStartPosition();
				
				foundComment= terminal == ITerminalSymbols.TokenNameCOMMENT_JAVADOC || terminal == ITerminalSymbols.TokenNameCOMMENT_BLOCK;
				
				terminal= scanner.getNextToken();
			}
			
			int mainTokenEnd= scanner.getCurrentTokenEndPosition();
			if (terminal != ITerminalSymbols.TokenNameEOF) {
				mainTokenEnd++;
				if (offset == -1 || (foundComment && (terminal == ITerminalSymbols.TokenNameimport || terminal == ITerminalSymbols.TokenNamepackage)))
					offset= scanner.getCurrentTokenStartPosition();
			} else
				offset= -1;
			
			try {
				fLastHeaderHash= document.get(0, mainTokenEnd).hashCode();
			} catch (BadLocationException e) {
				// should not happen -> recompute next time
				mainTokenEnd= -1;
			}
			
			fLastDocumentHash= document.hashCode();
			fLastMainTokenEnd= mainTokenEnd;
			fLastDocumentsHeaderEnd= offset;
			return offset;
			
		} catch (InvalidInputException ex) {
			// enable formatting
			return -1;
		}
	}
}
