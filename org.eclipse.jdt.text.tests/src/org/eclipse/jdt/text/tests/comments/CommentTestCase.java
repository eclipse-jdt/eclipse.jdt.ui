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
package org.eclipse.jdt.text.tests.comments;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

import org.eclipse.jdt.internal.ui.text.IJavaPartitions;
import org.eclipse.jdt.internal.ui.text.comment.CommentFormattingContext;

public abstract class CommentTestCase extends TestCase {

	public static final String DELIMITER= TextUtilities.getDefaultLineDelimiter(new Document());

	protected CommentTestCase(String name) {
		super(name);
	}

	protected final Map createOptions(CommentFormattingContext context) {
		assertNotNull(context);

		final Map map= new HashMap();
		final String[] keys= context.getPreferenceKeys();

		for (int index= 0; index < keys.length; index++) {

			if (context.isBooleanPreference(keys[index]))
				map.put(keys[index], IPreferenceStore.TRUE);
			else if (context.isIntegerPreference(keys[index]))
				map.put(keys[index], "80"); //$NON-NLS-1$
		}

		final Map user= getUserOptions();
		if (user != null) {

			Object key= null;
			for (final Iterator iterator= user.keySet().iterator(); iterator.hasNext();) {

				key= iterator.next();
				map.put(key, user.get(key));
			}
		}
		return map;
	}

	protected abstract String getCommentType();

	protected abstract Map getUserOptions();

	protected final String testFormat(String text) {
		return testFormat(text, 0, text.length());
	}

	protected String testFormat(String text, int offset, int length) {
		assertNotNull(text);
		assertTrue(offset >= 0);
		assertTrue(offset < text.length());
		assertTrue(length >= 0);
		assertTrue(offset + length <= text.length());

		final String type= getCommentType();
		assertNotNull(type);
		assertTrue(type.equals(IJavaPartitions.JAVA_DOC) || type.equals(IJavaPartitions.JAVA_MULTI_LINE_COMMENT) || type.equals(IJavaPartitions.JAVA_SINGLE_LINE_COMMENT));

		final CommentFormattingContext context= new CommentFormattingContext();
		final TextEdit edit= CommentFormatterUtil.format(type, text, offset, length, createOptions(context), null);

		return CodeFormatterUtil.evaluateFormatterEdit(text, edit, null);
	}
}
