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

package org.eclipse.jdt.internal.ui.text.comment;

import java.util.Map;

import org.eclipse.jface.text.formatter.FormattingContext;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.text.comment.CommentFormatterPreferenceConstants;

/**
 * Formatting context for the comment formatter.
 * 
 * @since 3.0
 */
public class CommentFormattingContext extends FormattingContext {
	
	/**
	 * Preference keys of this context's preferences.
	 * @since 3.1
	 */
	private static final String[] PREFERENCE_KEYS= new String[] { 
		    PreferenceConstants.FORMATTER_COMMENT_FORMAT, 
		    PreferenceConstants.FORMATTER_COMMENT_FORMATHEADER, 
		    PreferenceConstants.FORMATTER_COMMENT_FORMATSOURCE, 
		    PreferenceConstants.FORMATTER_COMMENT_INDENTPARAMETERDESCRIPTION, 
		    PreferenceConstants.FORMATTER_COMMENT_INDENTROOTTAGS, 
		    PreferenceConstants.FORMATTER_COMMENT_NEWLINEFORPARAMETER, 
		    PreferenceConstants.FORMATTER_COMMENT_SEPARATEROOTTAGS, 
		    PreferenceConstants.FORMATTER_COMMENT_LINELENGTH, 
		    PreferenceConstants.FORMATTER_COMMENT_CLEARBLANKLINES, 
		    PreferenceConstants.FORMATTER_COMMENT_FORMATHTML };
	
	/**
	 * Mapped preference keys of this context's preferences.
	 * @since 3.1
	 */
	private static final String[] MAPPED_PREFERENCE_KEYS= new String[] { 
		    CommentFormatterPreferenceConstants.FORMATTER_COMMENT_FORMAT, 
		    CommentFormatterPreferenceConstants.FORMATTER_COMMENT_FORMATHEADER, 
		    CommentFormatterPreferenceConstants.FORMATTER_COMMENT_FORMATSOURCE, 
		    CommentFormatterPreferenceConstants.FORMATTER_COMMENT_INDENTPARAMETERDESCRIPTION, 
		    CommentFormatterPreferenceConstants.FORMATTER_COMMENT_INDENTROOTTAGS, 
		    CommentFormatterPreferenceConstants.FORMATTER_COMMENT_NEWLINEFORPARAMETER, 
		    CommentFormatterPreferenceConstants.FORMATTER_COMMENT_SEPARATEROOTTAGS, 
		    CommentFormatterPreferenceConstants.FORMATTER_COMMENT_LINELENGTH, 
		    CommentFormatterPreferenceConstants.FORMATTER_COMMENT_CLEARBLANKLINES, 
		    CommentFormatterPreferenceConstants.FORMATTER_COMMENT_FORMATHTML };

	/*
	 * @see org.eclipse.jface.text.formatter.IFormattingContext#getPreferenceKeys()
	 */
	public String[] getPreferenceKeys() {
		return PREFERENCE_KEYS;
	}

	
	/*
	 * @see org.eclipse.jface.text.formatter.IFormattingContext#isBooleanPreference(java.lang.String)
	 */
	public boolean isBooleanPreference(String key) {
		return !key.equals(PreferenceConstants.FORMATTER_COMMENT_LINELENGTH);
	}

	/*
	 * @see org.eclipse.jface.text.formatter.IFormattingContext#isIntegerPreference(java.lang.String)
	 */
	public boolean isIntegerPreference(String key) {
		return key.equals(PreferenceConstants.FORMATTER_COMMENT_LINELENGTH);
	}

	/**
	 * Map from JDT/Text preference keys to JDT/Core preference keys.
	 * <p>
	 * TODO: remove after migrating comment formatter preferences to
	 * JDT/Core
	 * </p>
	 * 
	 * @param preferences the JDT/Text preferences
	 * @return the JDT/Core preferences
	 * @since 3.1
	 */
	public static Map mapOptions(Map preferences) {
		// TODO: stop modifying the original map
		String[] keys= PREFERENCE_KEYS;
		int n= keys.length;
		Object[] values= new Object[n];
		for (int i= 0; i < n; i++)
			values[i]= preferences.get(keys[i]);
		String[] remapedKeys= MAPPED_PREFERENCE_KEYS;
		for (int i= 0; i < n; i++)
			preferences.put(remapedKeys[i], values[i]);
		return preferences;
	}
}
