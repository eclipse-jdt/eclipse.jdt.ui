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

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.GC;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.ConfigurableLineTracker;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.jdt.ui.PreferenceConstants;

/**
 * Comment region in a source code document.
 * 
 * @since 3.0
 */
public class CommentRegion extends TypedPosition implements IHtmlTagConstants, IBorderAttributes, ICommentAttributes {

	/** Position category of comment regions */
	protected static final String COMMENT_POSITION_CATEGORY= "__comment_position"; //$NON-NLS-1$

	/** Default line prefix length */
	public static final int COMMENT_PREFIX_LENGTH= 3;

	/** Default range delimiter */
	protected static final String COMMENT_RANGE_DELIMITER= " "; //$NON-NLS-1$

	/** The borders of this range */
	private int fBorders= 0;

	/** Should all blank lines be cleared during formatting? */
	private final boolean fClear;

	/** The line delimiter used in this comment region */
	private final String fDelimiter;

	/** The document to format */
	private final IDocument fDocument;

	/** Graphics context for non-monospace fonts */
	private final GC fGraphics;

	/** The sequence of lines in this comment region */
	private final LinkedList fLines= new LinkedList();

	/** The sequence of comment ranges in this comment region */
	private final LinkedList fRanges= new LinkedList();

	/** Is this comment region a single line region? */
	private final boolean fSingleLine;

	/** The comment formatting strategy for this comment region */
	private final CommentFormattingStrategy fStrategy;

	/** Number of characters representing tabulator */
	private final int fTabs;

