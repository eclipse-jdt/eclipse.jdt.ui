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

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;

/**
 * Javadoc region in a source code document.
 * 
 * @since 3.0
 */
public class JavaDocRegion extends CommentRegion {

	/** Javadoc immutable tags */
	public static final String[] JAVADOC_IMMUTABLE_TAGS= new String[] { "code", "pre" }; //$NON-NLS-1$ //$NON-NLS-2$

	/** Javadoc source tags */
	public static final String[] JAVADOC_SOURCE_TAGS= JAVADOC_IMMUTABLE_TAGS;

	/**
	 * Creates a new javadoc region.
	 * 
	 * @param document The document where this javadoc region belongs to
	 * @param region The typed region which forms this javadoc region
	 * @param delimiter The line delimiter to use in this javadoc region
	 */
	public JavaDocRegion(IDocument document, ITypedRegion region, String delimiter) {
		super(document, region, delimiter);
	}

	/**
	 * Finds immutable ranges in this javadoc region.
	 */
	protected void findImmutableRanges() {

		int level= 0;
		JavaDocLine current= null;

		for (int tag= 0; tag < JAVADOC_IMMUTABLE_TAGS.length; tag++) {

			level= 0;
			for (int line= 0; line < fLines.size(); line++) {
				current= (JavaDocLine)fLines.get(line);
				level= current.markImmutableTags(JAVADOC_IMMUTABLE_TAGS[tag], level);
			}
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.CommentRegion#formatRegion()
	 */
	public void formatRegion() {

		scanLines();
		tokenizeLines();
		findImmutableRanges();
		formatSourceRanges();

		// TODO remove
		System.out.print(this);
	}

	/**
	 * Formats source code ranges in this javadoc region.
	 */
	protected void formatSourceRanges() {

		int level= 0;
		JavaDocLine current= null;

		for (int tag= 0; tag < JAVADOC_SOURCE_TAGS.length; tag++) {

			level= 0;
			for (int line= 0; line < fLines.size(); line++) {
				current= (JavaDocLine)fLines.get(line);
				level= current.formatSourceRanges(JAVADOC_SOURCE_TAGS[tag], level);
			}
		}
	}
}
