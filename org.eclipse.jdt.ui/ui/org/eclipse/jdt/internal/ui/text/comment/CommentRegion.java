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
import java.util.LinkedList;
import java.util.Map;

import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ConfigurableLineTracker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TypedPosition;

import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.javadoc.IHtmlTagConstants;

/**
 * Comment region in a source code document.
 * 
 * @since 3.0
 */
public class CommentRegion extends TypedPosition implements IHtmlTagConstants, IBorderAttributes, ICommentAttributes {

	/** Default line prefix length */
	public static final int COMMENT_PREFIX_LENGTH= 3;

	/** Default comment range delimiter */
	protected static final String COMMENT_RANGE_DELIMITER= " "; //$NON-NLS-1$

	/** The borders of this region */
	private int fBorders= 0;

	/** Should all blank lines be cleared during formatting? */
	private final boolean fClear;

	/** The line delimiter used in this comment region */
	private final String fDelimiter;

	/** The document to format */
	private final IDocument fDocument;

	/** Text measurement */
	private final ITextMeasurement fTextMeasurement;

	/** The lines in this comment region */
	private final LinkedList fLines= new LinkedList();
	
	/** The formatting preferences */
	private final Map fPreferences;
	
	/** The comment ranges in this comment region */
	private final LinkedList fRanges= new LinkedList();

	/** The resulting text edit */
	private MultiTextEdit fResult= new MultiTextEdit();
	
	/** Is this comment region a single line region? */
	private final boolean fSingleLine;

	/** Number of whitespaces representing tabulator */
	private int fTabs;

	/**
	 * Creates a new comment region.
	 * 
	 * @param document
	 *                   The document which contains the comment region
 	 * @param position
	 *                   The position of this comment region in the document
 	 * @param delimiter
	 *                   The line delimiter of this comment region
	 * @param preferences
	 *                   The formatting preferences for this region
	 * @param textMeasurement
	 *                   The text measurement. Can be <code>null</code>.
	 */
	protected CommentRegion(final IDocument document, final TypedPosition position, final String delimiter, final Map preferences, final ITextMeasurement textMeasurement) {
		super(position.getOffset(), position.getLength(), position.getType());

		fDelimiter= delimiter;
		fPreferences= preferences;
		fDocument= document;
		
		fClear= fPreferences.get(PreferenceConstants.FORMATTER_COMMENT_CLEARBLANKLINES) == IPreferenceStore.TRUE;

		fTextMeasurement= textMeasurement;
		
		if (fPreferences.containsKey(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE))
			try {
				fTabs= Integer.parseInt((String) fPreferences.get(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE));
			} catch (NumberFormatException e) {
				fTabs= 4;
			}
		else
			fTabs= 4;

		final ILineTracker tracker= new ConfigurableLineTracker(new String[] { delimiter });

		IRegion range= null;
		CommentLine line= null;

		tracker.set(getText(0, getLength()));
		final int lines= tracker.getNumberOfLines();

		fSingleLine= lines == 1;

		try {

			for (int index= 0; index < lines; index++) {

				range= tracker.getLineInformation(index);
				line= CommentObjectFactory.createLine(this);
				line.append(new CommentRange(range.getOffset(), range.getLength()));

				fLines.add(line);
			}

		} catch (BadLocationException exception) {
			// Should not happen
		}
	}

	/**
	 * Appends the comment range to this comment region.
	 * 
	 * @param range
	 *                   Comment range to append to this comment region
	 */
	protected final void append(final CommentRange range) {
		fRanges.addLast(range);
	}

	/**
	 * Can the comment range be appended to the comment line?
	 * 
	 * @param line
	 *                  Comment line where to append the comment range
	 * @param previous
	 *                  Comment range which is the predecessor of the current comment
	 *                  range
	 * @param next
	 *                  Comment range to test whether it can be appended to the
	 *                  comment line
	 * @param index
	 *                  Amount of space in the comment line used by already inserted
	 *                  comment ranges
	 * @param width
	 *                  The maximal width of text in this comment region measured in
	 *                  average character widths
	 * @return <code>true</code> iff the comment range can be added to the
	 *               line, <code>false</code> otherwise
	 */
	protected boolean canAppend(final CommentLine line, final CommentRange previous, final CommentRange next, final int index, final int width) {
		return index == 0 || index + next.getLength() <= width;
	}

