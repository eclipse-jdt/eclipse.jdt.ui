/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.comments;

import java.util.Map;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.TypedPosition;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.text.comment.CommentObjectFactory;
import org.eclipse.jdt.internal.corext.text.comment.CommentRegion;
import org.eclipse.jdt.internal.corext.text.comment.ITextMeasurement;

import org.eclipse.jdt.internal.ui.text.IJavaPartitions;

/**
 * Utilities for the comment formatter.
 * 
 * @since 3.0
 */
public class CommentFormatterUtil {

	/**
	 * Formats the source string as a comment region of the specified type.
	 * <p>
	 * Both offset and length must denote a valid comment partition, that is to
	 * say a substring that starts and ends with the corresponding comment
	 * delimiter tokens.
	 * 
	 * @param type
	 *                   The type of the comment. Must be one of the constants of
	 *                   <code>IJavaPartitions</code>
	 * @param source
	 *                   The source string to format
	 * @param offset
	 *                   The offset relative to the source string where to format
	 * @param length
	 *                   The length of the region in the source string to format
	 * @param preferences
	 *                   Preferences for the comment formatter
	 * @param textMeasurement
	 *                   Optional text measurement for font specific formatting. Can be
	 *                   <code>null</code>.
	 * @return The text edit of the formatting process
	 */
	public static TextEdit format(String type, String source, int offset, int length, Map preferences, ITextMeasurement textMeasurement) {
		Assert.isTrue(IJavaPartitions.JAVA_DOC.equals(type) || IJavaPartitions.JAVA_MULTI_LINE_COMMENT.equals(type) || IJavaPartitions.JAVA_SINGLE_LINE_COMMENT.equals(type));

		Assert.isNotNull(source);
		Assert.isNotNull(preferences);

		Assert.isTrue(offset >= 0);
		Assert.isTrue(length <= source.length());

		final IDocument document= new Document(source);
		final TypedPosition position= new TypedPosition(offset, length, type);

		final CommentRegion region= CommentObjectFactory.createRegion(document, position, TextUtilities.getDefaultLineDelimiter(document), preferences, textMeasurement);

		return region.format();
	}
}
