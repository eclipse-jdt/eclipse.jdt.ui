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

import java.util.ListIterator;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.TypedPosition;

import org.eclipse.jdt.ui.PreferenceConstants;

/**
 * Multi-comment region in a source code document.
 * 
 * @since 3.0
 */
public class MultiCommentRegion extends CommentRegion implements ICommentTagConstants {

	/** Should root tag parameter descriptions be indented after the tag? */
	private final boolean fIndentDescriptions;

	/** Should root tag parameter descriptions be indented? */
	private final boolean fIndentRoots;

	/** Should description of parameters go to the next line? */
	private final boolean fParameterNewLine;

	/** Should root tags be separated from description? */
	private boolean fSeparateRoots;

	/**
	 * Creates a new multi-comment region.
	 * 
	 * @param strategy
	 *                  The comment formatting strategy used to format this comment
	 *                  region
	 * @param position
	 *                  The typed position which forms this comment region
	 * @param delimiter
	 *                  The line delimiter to use in this comment region
	 */
	protected MultiCommentRegion(final CommentFormattingStrategy strategy, final TypedPosition position, final String delimiter) {
		super(strategy, position, delimiter);

		final Map preferences= strategy.getPreferences();

		fIndentRoots= IPreferenceStore.TRUE.equals(preferences.get(PreferenceConstants.FORMATTER_COMMENT_INDENTROOTTAGS));
		fIndentDescriptions= IPreferenceStore.TRUE.equals(preferences.get(PreferenceConstants.FORMATTER_COMMENT_INDENTPARAMETERDESCRIPTION));
		fSeparateRoots= IPreferenceStore.TRUE.equals(preferences.get(PreferenceConstants.FORMATTER_COMMENT_SEPARATEROOTTAGS));
		fParameterNewLine= IPreferenceStore.TRUE.equals(preferences.get(PreferenceConstants.FORMATTER_COMMENT_NEWLINEFORPARAMETER));
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentRegion#canAppend(org.eclipse.jdt.internal.ui.text.comment.CommentLine,org.eclipse.jdt.internal.ui.text.comment.CommentRange, org.eclipse.jdt.internal.ui.text.comment.CommentRange, int, int)
	 */
	protected boolean canAppend(final CommentLine line, final CommentRange previous, final CommentRange next, final int position, int count) {

		final boolean blank= next.hasAttribute(COMMENT_BLANKLINE);

		// TODO does not work for TestCase.java: <br>1) is not wrapped
		if (next.getLength() <= 2 && !blank && !isCommentWord(next))
			return true;

		if (fParameterNewLine && line.hasAttribute(COMMENT_PARAMETER) && line.getSize() > 1)
			return false;

		if (previous != null) {

			if (previous.hasAttribute(COMMENT_ROOT))
				return true;

			if (position != 0 && (blank || previous.hasAttribute(COMMENT_BLANKLINE) || next.hasAttribute(COMMENT_PARAMETER) || next.hasAttribute(COMMENT_ROOT) || next.hasAttribute(COMMENT_SEPARATOR) || next.hasAttribute(COMMENT_NEWLINE) || previous.hasAttribute(COMMENT_BREAK) || previous.hasAttribute(COMMENT_SEPARATOR)))
				return false;

			if (next.hasAttribute(COMMENT_IMMUTABLE) && previous.hasAttribute(COMMENT_IMMUTABLE))
				return true;
		}

		if (fIndentRoots && !line.hasAttribute(COMMENT_ROOT) && !line.hasAttribute(COMMENT_PARAMETER))
			count -= stringToLength(line.getIndentation());

		return super.canAppend(line, previous, next, position, count);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentRegion#getDelimiter(org.eclipse.jdt.internal.ui.text.comment.CommentLine, org.eclipse.jdt.internal.ui.text.comment.CommentLine, org.eclipse.jdt.internal.ui.text.comment.CommentRange, org.eclipse.jdt.internal.ui.text.comment.CommentRange, java.lang.String)
	 */
	protected String getDelimiter(CommentLine predecessor, CommentLine successor, CommentRange previous, CommentRange next, String indentation) {

		final String delimiter= super.getDelimiter(predecessor, successor, previous, next, indentation);

		if (previous != null) {

			if (previous.hasAttribute(COMMENT_IMMUTABLE | COMMENT_SEPARATOR) && !next.hasAttribute(COMMENT_CODE) && !successor.hasAttribute(COMMENT_BLANKLINE))
				return delimiter + delimiter;

			else if (previous.hasAttribute(COMMENT_CODE) && !next.hasAttribute(COMMENT_CODE))
				return getDelimiter();
			
			// remove any asterix borders inside code sections
			else if (previous.hasAttribute(COMMENT_CODE) && next.hasAttribute(COMMENT_CODE))
				return getDelimiter();

			else if ((next.hasAttribute(COMMENT_IMMUTABLE | COMMENT_SEPARATOR) || ((fSeparateRoots || !isClearLines()) && previous.hasAttribute(COMMENT_PARAGRAPH))) && !successor.hasAttribute(COMMENT_BLANKLINE))
				return delimiter + delimiter;

			else if (fIndentRoots && !predecessor.hasAttribute(COMMENT_ROOT) && !predecessor.hasAttribute(COMMENT_PARAMETER) && !predecessor.hasAttribute(COMMENT_BLANKLINE))
				return delimiter + stringToIndent(predecessor.getIndentation(), false);
		}
		return delimiter;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentRegion#getDelimiter(org.eclipse.jdt.internal.ui.text.comment.CommentRange,org.eclipse.jdt.internal.ui.text.comment.CommentRange)
	 */
	protected String getDelimiter(final CommentRange previous, final CommentRange next) {

		if (previous != null) {

			if (previous.hasAttribute(COMMENT_HTML) && next.hasAttribute(COMMENT_HTML))
				return ""; //$NON-NLS-1$

			else if (next.hasAttribute(COMMENT_OPEN) || previous.hasAttribute(COMMENT_HTML | COMMENT_CLOSE))
				return ""; //$NON-NLS-1$

			else if (!next.hasAttribute(COMMENT_CODE) && previous.hasAttribute(COMMENT_CODE))
				return ""; //$NON-NLS-1$

			else if (next.hasAttribute(COMMENT_CLOSE) && previous.getLength() <= 2 && !isCommentWord(previous))
				return ""; //$NON-NLS-1$

			else if (previous.hasAttribute(COMMENT_OPEN) && next.getLength() <= 2 && !isCommentWord(next))
				return ""; //$NON-NLS-1$
		}
		return super.getDelimiter(previous, next);
	}

	/**
	 * Should root tag parameter descriptions be indented after the tag?
	 * 
	 * @return <code>true</code> iff the descriptions should be indented after,
	 *               <code>false</code> otherwise.
	 */
	protected final boolean isIndentDescriptions() {
		return fIndentDescriptions;
	}

	/**
	 * Should root tag parameter descriptions be indented?
	 * 
	 * @return <code>true</code> iff the root tags should be indented,
	 *               <code>false</code> otherwise.
	 */
	protected final boolean isIndentRoots() {
		return fIndentRoots;
	}

	/**
	 * Marks the comment ranges confined by html tags.
	 */
	protected void markHtmlRanges() {
		// Do nothing
	}

	/**
	 * Marks the comment range with its html tag attributes.
	 * 
	 * @param range
	 *                  The comment range to mark
	 * @param token
	 *                  Token associated with the comment range
	 */
	protected void markHtmlTag(final CommentRange range, final String token) {
		// Do nothing
	}

	/**
	 * Marks the comment range with its javadoc tag attributes.
	 * 
	 * @param range
	 *                  The comment range to mark
	 * @param token
	 *                  Token associated with the comment range
	 */
	protected void markJavadocTag(final CommentRange range, final String token) {
		range.markPrefixTag(COMMENT_ROOT_TAGS, COMMENT_TAG_PREFIX, token, COMMENT_ROOT);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentRegion#markRegion()
	 */
	protected final void markRegion() {

		int count= 0;
		boolean paragraph= false;

		String token= null;
		CommentRange range= null;
		CommentRange blank= null;

		for (final ListIterator iterator= getRanges().listIterator(); iterator.hasNext();) {

			range= (CommentRange)iterator.next();
			count= range.getLength();

			if (count > 0) {

				token= getText(range.getOffset(), count).toLowerCase();

				markJavadocTag(range, token);
				if (!paragraph && (range.hasAttribute(COMMENT_ROOT) || range.hasAttribute(COMMENT_PARAMETER))) {

					iterator.previous();
					while (iterator.hasPrevious()) {

						blank= (CommentRange)iterator.previous();
						if (blank.hasAttribute(COMMENT_BLANKLINE))
							iterator.remove();
						else
							break;
					}

					range.setAttribute(COMMENT_PARAGRAPH);
					paragraph= true;
				}
				markHtmlTag(range, token);
			}
		}
		markHtmlRanges();
	}
}
