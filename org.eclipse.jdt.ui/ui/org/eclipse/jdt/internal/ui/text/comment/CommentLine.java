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

/**
 * General comment line in a comment region.
 * 
 * @since 3.0
 */
public abstract class CommentLine {

	/** Html tag postfix */
	public static final char HTML_TAG_POSTFIX= '>';

	/** Html tag prefix */
	public static final char HTML_TAG_PREFIX= '<';

	/** The sequence of comment ranges in this line */
	protected final ArrayList fRanges= new ArrayList();

	/** The region this comment line belongs to */
	protected final CommentRegion fRegion;

	/**
	 * Creates a new comment line.
	 * 
	 * @param region Comment region to create the line for
	 * @param range Range of the line in the underlying text store measured in comment region coordinates 
	 */
	protected CommentLine(CommentRegion region, CommentRange range) {
		fRegion= region;
		fRanges.add(range);
	}

	/**
	 * Returns the line prefix of content lines.
	 * 
	 * @return Line prefix of content lines
	 */
	protected abstract String getContentLinePrefix();

	/**
	 * Returns the line prefix of end lines.
	 * 
	 * @return Line prefix of end lines
	 */
	protected abstract String getEndLinePrefix();

	/**
	 * Returns the line prefix for the line with the indicated index.
	 * 
	 * @param index Index of the line to get the prefix for
	 */
	protected String getLinePrefix(int index) {

		String prefix= null;
		if (index == 0)
			prefix= getStartLinePrefix();
		else if (index == fRegion.getLineCount() - 1)
			prefix= getEndLinePrefix();
		else
			prefix= getContentLinePrefix();

		return prefix;
	}

	/**
	 * Returns the line prefix of start lines.
	 * 
	 * @return Line prefix of start lines
	 */
	protected abstract String getStartLinePrefix();

	/**
	 * Scans this line in the comment region.
	 * 
	 * @param index Index of this line in the comment region
	 * @return <code>true</code> iff this line could successfully be scanned, <code>false</code> otherwise
	 */
	public boolean scanLine(int index) {

		final String prefix= getLinePrefix(index).trim();
		final CommentRange range= (CommentRange)fRanges.get(0);
		final String content= fRegion.getContent(range);

		// Search for the prefix
		int offset= content.indexOf(prefix);
		if (offset >= 0) {

			// Update content range
			offset += prefix.length();
			range.changeOffset(offset);
			range.changeLength(-offset);

			return true;
		}
		return false;
	}

	/** 
	 * Tokenizes this line into token ranges.
	 */
	public void tokenizeLine() {

		final CommentRange range= (CommentRange)fRanges.get(0);
		final String content= fRegion.getContent(range);

		// Remove old line range
		fRanges.remove(0);

		int offset= 0;
		int index= offset;

		while (offset < content.length()) {

			// Skip white spaces
			while (offset < content.length() && Character.isWhitespace(content.charAt(offset)))
				offset++;

			// Skip tag characters
			index= offset;
			if (offset < content.length() && content.charAt(offset) == HTML_TAG_PREFIX)
				index++;

			// Record line token
			while (index < content.length() && !Character.isWhitespace(content.charAt(index)) && content.charAt(index) != HTML_TAG_POSTFIX && content.charAt(index) != HTML_TAG_PREFIX)
				index++;

			// Skip tag characters
			if (index < content.length() && content.charAt(index) == HTML_TAG_POSTFIX)
				index++;

			// Insert token range
			if (index - offset > 0)
				fRanges.add(CommentObjectFactory.getRange(fRegion, offset + range.getOffset(), index - offset));

			offset= index;
		}
	}

	/*
	 * @see java.lang.Object#toString()
	 */
	public String toString() {

		// TODO remove

		final StringBuffer buffer= new StringBuffer(32 * fRanges.size());

		for (int token= 0; token < fRanges.size(); token++)
			buffer.append(fRanges.get(token).toString());

		return buffer.toString();
	}
}