	/**
	 * Can the whitespace between the two comment ranges be formatted?
	 * 
	 * @param previous
	 *                   Previous comment range which was already formatted
	 * @param next
	 *                   Next comment range to be formatted
	 * @return <code>true</code> iff the next comment range can be formatted,
	 *               <code>false</code> otherwise.
	 */
	protected boolean canFormat(final CommentRange previous, final CommentRange next) {
		return true;
	}

	/**
	 * Formats the comment region.
	 * 
	 * @param indentation	the indentation of the comment region
	 * @return The resulting text edit of the formatting process
	 */
	public final TextEdit format(final String indentation) {

		fResult= new MultiTextEdit();

		final String probe= getText(0, CommentLine.NON_FORMAT_START_PREFIX.length());
		if (!probe.startsWith(CommentLine.NON_FORMAT_START_PREFIX)) {

			int margin= 80;
			try {
				margin= Integer.parseInt(fPreferences.get(PreferenceConstants.FORMATTER_COMMENT_LINELENGTH).toString());
			} catch (Exception exception) {
				// Do nothing
			}
			margin= Math.max(COMMENT_PREFIX_LENGTH + 1, margin - stringToLength(indentation) - COMMENT_PREFIX_LENGTH);

			tokenizeRegion();
			markRegion();
			wrapRegion(margin);
			formatRegion(indentation, margin);

		}
		return fResult;
	}

	/**
	 * Formats this comment region.
	 * 
	 * @param indentation
	 *                   The indentation of this comment region
	 * @param width
	 *                   The maximal width of text in this comment region measured in
	 *                   average character widths
	 */
	protected void formatRegion(final String indentation, final int width) {

		final int last= fLines.size() - 1;
		if (last >= 0) {

			CommentLine previous= null;
			CommentLine next= (CommentLine)fLines.get(last);

			CommentRange range= next.getLast();
			next.formatLowerBorder(range, indentation, width);

			for (int line= last; line >= 0; line--) {

				previous= next;
				next= (CommentLine)fLines.get(line);

				range= next.formatLine(previous, range, indentation, line); // FIXME: range is fLines.get(last).getLast() in first iteration; leads to BadLocationException in logEdit(..)
			}
			next.formatUpperBorder(range, indentation, width);
		}
	}

	/**
	 * Returns the line delimiter used in this comment region.
	 * 
	 * @return The line delimiter for this comment region
	 */
	protected final String getDelimiter() {
		return fDelimiter;
	}

	/**
	 * Returns the line delimiter used in this comment line break.
	 * 
	 * @param predecessor
	 *                  The predecessor comment line after the line break
	 * @param successor
	 *                  The successor comment line before the line break
	 * @param previous
	 *                  The comment range after the line break
	 * @param next
	 *                  The comment range before the line break
	 * @param indentation
	 *                  Indentation of the formatted line break
	 * @return The line delimiter for this comment line break
	 */
	protected String getDelimiter(final CommentLine predecessor, final CommentLine successor, final CommentRange previous, final CommentRange next, final String indentation) {
		return fDelimiter + indentation + successor.getContentPrefix();
	}

	/**
	 * Returns the range delimiter for this comment range break.
	 * 
	 * @param previous
	 *                   The previous comment range to the right of the range
	 *                   delimiter
	 * @param next
	 *                   The next comment range to the left of the range delimiter
	 * @return The delimiter for this comment range break
	 */
	protected String getDelimiter(final CommentRange previous, final CommentRange next) {
		return COMMENT_RANGE_DELIMITER;
	}

	/**
	 * Returns the document of this comment region.
	 * 
	 * @return The document of this region
	 */
	protected final IDocument getDocument() {
		return fDocument;
	}

