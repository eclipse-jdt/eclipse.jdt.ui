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

import java.util.ArrayList;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ConfigurableLineTracker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TypedRegion;

/**
 * Comment region in a source code document.
 * 
 * @since 3.0
 */
public class CommentRegion extends TypedRegion {

	/** The line delimiter to use in this comment region */
	private final String fDelimiter;

	/** The document to format */
	private final IDocument fDocument;

	/** Has this region an lower border? */
	private boolean fHasLowerBorder= false;

	/** Has this region an right border? */
	private boolean fHasRightBorder= false;

	/** Has this region an upper border? */
	private boolean fHasUpperBorder= false;

	/** The sequence of lines in this comment region */
	protected final ArrayList fLines= new ArrayList();

	/**
	 * Creates a new comment region.
	 * 
	 * @param document The document where this comment region belongs to
	 * @param region The typed region which forms this comment region
	 * @param delimiter The line delimiter to use in this comment region
	 */
	public CommentRegion(IDocument document, ITypedRegion region, String delimiter) {
		super(region.getOffset(), region.getLength(), region.getType());

		fDocument= document;
		fDelimiter= delimiter;

		final ILineTracker tracker= new ConfigurableLineTracker(new String[] { fDelimiter });
		try {

			IRegion range= null;
			tracker.set(fDocument.get(getOffset(), getLength()));

			for (int line= 0; line < tracker.getNumberOfLines(); line++) {

				range= tracker.getLineInformation(line);
				fLines.add(CommentObjectFactory.getLine(this, range));
			}
		} catch (BadLocationException e) {
			// Should not happen
		}
	}

	/**
	 * Formats the comment region.
	 */
	public void formatRegion() {

		scanLines();
		tokenizeLines();

		// TODO remove
		System.out.print(this);
	}

	/**
	 * Returns the textual content of this comment region in the indicated range.
	 * 
	 * @param range The range in comment region coordinates to retrieve the content of
	 * @return The content of the region in this comment region
	 */
	public String getContent(IRegion range) {

		String content= ""; //$NON-NLS-1$
		try {
			content= fDocument.get(getOffset() + range.getOffset(), range.getLength());
		} catch (BadLocationException e) {
			// Should not happen
		}
		return content;
	}

	/**
	 * Returns the line delimiter used in this comment region.
	 * 
	 * @return The line delimiter used in this comment region
	 */
	public final String getDelimiter() {
		return fDelimiter;
	}

	/**
	 * Returns the number of comment lines in this comment region.
	 * 
	 * @return The number of lines in this comment region
	 */
	public final int getLineCount() {
		return fLines.size();
	}

	/**
	 * Has this region a lower border?
	 * 
	 * @return <code>true</code> iff this region has a lower border, <code>false</code> otherwise.
	 */
	public final boolean hasLowerBorder() {
		return fHasLowerBorder;
	}

	/**
	 * Has this region a right border?
	 * 
	 * @return <code>true</code> iff this region has a right border, <code>false</code> otherwise.
	 */
	public final boolean hasRightBorder() {
		return fHasRightBorder;
	}

	/**
	 * Has this region an upper border?
	 * 
	 * @return <code>true</code> iff this region has an upper border, <code>false</code> otherwise.
	 */
	public final boolean hasUpperBorder() {
		return fHasUpperBorder;
	}

	/**
	 * Scans the lines in this comment region.
	 */
	protected void scanLines() {

		for (int line= 0; line < fLines.size(); line++)
			 ((CommentLine)fLines.get(line)).scanLine(line);
	}

	/**
	 * Marks this region as having a lower border.
	 */
	public final void setLowerBorder() {
		fHasLowerBorder= true;
	}

	/**
	 * Marks this region as having a right border.
	 */
	public final void setRightBorder() {
		fHasRightBorder= true;
	}

	/**
	 * Marks this region as having an upper border.
	 */
	public final void setUpperBorder() {
		fHasUpperBorder= true;
	}

	/**
	 * Tokenizes the lines in this comment region.
	 */
	protected void tokenizeLines() {

		for (int line= 0; line < fLines.size(); line++) {
			((CommentLine)fLines.get(line)).tokenizeLine();
		}
	}

	/*
	 * @see java.lang.Object#toString()
	 */
	public String toString() {

		// TODO remove

		final StringBuffer buffer= new StringBuffer(80 * fLines.size());

		for (int line= 0; line < fLines.size(); line++)
			buffer.append(fLines.get(line) + "\n"); //$NON-NLS-1$

		return buffer.toString();
	}
}
