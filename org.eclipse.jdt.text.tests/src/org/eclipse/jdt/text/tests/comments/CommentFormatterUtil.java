/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.comments;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jdt.internal.ui.text.comment.CommentFormattingContext;
import org.eclipse.jdt.internal.ui.text.comment.CommentFormattingStrategy;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.formatter.FormattingContext;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;

/**
 * Utilities for the comment formatter.
 * 
 * @since 3.0
 */
public class CommentFormatterUtil {

	/**
	 * Formats the source string as a comment region of the specified type.
	 * <p>
	 * Both offset and length must denote a valid comment partition, that is
	 * to say a substring that starts and ends with the corresponding
	 * comment delimiter tokens.
	 * 
	 * @param type the type of the comment, must be one of the constants of
	 *                <code>IJavaPartitions</code>
	 * @param source the source string to format
	 * @param offset the offset relative to the source string where to
	 *                format
	 * @param length the length of the region in the source string to format
	 * @param preferences preferences for the comment formatter
	 * @return the formatted source string
	 */
	public static String format(String type, String source, int offset, int length, Map preferences) {
		Assert.isTrue(IJavaPartitions.JAVA_DOC.equals(type) || IJavaPartitions.JAVA_MULTI_LINE_COMMENT.equals(type) || IJavaPartitions.JAVA_SINGLE_LINE_COMMENT.equals(type));

		Assert.isNotNull(source);
		Assert.isNotNull(preferences);

		Assert.isTrue(offset >= 0);
		Assert.isTrue(length <= source.length());

		final IDocument document= new Document(source);

		IFormattingContext context= new CommentFormattingContext();
		context.setProperty(FormattingContextProperties.CONTEXT_PREFERENCES, preferences);
		context.setProperty(FormattingContextProperties.CONTEXT_DOCUMENT, Boolean.TRUE);
		context.setProperty(FormattingContextProperties.CONTEXT_MEDIUM, document);
		context.setProperty(FormattingContextProperties.CONTEXT_PARTITION, new TypedPosition(offset, length, type));
		
		CommentFormattingStrategy strategy= new CommentFormattingStrategy();
		strategy.formatterStarts(context);
		strategy.format();
		strategy.formatterStops();
		
		return document.get();
	}

	/**
	 * Creates a formatting options with all default options and the given custom user options.
	 * 
	 * @param user the custom user options
	 * @return the formatting options
	 * @since 3.1
	 */
	public static Map createOptions(Map user) {
		Map map= new HashMap();
		FormattingContext context= new CommentFormattingContext();
		String[] keys= context.getPreferenceKeys();
	
		for (int index= 0; index < keys.length; index++) {
	
			if (context.isBooleanPreference(keys[index]))
				map.put(keys[index], IPreferenceStore.TRUE);
			else if (context.isIntegerPreference(keys[index]))
				map.put(keys[index], "80"); //$NON-NLS-1$
		}
	
		if (user != null) {
	
			Object key= null;
			for (final Iterator iterator= user.keySet().iterator(); iterator.hasNext();) {
	
				key= iterator.next();
				map.put(key, user.get(key));
			}
		}
		return map;
	}
}
