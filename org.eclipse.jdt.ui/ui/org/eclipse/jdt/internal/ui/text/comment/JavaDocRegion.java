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
public class JavaDocRegion extends CommentRegion implements IJavaDocAttributes, IJavaDocTagConstants {

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
		fParameterNewLine= preferences.get(PreferenceConstants.FORMATTER_COMMENT_NEWLINEPARAM) == IPreferenceStore.TRUE;
		fIndentRootDescriptions= preferences.get(PreferenceConstants.FORMATTER_COMMENT_INDENTPARAMDESC) == IPreferenceStore.TRUE;
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

		final JavaDocRange current= (JavaDocRange)next;
		final JavaDocRange last= (JavaDocRange)previous;

		final JavaDocLine reference= (JavaDocLine)line;

		if (current.getLength() <= 2 && !isWord(current))
			return true;

		if (fParameterNewLine && reference.hasAttribute(JAVADOC_PARAMETER) && reference.getSize() > 1)
			return false;

		if (last != null) {

			if (offset != 0 && (current.hasAttribute(JAVADOC_PARAMETER) || current.hasAttribute(JAVADOC_ROOT) || current.hasAttribute(JAVADOC_SEPARATOR) || current.hasAttribute(JAVADOC_NEWLINE) || last.hasAttribute(JAVADOC_BREAK) || last.hasAttribute(JAVADOC_SEPARATOR)))
				return false;

			if (current.hasAttribute(JAVADOC_IMMUTABLE) && last.hasAttribute(JAVADOC_IMMUTABLE))
				return true;
		}

		if (fIndentRootTags && !reference.hasAttribute(JAVADOC_ROOT) && !reference.hasAttribute(JAVADOC_PARAMETER))
			length -= stringToLength(reference.getReference());

		return super.canAppend(line, previous, next, offset, length);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentRegion#canApply(org.eclipse.jdt.internal.ui.text.comment.CommentRange, org.eclipse.jdt.internal.ui.text.comment.CommentRange)
	 */
	protected boolean canApply(final CommentRange previous, final CommentRange next) {

		final JavaDocRange last= (JavaDocRange)previous;
		final JavaDocRange current= (JavaDocRange)next;

		if (last != null) {

			final boolean isCurrentCode= current.hasAttribute(JAVADOC_CODE);
			final boolean isLastCode= last.hasAttribute(JAVADOC_CODE);

			try {

				final int offset= getOffset();
				final IDocument document= getDocument();

				if (!isLastCode && isCurrentCode)
					document.addPosition(CODE_POSITION_CATEGORY, new Position(offset + current.getOffset() + current.getLength()));
				else if (isLastCode && !isCurrentCode)
					document.addPosition(CODE_POSITION_CATEGORY, new Position(offset + last.getOffset()));

			} catch (BadLocationException exception) {
				// Should not happen
			} catch (BadPositionCategoryException exception) {
				// Should not happen
			}

			if (last.hasAttribute(JAVADOC_IMMUTABLE) && current.hasAttribute(JAVADOC_IMMUTABLE) && !isLastCode)
				return false;

		}
		return true;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentRegion#finishRegion(java.lang.String)
	 */
	protected void finalizeRegion(final String indentation) {

		if (fFormatSource) {

			final String test= indentation + JavaDocLine.MULTI_COMMENT_CONTENT_PREFIX;
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

		final JavaDocRange last= (JavaDocRange)previous;
		final JavaDocRange current= (JavaDocRange)next;

		final JavaDocLine reference= (JavaDocLine)predecessor;
		final String delimiter= super.getDelimiter(predecessor, successor, previous, next, indentation);

		if (last != null) {

			if (last.hasAttribute(JAVADOC_IMMUTABLE | JAVADOC_SEPARATOR) && !current.hasAttribute(JAVADOC_CODE))
				return delimiter + delimiter;

			else if (last.hasAttribute(JAVADOC_CODE) && !current.hasAttribute(JAVADOC_CODE))
				return getDelimiter();

			else if (current.hasAttribute(JAVADOC_IMMUTABLE | JAVADOC_SEPARATOR) || last.hasAttribute(JAVADOC_PARAGRAPH))
				return delimiter + delimiter;

			else if (fIndentRootTags && !reference.hasAttribute(JAVADOC_ROOT) && !reference.hasAttribute(JAVADOC_PARAMETER))
				return delimiter + stringToIndent(reference.getReference(), false);
		}
		return delimiter;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentRegion#getDelimiter(org.eclipse.jdt.internal.ui.text.comment.CommentRange, org.eclipse.jdt.internal.ui.text.comment.CommentRange)
	 */
	protected String getDelimiter(final CommentRange previous, final CommentRange next) {

		final JavaDocRange current= (JavaDocRange)next;
		final JavaDocRange last= (JavaDocRange)previous;

		if (last != null) {

			if (last.hasAttribute(JAVADOC_HTML) && current.hasAttribute(JAVADOC_HTML))
				return ""; //$NON-NLS-1$

			else if (current.hasAttribute(JAVADOC_OPEN) || last.hasAttribute(JAVADOC_CLOSE))
				return ""; //$NON-NLS-1$

			else if (!current.hasAttribute(JAVADOC_CODE) && last.hasAttribute(JAVADOC_CODE))
				return ""; //$NON-NLS-1$

			else if (current.hasAttribute(JAVADOC_CLOSE) && last.getLength() <= 2 && !isWord(last))
				return ""; //$NON-NLS-1$

			else if (last.hasAttribute(JAVADOC_OPEN) && current.getLength() <= 2 && !isWord(current))
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
		String token= null;
		JavaDocRange current= null;

		for (int index= 0; index < tags.length; index++) {

			level= 0;
			for (final Iterator iterator= getRanges().iterator(); iterator.hasNext();) {

				current= (JavaDocRange)iterator.next();
				token= getText(current.getOffset(), current.getLength());

				level= current.markRange(token, tags[index], level, key, html);
			}
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentRegion#markRegion()
	 */
	protected void markRegion() {

		String token= null;
		JavaDocRange range= null;

		for (final Iterator iterator= getRanges().iterator(); iterator.hasNext();) {

			range= (JavaDocRange)iterator.next();
			token= getText(range.getOffset(), range.getLength()).toLowerCase();

			range.markJavadocTag(JAVADOC_PARAM_TAGS, token, JAVADOC_PARAMETER);
			range.markJavadocTag(JAVADOC_ROOT_TAGS, token, JAVADOC_ROOT);

			if (fSeparateRootTags && (range.hasAttribute(JAVADOC_ROOT) || range.hasAttribute(JAVADOC_PARAMETER))) {
				range.setAttribute(JAVADOC_PARAGRAPH);
				fSeparateRootTags= false;
			}

			range.markHtmlTag(JAVADOC_SEPARATOR_TAGS, token, JAVADOC_SEPARATOR, true, true);
			range.markHtmlTag(JAVADOC_BREAK_TAGS, token, JAVADOC_BREAK, false, true);
			range.markHtmlTag(JAVADOC_NEWLINE_TAGS, token, JAVADOC_NEWLINE, true, false);
			range.markHtmlTag(JAVADOC_IMMUTABLE_TAGS, token, JAVADOC_IMMUTABLE, true, true);
		}

		markRanges(JAVADOC_IMMUTABLE_TAGS, JAVADOC_IMMUTABLE, true);

		if (fFormatSource)
			markRanges(JAVADOC_CODE_TAGS, JAVADOC_CODE, false);
	}
}
