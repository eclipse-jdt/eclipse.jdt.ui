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

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;

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
		return format(kind, new Document(source), new Position(offset, length), indentationLevel, lineSeparator);
	}

	/**
	 * Compute a text edit for formatting the given partition in the given
	 * document with the given indentation level and line delimiter.
	 * 
	 * @param kind the comment snippet kind
	 * @param document the document
	 * @param position the position
	 * @param indentationLevel the indentation level
	 * @param lineDelimiter the line delimiter
	 * @return the text edit for formatting
	 * @since 3.1
	 */
	private TextEdit format(int kind, IDocument document, Position position, int indentationLevel, String lineDelimiter) {
		final boolean isFormattingComments= Boolean.toString(true).equals(fPreferences.get(CommentFormatterPreferenceConstants.FORMATTER_COMMENT_FORMAT));
		TextEdit edit= null;
		if (isFormattingComments) {
			final CommentRegion region= createRegion(kind, document, position, lineDelimiter, fPreferences, fTextMeasurement);
			if (region != null)
				edit= region.format(indentationLevel);
		}
		return edit;
	}

	/**
	 * Creates a comment region for a specific document partition type.
	 * 
	 * @param kind the comment snippet kind
	 * @param document the document which contains the comment region
	 * @param range range of the comment region in the document
	 * @param delimiter line delimiter to use in the comment region
	 * @param preferences the preferences to use
	 * @param textMeasurement the text measurement. Can be <code>null</code>.
	 * @return a new comment region for the comment region range in the
	 *         document
	 * @since 3.1
	 */
	private static CommentRegion createRegion(int kind, IDocument document, Position range, String delimiter, Map preferences, ITextMeasurement textMeasurement) {
		switch (kind) {
			case CommentFormatterConstants.K_SINGLE_LINE_COMMENT:
				return new CommentRegion(document, range, delimiter, preferences, textMeasurement);
			case CommentFormatterConstants.K_MULTI_LINE_COMMENT:
				return new MultiCommentRegion(document, range, delimiter, preferences, textMeasurement);
			case CommentFormatterConstants.K_JAVA_DOC:
				return new JavaDocRegion(document, range, delimiter, preferences, textMeasurement);
			default:
				return null;
		}
	}
}