	/**
	 * Returns the formatting preferences.
	 * 
	 * @return The formatting preferences
	 */
	public final Map getPreferences() {
		return fPreferences;
	}

	/**
	 * Returns the comment ranges in this comment region
	 * 
	 * @return The comment ranges in this region
	 */
	protected final LinkedList getRanges() {
		return fRanges;
	}

	/**
	 * Returns the number of comment lines in this comment region.
	 * 
	 * @return The number of lines in this comment region
	 */
	protected final int getSize() {
		return fLines.size();
	}

	/**
	 * Returns the text of this comment region in the indicated range.
	 * 
	 * @param position
	 *                  The offset of the comment range to retrieve in comment region
	 *                  coordinates
	 * @param count
	 *                  The length of the comment range to retrieve
	 * @return The content of this comment region in the indicated range
	 */
	protected final String getText(final int position, final int count) {

		String content= ""; //$NON-NLS-1$
		try {
			content= fDocument.get(getOffset() + position, count);
		} catch (BadLocationException exception) {
			// Should not happen
		}
		return content;
	}

	/**
	 * Does the border <code>border</code> exist?
	 * 
	 * @param border
	 *                  The type of the border. Must be a border attribute of <code>CommentRegion</code>.
	 * @return <code>true</code> iff this border exists, <code>false</code>
	 *               otherwise.
	 */
	protected final boolean hasBorder(final int border) {
		return (fBorders & border) == border;
	}

	/**
	 * Does the comment range consist of letters and digits only?
	 * 
	 * @param range
	 *                   The comment range to text
	 * @return <code>true</code> iff the comment range consists of letters
	 *               and digits only, <code>false</code> otherwise.
	 */
	protected final boolean isAlphaNumeric(final CommentRange range) {

		final String token= getText(range.getOffset(), range.getLength());

		for (int index= 0; index < token.length(); index++) {
			if (!Character.isLetterOrDigit(token.charAt(index)))
				return false;
		}
		return true;
	}

	/**
	 * Does the comment range contain no letters and digits?
	 * 
	 * @param range
	 *                   The comment range to text
	 * @return <code>true</code> iff the comment range contains no letters
	 *               and digits, <code>false</code> otherwise.
	 */
	protected final boolean isNonAlphaNumeric(final CommentRange range) {

		final String token= getText(range.getOffset(), range.getLength());

		for (int index= 0; index < token.length(); index++) {
			if (Character.isLetterOrDigit(token.charAt(index)))
				return false;
		}
		return true;
	}

	/**
	 * Should blank lines be cleared during formatting?
	 * 
	 * @return <code>true</code> iff blank lines should be cleared, <code>false</code>
	 *               otherwise
	 */
	protected final boolean isClearLines() {
		return fClear;
	}

	/**
	 * Is this comment region a single line region?
	 * 
	 * @return <code>true</code> iff this region is single line, <code>false</code>
	 *               otherwise.
	 */
	protected final boolean isSingleLine() {
		return fSingleLine;
	}

	/**
	 * Logs a text edit operation occurred during the formatting process
	 * 
	 * @param change
	 *                   The changed text
	 * @param position
	 *                   Offset measured in comment region coordinates where to apply
	 *                   the changed text
	 * @param count
	 *                   Length of the range where to apply the changed text
	 * @return The resulting text edit, or <code>null</code> iff the
	 *               operation can not be performed.
	 */
	protected final TextEdit logEdit(final String change, final int position, final int count) {

		TextEdit child= null;
		try {

			final int base= getOffset() + position;
			final String content= fDocument.get(base, count);

			if (!change.equals(content)) {

				if (count > 0)
					child= new ReplaceEdit(base, count, change);
				else
					child= new InsertEdit(base, change);

				fResult.addChild(child);
			}

		} catch (BadLocationException exception) {
			// Should not happen
		} catch (MalformedTreeException exception) {
			// Do nothing
			JavaPlugin.log(exception);
		}
		return child;
	}

