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

import org.eclipse.jface.text.formatter.FormattingContext;

import org.eclipse.jdt.ui.PreferenceConstants;

/**
 * Formatting context for the comment formatter.
 * 
 * @since 3.0
 */
public class CommentFormattingContext extends FormattingContext {

	/*
	 * @see org.eclipse.jface.text.formatter.IFormattingContext#getPreferenceKeys()
	 */
	public String[] getPreferenceKeys() {
		return new String[] { 
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
}
