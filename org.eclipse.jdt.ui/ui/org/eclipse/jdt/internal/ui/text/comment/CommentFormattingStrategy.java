/*****************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package org.eclipse.jdt.internal.ui.text.comment;

import java.util.LinkedList;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.IJavaPartitions;

/**
 * Formatting strategy for general source code comments.
 * 
 * @since 3.0
 */
public class CommentFormattingStrategy extends ContextBasedFormattingStrategy {

	/**
	 * Returns the indentation of the line at the specified offset.
	 * 
	 * @param document
	 *                  Document which owns the line
	 * @param region
	 *                  Comment region which owns the line
	 * @param offset
	 *                   Offset where to determine the indentation
	 * @param tabs
	 *                   <code>true</code> iff the indentation should use tabs
	 *                   instead of spaces, <code>false</code> otherwise
	 * @return The indentation of the line
	 */
	public static String getLineIndentation(final IDocument document, final CommentRegion region, final int offset, final boolean tabs) {

		String result= ""; //$NON-NLS-1$

		try {

			final IRegion line= document.getLineInformationOfOffset(offset);

			final int begin= line.getOffset();
			final int end= Math.min(offset, line.getOffset() + line.getLength());

			result= region.stringToIndent(document.get(begin, end - begin), tabs);

		} catch (BadLocationException exception) {
			// Ignore and return empty
		}
		return result;
	}

	/** Documents to be formatted by this strategy */
	private final LinkedList fDocuments= new LinkedList();

	/** Partitions to be formatted by this strategy */
	private final LinkedList fPartitions= new LinkedList();

	/** Text measurement, can be <code>null</code> */
	private ITextMeasurement fTextMeasurement;

	/**
	 * Creates a new comment formatting strategy.
	 */
	public CommentFormattingStrategy() {
		super();
	}

	/**
	 * Creates a new comment formatting strategy.
	 * 
	 * @param textMeasurement
	 *                  The text measurement
	 */
	public CommentFormattingStrategy(ITextMeasurement textMeasurement) {
		super();
		fTextMeasurement= textMeasurement;
	}

	/**
	 * @inheritDoc
	 */
	public void format() {
		super.format();
		
		final IDocument document= (IDocument) fDocuments.removeFirst();
		final TypedPosition position= (TypedPosition)fPartitions.removeFirst();
		
		if (document != null && position != null) {
			final boolean format= getPreferences().get(PreferenceConstants.FORMATTER_COMMENT_FORMAT).equals(IPreferenceStore.TRUE);
			final boolean header= getPreferences().get(PreferenceConstants.FORMATTER_COMMENT_FORMATHEADER).equals(IPreferenceStore.TRUE);
			final boolean tabs= getPreferences().get(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR).equals(JavaCore.TAB);
			
			final int offset= position.getOffset();
			if (format && (header || offset != 0 || !position.getType().equals(IJavaPartitions.JAVA_DOC))) {
				
				final CommentRegion region= CommentObjectFactory.createRegion(document, position, TextUtilities.getDefaultLineDelimiter(document), getPreferences(), fTextMeasurement);
				final TextEdit edit= region.format(getLineIndentation(document, region, offset, tabs));
				try {
					
					if (edit != null)
						edit.apply(document);
					
				} catch (MalformedTreeException exception) {
					JavaPlugin.log(exception);
				} catch (BadLocationException exception) {
					JavaPlugin.log(exception);
				}
			}
		}
	}

	/**
	 * @inheritDoc
	 */
	public void formatterStarts(IFormattingContext context) {
		super.formatterStarts(context);

		fPartitions.addLast(context.getProperty(FormattingContextProperties.CONTEXT_PARTITION));
		fDocuments.addLast(context.getProperty(FormattingContextProperties.CONTEXT_MEDIUM));
	}

	/**
	 * @inheritDoc
	 */
	public void formatterStops() {
		super.formatterStops();

		fPartitions.clear();
		fDocuments.clear();
	}
}