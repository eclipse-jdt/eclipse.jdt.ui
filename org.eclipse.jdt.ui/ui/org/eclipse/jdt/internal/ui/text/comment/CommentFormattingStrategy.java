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
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.formatter.ContentFormatter;
import org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy;
import org.eclipse.jface.text.formatter.FormattingContext;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.text.IJavaPartitions;

/**
 * Formatting strategy for general source code comments.
 * <p>
 * This strategy implements <code>IFormattingStrategyExtension</code>. It
 * must be registered with a content formatter implementing <code>IContentFormatterExtension2<code>
 * to take effect.
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
	 *                  Offset where to determine the indentation
	 * @return The indentation of the line
	 */
	public static String getLineIndentation(final IDocument document, final CommentRegion region, final int offset) {

		String result= ""; //$NON-NLS-1$

		try {

			final IRegion line= document.getLineInformationOfOffset(offset);

			final int begin= line.getOffset();
			final int end= Math.min(offset, line.getOffset() + line.getLength());

			result= region.stringToIndent(document.get(begin, end - begin), true);

		} catch (BadLocationException exception) {
			// Should not happen
		}
		return result;
	}

	/**
	 * Content formatter with which this formatting strategy has been
	 * registered
	 */
	private final ContentFormatter fFormatter;

	/** Partitions to be formatted by this strategy */
	private final LinkedList fPartitions= new LinkedList();

	/**
	 * Creates a new comment formatting strategy.
	 * 
	 * @param formatter
	 *                  The content formatter with which this formatting strategy has
	 *                  been registered
	 * @param viewer
	 *                  The source viewer where to apply the formatting strategy
	 */
	public CommentFormattingStrategy(final ContentFormatter formatter, final ISourceViewer viewer) {
		super(viewer);

		fFormatter= formatter;
	}

	/*
	 * @see org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy#format()
	 */
	public void format() {
		super.format();

		Assert.isLegal(fPartitions.size() > 0);

		final IDocument document= getViewer().getDocument();
		final TypedPosition position= (TypedPosition)fPartitions.removeFirst();

		try {

			final ITypedRegion partition= TextUtilities.getPartition(document, fFormatter.getDocumentPartitioning(), position.getOffset());
			final String type= partition.getType();

			position.offset= partition.getOffset();
			position.length= partition.getLength();

			final Map preferences= getPreferences();
			final boolean format= preferences.get(PreferenceConstants.FORMATTER_COMMENT_FORMAT) == IPreferenceStore.TRUE;
			final boolean header= preferences.get(PreferenceConstants.FORMATTER_COMMENT_FORMATHEADER) == IPreferenceStore.TRUE;

			if (format && (header || position.getOffset() != 0 || !type.equals(IJavaPartitions.JAVA_DOC))) {

				final CommentRegion region= CommentObjectFactory.createRegion(this, position, TextUtilities.getDefaultLineDelimiter(document));
				final String indentation= getLineIndentation(document, region, position.getOffset());

				final Map partitioners= TextUtilities.removeDocumentPartitioners(document);
				region.format(indentation);
				TextUtilities.addDocumentPartitioners(document, partitioners);
			}
		} catch (BadLocationException exception) {
			// Should not happen
		}
	}

	/*
	 * @see org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy#formatterStarts(org.eclipse.jface.text.formatter.IFormattingContext)
	 */
	public void formatterStarts(IFormattingContext context) {
		super.formatterStarts(context);

		final FormattingContext current= (FormattingContext)context;

		fPartitions.addLast(current.getProperty(FormattingContextProperties.CONTEXT_PARTITION));
	}

	/*
	 * @see org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy#formatterStops()
	 */
	public void formatterStops() {
		super.formatterStops();

		fPartitions.clear();
	}

	/**
	 * Returns the content formatter with which this formatting strategy has
	 * been registered.
	 * 
	 * @return The content formatter
	 */
	public final ContentFormatter getFormatter() {
		return fFormatter;
	}
}
