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

import org.eclipse.jdt.internal.ui.text.IJavaPartitions;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;

/**
 * Factory for comment related objects.
 * 
 * @since 3.0
 */
public class CommentObjectFactory {

	/**
	 * Creates a comment line for a specific comment region type.
	 * 
	 * @param region Comment region to create the line for
	 * @param range Range of the line in the underlying text store measured in comment region coordinates 
	 * @return A new comment line for the comment region, or <code>null</code> iff there is no line available for this comment type
	 */
	public static CommentLine getLine(CommentRegion region, IRegion range) {

		final String type= region.getType();
		final CommentRange comment= getRange(region, range.getOffset(), range.getLength());

		if (type.equals(IJavaPartitions.JAVA_DOC))
			return new JavaDocLine(region, comment);
		else if (type.equals(IJavaPartitions.JAVA_MULTI_LINE_COMMENT))
			return new MultiCommentLine(region, comment);
		else if (type.equals(IJavaPartitions.JAVA_SINGLE_LINE_COMMENT))
			return new SingleCommentLine(region, comment);

		return null;
	}

	/**
	 * Creates a comment range for a specific comment region type.
	 * 
	 * @param region Comment region to get the range from
	 * @param offset Offset of the range
	 * @param length Length of the range
	 * @return A new comment range for the comment region, or <code>null</code> iff there is no range available for this comment type
	 */
	public static CommentRange getRange(CommentRegion region, int offset, int length) {

		final String type= region.getType();

		if (type.equals(IJavaPartitions.JAVA_DOC))
			return new JavaDocRange(offset, length);

		return new CommentRange(offset, length);
	}

	/**
	 * Creates a comment region for a specific document partition type.
	 * 
	 * @param document Document to create the region for
	 * @param range Range of the comment region in the document
	 * @param delimiter Line delimiter to use in the comment region
	 * @return A new comment region for the comment region range in the document
	 */
	public static CommentRegion getRegion(IDocument document, ITypedRegion range, String delimiter) {

		final String type= range.getType();

		if (type.equals(IJavaPartitions.JAVA_DOC))
			return new JavaDocRegion(document, range, delimiter);

		return new CommentRegion(document, range, delimiter);
	}

	/*
	 * Not for instantiation.
	 */
	private CommentObjectFactory() {
		// Not for instantiation
	}
}
