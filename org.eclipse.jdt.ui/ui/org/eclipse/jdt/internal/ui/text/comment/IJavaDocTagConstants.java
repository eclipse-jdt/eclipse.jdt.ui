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
 * Javadoc tag constants.
 * 
 * @since 3.0
 */
public interface IJavaDocTagConstants extends ICommentTagConstants {

	/** Javadoc break tags */
	public static final String[] JAVADOC_BREAK_TAGS= new String[] { "dd", "dt", "li", "td", "th", "tr", "h1", "h2", "h3", "h4", "h5", "h6", "q" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$

	/** Javadoc single break tag */
	public static final String[] JAVADOC_SINGLE_BREAK_TAG= new String[] { "br" }; //$NON-NLS-1$
	
	/** Javadoc code tags */
	public static final String[] JAVADOC_CODE_TAGS= new String[] { "pre" }; //$NON-NLS-1$

	/** Javadoc immutable tags */
	public static final String[] JAVADOC_IMMUTABLE_TAGS= new String[] { "code", "em", "pre", "q", "tt" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

	/** Javadoc new line tags */
	public static final String[] JAVADOC_NEWLINE_TAGS= new String[] { "dd", "dt", "li", "td", "th", "tr", "h1", "h2", "h3", "h4", "h5", "h6", "q" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$

	/** Javadoc parameter tags */
	public static final String[] JAVADOC_PARAM_TAGS= new String[] { "@exception", "@param", "@serialField", "@throws" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

	/** Javadoc root tags */
	public static final String[] JAVADOC_ROOT_TAGS= new String[] { "@author", "@deprecated", "@return", "@see", "@serial", "@serialData", "@since", "@version", "@inheritDoc" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$

	/** Javadoc separator tags */
	public static final String[] JAVADOC_SEPARATOR_TAGS= new String[] {"dl", "hr", "nl", "p", "pre", "ul", "ol" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
}
