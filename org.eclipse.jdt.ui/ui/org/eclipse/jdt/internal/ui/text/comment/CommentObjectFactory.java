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

import org.eclipse.jface.text.TypedPosition;

import org.eclipse.jdt.internal.ui.text.IJavaPartitions;

/**
 * Factory for comment related objects.
 * 
 * @since 3.0
 */
public class CommentObjectFactory {

	/**
	 * Creates a comment line for a specific comment region.
	 * 
	 * @param region
	 *                  Comment region to create the line for
	 * @return A new comment line for the comment region, or <code>null</code>
	 *               iff there is no line available for this comment type
	 */
	public static CommentLine createLine(final CommentRegion region) {

		final String type= region.getType();

		if (type.equals(IJavaPartitions.JAVA_DOC))
			return new JavaDocLine(region);
		else if (type.equals(IJavaPartitions.JAVA_MULTI_LINE_COMMENT))
			return new MultiCommentLine(region);
		else if (type.equals(IJavaPartitions.JAVA_SINGLE_LINE_COMMENT))
			return new SingleCommentLine(region);

		return null;
	}

	/**
	 * Creates a comment region for a specific document partition type.
	 * 
	 * @param strategy
	 *                  The comment formatting strategy used to format this comment
	 *                  region
	 * @param range
	 *                  Range of the comment region in the document
	 * @param delimiter
	 *                  Line delimiter to use in the comment region
	 * @return A new comment region for the comment region range in the
	 *               document
	 */
	public static CommentRegion createRegion(final CommentFormattingStrategy strategy, final TypedPosition range, final String delimiter) {

		final String type= range.getType();

		if (type.equals(IJavaPartitions.JAVA_DOC))
			return new JavaDocRegion(strategy, range, delimiter);
		else if (type.equals(IJavaPartitions.JAVA_MULTI_LINE_COMMENT))
			return new MultiCommentRegion(strategy, range, delimiter);

		return new CommentRegion(strategy, range, delimiter);
	}

	/**
	 * This class is not intended for instantiation.
	 * <p>
	 * Use the factory methods to create comment object instances.
	 */
	private CommentObjectFactory() {
		// Not for instantiation
	}
}
