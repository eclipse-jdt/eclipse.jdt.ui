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

package org.eclipse.jdt.internal.corext.text.comment;

import java.util.Map;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;

/**
 * Factory for comment related objects.
 * <p>
 * Use this factory to create comment objects specific to a certain comment
 * type.
 * </p>
 * 
 * @since 3.0
 */
public class CommentObjectFactory {

	// TODO: move to CodeFormatter
	/**
	 * Kind used to format single-line comments
	 */
	public static final int K_SINGLE_LINE_COMMENT= 0x10;
	/**
	 * Kind used to format multi-line comments
	 */
	public static final int K_MULTI_LINE_COMMENT= 0x20;
	/**
	 * Kind used to format a Javadoc comments
	 */
	public static final int K_JAVA_DOC= 0x40;

	/**
	 * Creates a comment region for a specific document partition type.
	 * 
	 * @param kind the comment snippet kind
	 * @param document The document which contains the comment region
	 * @param range Range of the comment region in the document
	 * @param delimiter Line delimiter to use in the comment region
	 * @param preferences The preferences to use
	 * @param textMeasurement The text measurement. Can be <code>null</code>.
	 * @return A new comment region for the comment region range in the
	 *         document
	 */
	public static CommentRegion createRegion(int kind, IDocument document, Position range, String delimiter, Map preferences, ITextMeasurement textMeasurement) {
		switch (kind) {
			case K_SINGLE_LINE_COMMENT:
				return new CommentRegion(document, range, delimiter, preferences, textMeasurement);
			case K_MULTI_LINE_COMMENT:
				return new MultiCommentRegion(document, range, delimiter, preferences, textMeasurement);
			case K_JAVA_DOC:
				return new JavaDocRegion(document, range, delimiter, preferences, textMeasurement);
			default:
				return null;
		}
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
