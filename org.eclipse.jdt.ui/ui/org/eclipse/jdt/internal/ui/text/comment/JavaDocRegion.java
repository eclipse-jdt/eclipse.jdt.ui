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

import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.ConfigurableLineTracker;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.formatter.ContentFormatter2;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;

import org.eclipse.jdt.ui.PreferenceConstants;

/**
 * Javadoc region in a source code document.
 * 
 * @since 3.0
 */
public class JavaDocRegion extends MultiCommentRegion implements IJavaDocTagConstants {

	/** Position category of javadoc code ranges */
	protected static final String CODE_POSITION_CATEGORY= "__javadoc_code_position"; //$NON-NLS-1$

	/** Should html tags be formatted? */
	private final boolean fFormatHtml;

	/** Should source code regions be formatted? */
	private final boolean fFormatSource;

	/** Content type constant used for code snippets withing javadoc comments, value: {@value}. */
	public static final String JAVA_SNIPPET_PARTITION= "__java_doc_snippet"; //$NON-NLS-1$

	/**
	 * Creates a new javadoc region.
	 * 
	 * @param strategy
	 *                  The comment formatting strategy used to format this comment
	 *                  region
	 * @param position
	 *                  The typed position which forms this javadoc region
	 * @param delimiter
	 *                  The line delimiter to use in this javadoc region
	 */
	protected JavaDocRegion(final CommentFormattingStrategy strategy, final TypedPosition position, final String delimiter) {
		super(strategy, position, delimiter);

		final Map preferences= strategy.getPreferences();

		fFormatSource= IPreferenceStore.TRUE.equals(preferences.get(PreferenceConstants.FORMATTER_COMMENT_FORMATSOURCE));
		fFormatHtml= IPreferenceStore.TRUE.equals(preferences.get(PreferenceConstants.FORMATTER_COMMENT_FORMATHTML));
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentRegion#applyRegion(java.lang.String,int)
	 */
	protected void applyRegion(final String indentation, final int width) {

		super.applyRegion(indentation, width);

		if (fFormatSource) {

			final ContentFormatter2 formatter= getStrategy().getFormatter();
			try {

				final IDocument document= getDocument();
				final Position[] positions= document.getPositions(CODE_POSITION_CATEGORY);

				if (positions.length > 0) {

					int begin= 0;
					int end= 0;

					final IFormattingContext context= new CommentFormattingContext();
					context.setProperty(FormattingContextProperties.CONTEXT_DOCUMENT, Boolean.valueOf(false));
					context.setProperty(FormattingContextProperties.CONTEXT_PREFERENCES, getStrategy().getPreferences());

					for (int position= 0; position < positions.length - 1; position++) {

						begin= positions[position++].getOffset();
						end= positions[position].getOffset();

						context.setProperty(FormattingContextProperties.CONTEXT_PARTITION, new TypedPosition(begin, end - begin, JAVA_SNIPPET_PARTITION));
						formatter.format(document, context);
					}
				}
			} catch (BadPositionCategoryException exception) {
				// Should not happen
			}
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentRegion#canApply(org.eclipse.jdt.internal.ui.text.comment.CommentRange,org.eclipse.jdt.internal.ui.text.comment.CommentRange)
	 */
	protected boolean canApply(final CommentRange previous, final CommentRange next) {

		if (previous != null) {

			final boolean isCurrentCode= next.hasAttribute(COMMENT_CODE);
			final boolean isLastCode= previous.hasAttribute(COMMENT_CODE);

			try {

				final int index= getOffset();
				final IDocument document= getDocument();

				if (!isLastCode && isCurrentCode)
					document.addPosition(CODE_POSITION_CATEGORY, new Position(index + next.getOffset() + next.getLength()));
				else if (isLastCode && !isCurrentCode)
					document.addPosition(CODE_POSITION_CATEGORY, new Position(index + previous.getOffset()));

			} catch (BadLocationException exception) {
				// Should not happen
			} catch (BadPositionCategoryException exception) {
				// Should not happen
			}

			if (previous.hasAttribute(COMMENT_IMMUTABLE) && next.hasAttribute(COMMENT_IMMUTABLE) && !isLastCode)
				return false;

		}
		return true;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentRegion#finishRegion(java.lang.String)
	 */
	protected void finalizeRegion(final String indentation) {

		final String test= indentation + MultiCommentLine.MULTI_COMMENT_CONTENT_PREFIX;
		final StringBuffer buffer= new StringBuffer();

		buffer.append(test);
		buffer.append(' ');

		final String delimiter= buffer.toString();
		try {

			final ILineTracker tracker= new ConfigurableLineTracker(new String[] { getDelimiter()});
			tracker.set(getText(0, getLength()));

			int index= 0;
			String content= null;
			IRegion range= null;

			for (int line= tracker.getNumberOfLines() - 3; line >= 1; line--) {

				range= tracker.getLineInformation(line);
				index= range.getOffset();
				content= getText(index, range.getLength());

				if (!content.startsWith(test))
					applyText(delimiter, index, 0);
			}
		} catch (BadLocationException exception) {
			// Should not happen
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentRegion#formatRegion(java.lang.String)
	 */
	public void format(final String indentation) {

		IPositionUpdater updater= null;
		final IDocument document= getDocument();

		if (fFormatSource) {

			document.addPositionCategory(CODE_POSITION_CATEGORY);

			updater= new DefaultPositionUpdater(CODE_POSITION_CATEGORY);
			document.addPositionUpdater(updater);
		}

		super.format(indentation);

		if (fFormatSource) {

			try {
				document.removePositionCategory(CODE_POSITION_CATEGORY);
				document.removePositionUpdater(updater);

			} catch (BadPositionCategoryException exception) {
				// Should not happen
			}
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.MultiCommentRegion#markHtmlRanges()
	 */
	protected final void markHtmlRanges() {

		markTagRanges(JAVADOC_IMMUTABLE_TAGS, COMMENT_IMMUTABLE, true);

		if (fFormatSource)
			markTagRanges(JAVADOC_CODE_TAGS, COMMENT_CODE, false);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.MultiCommentRegion#markHtmlTags(org.eclipse.jdt.internal.ui.text.comment.CommentRange,java.lang.String)
	 */
	protected final void markHtmlTag(final CommentRange range, final String token) {

		if (range.hasAttribute(COMMENT_HTML)) {

			range.markHtmlTag(JAVADOC_IMMUTABLE_TAGS, token, COMMENT_IMMUTABLE, true, true);
			if (fFormatHtml) {

				range.markHtmlTag(JAVADOC_SEPARATOR_TAGS, token, COMMENT_SEPARATOR, true, true);
				range.markHtmlTag(JAVADOC_BREAK_TAGS, token, COMMENT_BREAK, false, true);
				range.markHtmlTag(JAVADOC_SINGLE_BREAK_TAG, token, COMMENT_BREAK, true, false);
				range.markHtmlTag(JAVADOC_NEWLINE_TAGS, token, COMMENT_NEWLINE, true, false);

			} else
				range.markHtmlTag(JAVADOC_CODE_TAGS, token, COMMENT_SEPARATOR, true, true);
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.MultiCommentRegion#markJavadocTag(org.eclipse.jdt.internal.ui.text.comment.CommentRange,java.lang.String)
	 */
	protected final void markJavadocTag(final CommentRange range, final String token) {

		range.markPrefixTag(JAVADOC_PARAM_TAGS, COMMENT_TAG_PREFIX, token, COMMENT_PARAMETER);
		range.markPrefixTag(JAVADOC_ROOT_TAGS, COMMENT_TAG_PREFIX, token, COMMENT_ROOT);
	}

	/**
	 * Marks tag ranges in this javadoc region.
	 * 
	 * @param tags
	 *                  The tags which confine the attributed ranges
	 * @param key
	 *                  The key of the attribute to set when an attributed range has
	 *                  been recognized
	 * @param include
	 *                  <code>true</code> iff end tags in this attributed range
	 *                  should be attributed too, <code>false</code> otherwise
	 */
	protected final void markTagRanges(final String[] tags, final int key, final boolean include) {

		int level= 0;
		int count= 0;
		String token= null;
		CommentRange current= null;

		for (int index= 0; index < tags.length; index++) {

			level= 0;
			for (final Iterator iterator= getRanges().iterator(); iterator.hasNext();) {

				current= (CommentRange)iterator.next();
				count= current.getLength();

				if (count > 0) { // PR44035: when inside a tag, mark blank lines as well to get proper snippet formatting

					token= getText(current.getOffset(), current.getLength());
					level= current.markRange(token, tags[index], level, key, include);
				}
			}
		}
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.MultiCommentRegion#canAppend(org.eclipse.jdt.internal.ui.text.comment.CommentLine, org.eclipse.jdt.internal.ui.text.comment.CommentRange, org.eclipse.jdt.internal.ui.text.comment.CommentRange, int, int)
	 */
	protected boolean canAppend(CommentLine line, CommentRange previous, CommentRange next, int position, int count) {
		// don't append code sections
		if (next.hasAttribute(COMMENT_CODE | COMMENT_FIRST_TOKEN) && line.getSize() != 0)
			return false;
		return super.canAppend(line, previous, next, position, count);
	}
}