	/**
	 * Creates a new comment region.
	 * 
	 * @param strategy
	 *                  The comment formatting strategy used to format this comment
	 *                  region
	 * @param position
	 *                  The typed position which forms this comment region
	 * @param delimiter
	 *                  The line delimiter to use in this comment region
	 */
	protected CommentRegion(final CommentFormattingStrategy strategy, final TypedPosition position, final String delimiter) {
		super(position.getOffset(), position.getLength(), position.getType());

		fStrategy= strategy;
		fDelimiter= delimiter;
		fClear= fStrategy.getPreferences().get(PreferenceConstants.FORMATTER_COMMENT_CLEARBLANKLINES) == IPreferenceStore.TRUE;

		final ISourceViewer viewer= strategy.getViewer();
		final StyledText text= viewer.getTextWidget();
		fDocument= viewer.getDocument();

		if (text != null && !text.isDisposed()) {
			fGraphics= new GC(text);
			fGraphics.setFont(text.getFont());
			fTabs= text.getTabs();
		} else {
			fGraphics= null;
			fTabs= 4;
		}

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
	 *                  Comment range to append to this comment region
	 */
	protected final void append(final CommentRange range) {
		fRanges.addLast(range);
	}

	/**
	 * Applies the formatted comment region to the underlying document.
	 * 
	 * @param indentation
	 *                  Indentation of the formatted comment region
	 * @param width
	 *                  The maximal width of text in this comment region measured in
	 *                  average character widths
	 */
	protected void applyRegion(final String indentation, final int width) {

		final int last= fLines.size() - 1;
		if (last >= 0) {

			CommentLine previous= null;
			CommentLine next= (CommentLine)fLines.get(last);

			CommentRange range= next.getLast();
			next.applyEnd(range, indentation, width);

			for (int line= last; line >= 0; line--) {

				previous= next;
				next= (CommentLine)fLines.get(line);

				range= next.applyLine(previous, range, indentation, line);
			}
			next.applyStart(range, indentation, width);
		}
	}

	/**
	 * Applies the changed content to the underlying document
	 * 
	 * @param change
	 *                  Text content to apply to the underlying document
	 * @param position
	 *                  Offset measured in comment region coordinates where to apply
	 *                  the changed content
	 * @param count
	 *                  Length of the content to be changed
	 */
	protected final void applyText(final String change, final int position, final int count) {

		try {

			final int base= getOffset() + position;
			final String content= fDocument.get(base, count);

			if (!change.equals(content))
				fDocument.replace(getOffset() + position, count, change);

		} catch (BadLocationException exception) {
			// Should not happen
		}
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
	 * @param space
	 *                  Amount of space in the comment line used by already inserted
	 *                  comment ranges
	 * @param width
	 *                  The maximal width of text in this comment region measured in
	 *                  average character widths
	 * @return <code>true</code> iff the comment range can be added to the
	 *               line, <code>false</code> otherwise
	 */
	protected boolean canAppend(final CommentLine line, final CommentRange previous, final CommentRange next, final int space, final int width) {
		return space == 0 || space + next.getLength() < width;
	}

	/**
	 * Can the two current comment ranges be applied to the underlying
	 * document?
	 * 
	 * @param previous
	 *                  Previous comment range which was already applied
	 * @param next
	 *                  Next comment range to be applied
	 * @return <code>true</code> iff the next comment range can be applied,
	 *               <code>false</code> otherwise
	 */
	protected boolean canApply(final CommentRange previous, final CommentRange next) {
		return true;
	}

	/**
	 * Finalizes the comment region and adjusts its starting indentation.
	 * 
	 * @param indentation
	 *                  Indentation of the formatted comment region
	 */
	protected void finalizeRegion(final String indentation) {
		// Do nothing
	}

	/**
	 * Formats the comment region.
	 * 
	 * @param indentation
	 *                  Indentation of the formatted comment region
	 */
	public void format(String indentation) {

		final String probe= getText(0, CommentLine.NON_FORMAT_START_PREFIX.length());
		if (probe.startsWith(CommentLine.NON_FORMAT_START_PREFIX))
			return;

		final Map preferences= fStrategy.getPreferences();

		int margin= 80;
		try {
			margin= Integer.parseInt(preferences.get(PreferenceConstants.FORMATTER_COMMENT_LINELENGTH).toString());
		} catch (Exception exception) {
			// Do nothing
		}
		margin= Math.max(COMMENT_PREFIX_LENGTH + 1, margin - stringToLength(indentation) - COMMENT_PREFIX_LENGTH);

		fDocument.addPositionCategory(COMMENT_POSITION_CATEGORY);

		final IPositionUpdater positioner= new DefaultPositionUpdater(COMMENT_POSITION_CATEGORY);
		fDocument.addPositionUpdater(positioner);

		try {

			initializeRegion();
			markRegion();
			wrapRegion(margin);
			applyRegion(indentation, margin);
			finalizeRegion(indentation);

		} finally {

			if (fGraphics != null && !fGraphics.isDisposed())
				fGraphics.dispose();

			try {

				fDocument.removePositionCategory(COMMENT_POSITION_CATEGORY);
				fDocument.removePositionUpdater(positioner);

			} catch (BadPositionCategoryException exception) {
				// Should not happen
			}
		}
	}

	/**
	 * Returns the general line delimiter used in this comment region.
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
	 *                  The predecessor comment line before the line break
	 * @param successor
	 *                  The successor comment line after the line break
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
	 * Returns the range delimiter for this comment range break in this comment
	 * region.
	 * 
	 * @param previous
	 *                  The previous comment range to the right of the range delimiter
	 * @param next
	 *                  The next comment range to the left of the range delimiter
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
	 * Returns the list of comment ranges in this comment region
	 * 
	 * @return The list of comment ranges in this region
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
	 * Returns the comment formatting strategy used to format this comment
	 * region.
	 * 
	 * @return The formatting strategy for this comment region
	 */
	protected final CommentFormattingStrategy getStrategy() {
		return fStrategy;
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
	 * Initializes the internal representation of the comment region.
	 */
	protected void initializeRegion() {

		try {
			fDocument.addPosition(COMMENT_POSITION_CATEGORY, this);
		} catch (BadLocationException exception) {
			// Should not happen
		} catch (BadPositionCategoryException exception) {
			// Should not happen
		}

		int index= 0;
		CommentLine line= null;

		for (final Iterator iterator= fLines.iterator(); iterator.hasNext(); index++) {

			line= (CommentLine)iterator.next();

			line.scanLine(index);
			line.tokenizeLine(index);
		}
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
	 * Is the current comment range a word?
	 * 
	 * @param current
	 *                  Comment range to test whether it is a word
	 * @return <code>true</code> iff the comment range is a word, <code>false</code>
	 *               otherwise.
	 */
	protected final boolean isCommentWord(final CommentRange current) {

		final String token= getText(current.getOffset(), current.getLength());

		for (int index= 0; index < token.length(); index++) {
			if (!Character.isLetterOrDigit(token.charAt(index)))
				return false;
		}
		return true;
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
	 * Marks the attributed ranges in this comment region.
	 */
	protected void markRegion() {
		// Do nothing
	}

	/**
	 * Set the border <code>border</code> to true.
	 * 
	 * @param border
	 *                  The type of the border. Must be a border attribute of <code>CommentRegion</code>.
	 */
	protected final void setBorder(final int border) {
		fBorders |= border;
	}

	/**
	 * Returns the indentation string for a reference string
	 * 
	 * @param reference
	 *                  The reference string to get the indentation string for
	 * @param tabs
	 *                  <code>true</code> iff the indent should use tabs, <code>false</code>
	 *                  otherwise.
	 * @return The indentation string
	 */
	protected final String stringToIndent(final String reference, final boolean tabs) {

		int space= 1;
		int pixels= reference.length();

		if (fGraphics != null) {
			pixels= stringToPixels(reference);
			space= fGraphics.stringExtent(" ").x; //$NON-NLS-1$
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
	 * Returns the length of the reference string in characters.
	 * 
	 * @param reference
	 *                  The reference string to get the length
	 * @return The length of the string in characters
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
	 * Returns the width of the reference string in pixels.
	 * 
	 * @param reference
	 *                  The reference string to get the width
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
		return fGraphics.stringExtent(buffer.toString()).x;
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