	/**
	 * Marks the comment ranges in this comment region.
	 */
	protected void markRegion() {
		// Do nothing
	}

	/**
	 * Set the border type <code>border</code> to true.
	 * 
	 * @param border
	 *                   The type of the border. Must be a border attribute of <code>CommentRegion</code>.
	 */
	protected final void setBorder(final int border) {
		fBorders |= border;
	}

	/**
	 * Computes the equivalent indentation for a string
	 * 
	 * @param reference
	 *                   The string to compute the indentation for
	 * @param tabs
	 *                   <code>true</code> iff the indentation should use tabs,
	 *                   <code>false</code> otherwise.
	 * @return The indentation string
	 */
	protected final String stringToIndent(final String reference, final boolean tabs) {

		int space;
		int pixels;

		if (fTextMeasurement != null) {
			pixels= stringToPixels(reference);
			space= fTextMeasurement.computeWidth(" "); //$NON-NLS-1$
		} else {
			space= 1;
			pixels= reference.length();
			int index= -1;
			while ((index= reference.indexOf('\t', index+1)) >= 0)
				pixels += fTabs-1;
		}

		final StringBuffer buffer= new StringBuffer();
		final int spaces= pixels / space;

		if (tabs) {

			final int count= spaces / fTabs;
			final int modulo= spaces % fTabs;

			for (int index= 0; index < count; index++)
				buffer.append('\t');

			for (int index= 0; index < modulo; index++)
				buffer.append(' ');

		} else {

			for (int index= 0; index < spaces; index++)
				buffer.append(' ');
		}
		return buffer.toString();
	}

	/**
	 * Returns the length of the in expanded characters.
	 * 
	 * @param reference
	 *                   The string to get the length for
	 * @return The length of the string in expanded characters
	 */
	protected final int stringToLength(final String reference) {

		int tabs= 0;
		int count= reference.length();

		for (int index= 0; index < count; index++) {

			if (reference.charAt(index) == '\t')
				tabs++;
		}
		count += tabs * (fTabs - 1);

		return count;
	}

	/**
	 * Returns the width of the string in pixels.
	 * 
	 * @param reference
	 *                   The string to get the width for
	 * @return The width of the string in pixels
	 */
	protected final int stringToPixels(final String reference) {

		final StringBuffer buffer= new StringBuffer();

		char character= 0;
		for (int index= 0; index < reference.length(); index++) {

			character= reference.charAt(index);
			if (character == '\t') {

				for (int tab= 0; tab < fTabs; tab++)
					buffer.append(' ');

			} else
				buffer.append(character);
		}
		return fTextMeasurement.computeWidth(buffer.toString());
	}

	/**
	 * Tokenizes the comment region.
	 */
	protected void tokenizeRegion() {

		int index= 0;
		CommentLine line= null;

		for (final Iterator iterator= fLines.iterator(); iterator.hasNext(); index++) {

			line= (CommentLine)iterator.next();

			line.scanLine(index);
			line.tokenizeLine(index);
		}
	}

	/**
	 * Wraps the comment ranges in this comment region into comment lines.
	 * 
	 * @param width
	 *                  The maximal width of text in this comment region measured in
	 *                  average character widths
	 */
	protected void wrapRegion(final int width) {

		fLines.clear();

		int index= 0;
		boolean adapted= false;

		CommentLine successor= null;
		CommentLine predecessor= null;

		CommentRange previous= null;
		CommentRange next= null;

		while (!fRanges.isEmpty()) {

			index= 0;
			adapted= false;

			predecessor= successor;
			successor= CommentObjectFactory.createLine(this);
			fLines.add(successor);

			while (!fRanges.isEmpty()) {
				next= (CommentRange)fRanges.getFirst();

				if (canAppend(successor, previous, next, index, width)) {

					if (!adapted && predecessor != null) {

						successor.adapt(predecessor);
						adapted= true;
					}

					fRanges.removeFirst();
					successor.append(next);

					index += (next.getLength() + 1);
					previous= next;
				} else
					break;
			}
		}
	}
}
