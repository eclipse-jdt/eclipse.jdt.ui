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
import org.eclipse.jface.text.formatter.ContentFormatter;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;

import org.eclipse.jdt.ui.PreferenceConstants;

/**
 * Javadoc region in a source code document.
 * 
 * @since 3.0
 */
public class JavaDocRegion extends MultiCommentRegion implements IJavaDocAttributes, IJavaDocTagConstants {

	/** Position category of javadoc code ranges */
	protected static final String CODE_POSITION_CATEGORY= "__javadoc_code_position"; //$NON-NLS-1$

	/** Should source code regions be formatted? */
	private final boolean fFormatSource;

	/** Should root tag parameter descriptions be indented? */
	private final boolean fIndentRootDescriptions;

	/** Should root tag paragraphs be indented? */
	private final boolean fIndentRootTags;

	/** Should description of parameters go to the next line? */
	private final boolean fParameterNewLine;

	/** Should root tags be separated from description? */
	private boolean fSeparateRootTags;

	/**
	 * Creates a new javadoc region.
	 * 
	 * @param strategy The comment formatting strategy used to format this comment region
	 * @param position The typed position which forms this javadoc region
	 * @param delimiter The line delimiter to use in this javadoc region
	 */
	protected JavaDocRegion(final CommentFormattingStrategy strategy, final TypedPosition position, final String delimiter) {
		super(strategy, position, delimiter);

		final Map preferences= getStrategy().getPreferences();

		fFormatSource= preferences.get(PreferenceConstants.FORMATTER_COMMENT_FORMATSOURCE) == IPreferenceStore.TRUE;
		fIndentRootTags= preferences.get(PreferenceConstants.FORMATTER_COMMENT_INDENTROOTTAGS) == IPreferenceStore.TRUE;
		fSeparateRootTags= preferences.get(PreferenceConstants.FORMATTER_COMMENT_SEPARATEROOTTAGS) == IPreferenceStore.TRUE;
		fParameterNewLine= preferences.get(PreferenceConstants.FORMATTER_COMMENT_NEWLINEFORPARAMETER) == IPreferenceStore.TRUE;
		fIndentRootDescriptions= preferences.get(PreferenceConstants.FORMATTER_COMMENT_INDENTPARAMETERDESCRIPTION) == IPreferenceStore.TRUE;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentRegion#applyRegion(java.lang.String, int)
	 */
	protected void applyRegion(final String indentation, final int length) {

		super.applyRegion(indentation, length);

		if (fFormatSource) {

			final ContentFormatter formatter= getStrategy().getFormatter();
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

						context.setProperty(FormattingContextProperties.CONTEXT_PARTITION, new TypedPosition(begin, end - begin, IDocument.DEFAULT_CONTENT_TYPE));
						formatter.format(document, context);
					}
				}
			} catch (BadPositionCategoryException exception) {
				// Should not happen
			}
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentRegion#canAppend(org.eclipse.jdt.internal.ui.text.comment.CommentLine, org.eclipse.jdt.internal.ui.text.comment.CommentRange, org.eclipse.jdt.internal.ui.text.comment.CommentRange, int, int)
	 */
	protected boolean canAppend(final CommentLine line, final CommentRange previous, final CommentRange next, final int offset, int length) {

		final boolean blank= next.hasAttribute(COMMENT_BLANKLINE);

		if (next.getLength() <= 2 && !blank && !isCommentWord(next))
			return true;

		if (fParameterNewLine && line.hasAttribute(JAVADOC_PARAMETER) && line.getSize() > 1)
			return false;

		if (previous != null) {

			if (offset != 0 && (blank || previous.hasAttribute(COMMENT_BLANKLINE) || next.hasAttribute(JAVADOC_PARAMETER) || next.hasAttribute(JAVADOC_ROOT) || next.hasAttribute(JAVADOC_SEPARATOR) || next.hasAttribute(COMMENT_NEWLINE) || previous.hasAttribute(COMMENT_BREAK) || previous.hasAttribute(JAVADOC_SEPARATOR)))
				return false;

			if (next.hasAttribute(JAVADOC_IMMUTABLE) && previous.hasAttribute(JAVADOC_IMMUTABLE))
				return true;
		}

		if (fIndentRootTags && !line.hasAttribute(JAVADOC_ROOT) && !line.hasAttribute(JAVADOC_PARAMETER))
			length -= stringToLength(line.getReference());

		return super.canAppend(line, previous, next, offset, length);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentRegion#canApply(org.eclipse.jdt.internal.ui.text.comment.CommentRange, org.eclipse.jdt.internal.ui.text.comment.CommentRange)
	 */
	protected boolean canApply(final CommentRange previous, final CommentRange next) {

		if (previous != null) {

			final boolean isCurrentCode= next.hasAttribute(JAVADOC_CODE);
			final boolean isLastCode= previous.hasAttribute(JAVADOC_CODE);

			try {

				final int offset= getOffset();
				final IDocument document= getDocument();

				if (!isLastCode && isCurrentCode)
					document.addPosition(CODE_POSITION_CATEGORY, new Position(offset + next.getOffset() + next.getLength()));
				else if (isLastCode && !isCurrentCode)
					document.addPosition(CODE_POSITION_CATEGORY, new Position(offset + previous.getOffset()));

			} catch (BadLocationException exception) {
				// Should not happen
			} catch (BadPositionCategoryException exception) {
				// Should not happen
			}

			if (previous.hasAttribute(JAVADOC_IMMUTABLE) && next.hasAttribute(JAVADOC_IMMUTABLE) && !isLastCode)
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
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentRegion#getDelimiter(org.eclipse.jdt.internal.ui.text.comment.CommentLine, org.eclipse.jdt.internal.ui.text.comment.CommentLine, org.eclipse.jdt.internal.ui.text.comment.CommentRange, org.eclipse.jdt.internal.ui.text.comment.CommentRange, java.lang.String)
	 */
	protected String getDelimiter(final CommentLine predecessor, final CommentLine successor, final CommentRange previous, final CommentRange next, final String indentation) {

		final String delimiter= super.getDelimiter(predecessor, successor, previous, next, indentation);

		if (previous != null) {

			if (previous.hasAttribute(JAVADOC_IMMUTABLE | JAVADOC_SEPARATOR) && !next.hasAttribute(JAVADOC_CODE) && !successor.hasAttribute(COMMENT_BLANKLINE))
				return delimiter + delimiter;

			else if (previous.hasAttribute(JAVADOC_CODE) && !next.hasAttribute(JAVADOC_CODE))
				return getDelimiter();

			else if ((next.hasAttribute(JAVADOC_IMMUTABLE | JAVADOC_SEPARATOR) || previous.hasAttribute(JAVADOC_PARAGRAPH)) && !successor.hasAttribute(COMMENT_BLANKLINE))
				return delimiter + delimiter;

			else if (fIndentRootTags && !predecessor.hasAttribute(JAVADOC_ROOT) && !predecessor.hasAttribute(JAVADOC_PARAMETER))
				return delimiter + stringToIndent(predecessor.getReference(), false);
		}
		return delimiter;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentRegion#getDelimiter(org.eclipse.jdt.internal.ui.text.comment.CommentRange, org.eclipse.jdt.internal.ui.text.comment.CommentRange)
	 */
	protected String getDelimiter(final CommentRange previous, final CommentRange next) {

		if (previous != null) {

			if (previous.hasAttribute(COMMENT_HTML) && next.hasAttribute(COMMENT_HTML))
				return ""; //$NON-NLS-1$

			else if (next.hasAttribute(COMMENT_OPEN) || previous.hasAttribute(COMMENT_HTML | COMMENT_CLOSE))
				return ""; //$NON-NLS-1$

			else if (!next.hasAttribute(JAVADOC_CODE) && previous.hasAttribute(JAVADOC_CODE))
				return ""; //$NON-NLS-1$

			else if (next.hasAttribute(COMMENT_CLOSE) && previous.getLength() <= 2 && !isCommentWord(previous))
				return ""; //$NON-NLS-1$

			else if (previous.hasAttribute(COMMENT_OPEN) && next.getLength() <= 2 && !isCommentWord(next))
				return ""; //$NON-NLS-1$
		}
		return super.getDelimiter(previous, next);
	}

	/**
	 * Should inline source code be formatted?
	 * 
	 * @return <code>true</code> iff the code should be formatted, <code>false</code> otherwise.
	 */
	protected final boolean isFormatSource() {
		return fFormatSource;
	}

	/**
	 * Should parameter descriptions be indented from their parameter?
	 * 
	 * @return <code>true</code> iff the descriptions should be indented, <code>false</code> otherwise.
	 */
	protected final boolean isIndentRootDescriptions() {
		return fIndentRootDescriptions;
	}

	/**
	 * Should javadoc root tags be indented?
	 * 
	 * @return <code>true</code> iff the root tags should be indented, <code>false</code> otherwise.
	 */
	protected final boolean isIndentRootTags() {
		return fIndentRootTags;
	}

	/**
	 * Should the formatter insert a new line after javadoc parameters?
	 * 
	 * @return <code>true</code> iff a new line should be inserted, <code>false</code> otherwise.
	 */
	protected final boolean isParameterNewLine() {
		return fParameterNewLine;
	}

	/**
	 * Should javadoc root tags be separated from the rest of the comment?
	 * 
	 * @return <code>true</code> iff the root tags should be separated, <code>false</code> otherwise.
	 */
	protected final boolean isSeparateRootTags() {
		return fSeparateRootTags;
	}

	/**
	 * Marks attributed ranges in this javadoc region.
	 * 
	 * @param tags The html tags which confine the attributed ranges
	 * @param key The key of the attribute to set when an attributed range has been recognized
	 * @param html <code>true</code> iff html tags in this attributed range should be attributed too, <code>false</code> otherwise
	 */
	protected void markRanges(final String[] tags, final int key, final boolean html) {

		int level= 0;
		int length= 0;
		String token= null;
		JavaDocRange current= null;

		for (int index= 0; index < tags.length; index++) {

			level= 0;
			for (final Iterator iterator= getRanges().iterator(); iterator.hasNext();) {

				current= (JavaDocRange)iterator.next();
				length= current.getLength();

				if (length > 0) {

					token= getText(current.getOffset(), current.getLength());
					level= current.markRange(token, tags[index], level, key, html);
				}
			}
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentRegion#markRegion()
	 */
	protected void markRegion() {

		int length= 0;
		String token= null;
		JavaDocRange range= null;

		for (final Iterator iterator= getRanges().iterator(); iterator.hasNext();) {

			range= (JavaDocRange)iterator.next();
			length= range.getLength();

			if (length > 0) {

				token= getText(range.getOffset(), length).toLowerCase();

				range.markJavadocTag(JAVADOC_PARAM_TAGS, token, JAVADOC_PARAMETER);
				range.markJavadocTag(JAVADOC_ROOT_TAGS, token, JAVADOC_ROOT);

				if (fSeparateRootTags && (range.hasAttribute(JAVADOC_ROOT) || range.hasAttribute(JAVADOC_PARAMETER))) {

					range.setAttribute(JAVADOC_PARAGRAPH);
					fSeparateRootTags= false;
				}

				if (range.hasAttribute(COMMENT_HTML)) {

					range.markHtmlTag(JAVADOC_SEPARATOR_TAGS, token, JAVADOC_SEPARATOR, true, true);
					range.markHtmlTag(JAVADOC_BREAK_TAGS, token, COMMENT_BREAK, false, true);
					range.markHtmlTag(JAVADOC_NEWLINE_TAGS, token, COMMENT_NEWLINE, true, false);
					range.markHtmlTag(JAVADOC_IMMUTABLE_TAGS, token, JAVADOC_IMMUTABLE, true, true);
				}
			}
		}

		markRanges(JAVADOC_IMMUTABLE_TAGS, JAVADOC_IMMUTABLE, true);

		if (fFormatSource)
			markRanges(JAVADOC_CODE_TAGS, JAVADOC_CODE, false);
	}
}
