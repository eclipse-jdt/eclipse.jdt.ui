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

/**
 * Javadoc comment line in a comment region.
 * 
 * @since 3.0
 */
public class JavaDocLine extends MultiCommentLine {

	/** Html tag close prefix */
	public static final String HTML_TAG_CLOSE_PREFIX= "</"; //$NON-NLS-1$

	/** Javadoc root tags */
	public static final String[] JAVADOC_ROOT_TAGS= new String[] { "@author", "@deprecated", "@exception", "@param", "@return", "@see", "@serial", "@serialData", "@serialField", "@since", "@throws", "@version" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$

	/** Javadoc separator tags */
	public static final String[] JAVADOC_SEPARATOR_TAGS= new String[] { "p", "pre" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	/** Line prefix of javadoc start lines */
	private static final String JAVADOC_START_PREFIX= "/**"; //$NON-NLS-1$

	/** Tag prefix of javadoc tags */
	public static final char JAVADOC_TAG_PREFIX= '@';

	/**
	 * Creates a new javadoc line.
	 * 
	 * @param region Comment region to create the line for
	 * @param range Range of the line in the underlying text store measured in comment region coordinates 
	 */
	public JavaDocLine(CommentRegion region, CommentRange range) {
		super(region, range);
	}

	/**
	 * Formats source code ranges in this javadoc line.
	 * 
	 * @param tag The html tag which confines the source code ranges
	 * @param level Hierarchical depth of layered source code ranges
	 * @return The new hierarchical depth of source code ranges
	 */
	public int formatSourceRanges(String tag, int level) {

		String token= null;
		JavaDocRange current= null;

		for (int range= 0; range < fRanges.size(); range++) {
			current= (JavaDocRange)fRanges.get(range);
			token= fRegion.getContent(current);

			if (hasOpeningTag(token, tag))
				formatSourceRange(current, level++);
			else if (hasClosingTag(token, tag))
				formatSourceRange(current, --level);
			else
				formatSourceRange(current, level);
		}
		return level;
	}

	/**
	 * Formats the current javadoc range as source code based on the current level.
	 * 
	 * @param current The javadoc range to format
	 * @param level Hierarchical depth of layered source code ranges
	 */
	private void formatSourceRange(JavaDocRange current, int level) {
		// TODO Auto-generated method stub
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#getStartLinePrefix()
	 */
	protected String getStartLinePrefix() {
		return JAVADOC_START_PREFIX;
	}

	/**
	 * Has this javadoc range a closing html tag?
	 * 
	 * @param token Token belonging to the javadoc range
	 * @param tag Html tag to find
	 * @return <code>true</code> iff this javadoc has a closing html tag, <code>false</code> otherwise
	 */
	private boolean hasClosingTag(String token, String tag) {
		return token.startsWith(HTML_TAG_CLOSE_PREFIX) && token.charAt(token.length() - 1) == HTML_TAG_POSTFIX && token.substring(HTML_TAG_CLOSE_PREFIX.length(), token.length() - 1).equals(tag);
	}

	/**
	 * Has this javadoc range an opening html tag?
	 * 
	 * @param token Token belonging to the javadoc range
	 * @param tag Html tag to find
	 * @return <code>true</code> iff this javadoc has an opening html tag, <code>false</code> otherwise
	 */
	private boolean hasOpeningTag(String token, String tag) {
		return token.charAt(0) == HTML_TAG_PREFIX && token.charAt(token.length() - 1) == HTML_TAG_POSTFIX && token.substring(1, token.length() - 1).equals(tag);
	}

	/**
	 * Marks the current javadoc range as immutable based on the current level.
	 * 
	 * @param current The javadoc range to mark
	 * @param level Hierarchical depth of layered immutable ranges
	 */
	private void markImmutableTag(JavaDocRange current, int level) {
		if (level > 0)
			current.setImmutable();
	}

	/**
	 * Marks immutable ranges in this javadoc line.
	 * 
	 * @param tag The html tag which confines the immutable ranges
	 * @param level Hierarchical depth of layered immutable ranges
	 * @return The new hierarchical depth of immutable ranges
	 */
	public int markImmutableTags(String tag, int level) {

		String token= null;
		JavaDocRange current= null;

		for (int range= 0; range < fRanges.size(); range++) {
			current= (JavaDocRange)fRanges.get(range);
			token= fRegion.getContent(current);

			if (hasOpeningTag(token, tag))
				markImmutableTag(current, level++);
			else if (hasClosingTag(token, tag))
				markImmutableTag(current, --level);
			else
				markImmutableTag(current, level);
		}
		return level;
	}

	/**
	 * Marks the javadoc range as root tag if it contains a root tag.
	 * 
	 * @param current Javadoc range to check for the root tag property
	 * @param token Token belonging to the javadoc range
	 */
	protected void markRootTag(JavaDocRange current, String token) {

		if (token.charAt(0) == JAVADOC_TAG_PREFIX) {
			for (int tag= 0; tag < JAVADOC_ROOT_TAGS.length; tag++) {
				if (token.equals(JAVADOC_ROOT_TAGS[tag])) {
					current.setRootTag();
					break;
				}
			}
		}
	}

	/**
	 * Marks the javadoc range as region separator if it contains a separator tag.
	 * 
	 * @param current Javadoc range to check for the separator property
	 * @param token Token belonging to the javadoc range
	 */
	protected void markSeparatorTag(JavaDocRange current, String token) {

		if (token.charAt(0) == HTML_TAG_PREFIX && token.charAt(token.length() - 1) == HTML_TAG_POSTFIX) {
			for (int tag= 0; tag < JAVADOC_SEPARATOR_TAGS.length; tag++) {
				if (hasOpeningTag(token, JAVADOC_SEPARATOR_TAGS[tag]) || hasClosingTag(token, JAVADOC_SEPARATOR_TAGS[tag])) {
					current.setSeparatorTag();
					break;
				}
			}
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentLine#tokenizeLine()
	 */
	public void tokenizeLine() {

		super.tokenizeLine();

		String token= null;
		JavaDocRange current= null;

		for (int range= 0; range < fRanges.size(); range++) {
			current= (JavaDocRange)fRanges.get(range);
			token= fRegion.getContent(current);

			markRootTag(current, token);
			markSeparatorTag(current, token);
		}
	}

	/*
	 * @see java.lang.Object#toString()
	 */
	public String toString() {

		// TODO remove

		JavaDocRange current= null;
		final StringBuffer buffer= new StringBuffer(32 * fRanges.size());

		for (int token= 0; token < fRanges.size(); token++) {
			current= (JavaDocRange)fRanges.get(token);
			buffer.append('{');
			buffer.append(current.isImmutable() ? 't' : 'f');
			buffer.append(current.isRootTag() ? 't' : 'f');
			buffer.append(current.isSeparatorTag() ? 't' : 'f');
			buffer.append('}');
			buffer.append(fRegion.getContent(current));
			buffer.append('~');
		}

		return buffer.toString();
	}
}
