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


package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.CustomProfile;


public class ProfileVersioner {
	
	public static final int VERSION_1= 1; // < 20040113 (incl. M6)
	
	/**
	 * Profile version
	 */
	public static final int CURRENT_VERSION= 2;
	

	public static void updateAndComplete(CustomProfile profile) {
		final Map oldSettings= profile.getSettings();
		final Map newSettings= ProfileManager.getDefaultSettings();
		
		switch (profile.getVersion()) {

		/**
		 * Insert updating code here without using breaks, in order to get updates
		 * across several version numbers.
		 */
		case VERSION_1:
			checkAndReplace(oldSettings, 
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_WITHIN_MESSAGE_SEND,	
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_MESSAGE_SEND,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_MESSAGE_SEND);
			
			checkAndReplace(oldSettings, 
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPEN_PAREN_IN_PARENTHESIZED_EXPRESSION,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION);
			
			checkAndReplace(oldSettings, 
				JavaCore.PLUGIN_ID + ".formatter.inset_space_between_empty_arguments", //$NON-NLS-1$
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BETWEEN_EMPTY_ARGUMENTS);
			
			checkAndReplace(oldSettings, 
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_METHOD_DECLARATION_OPEN_PAREN,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CONSTRUCTOR_DECLARATION_OPEN_PAREN);
			
			checkAndReplace(oldSettings, 
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_WITHIN_MESSAGE_SEND,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_MESSAGE_SEND,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_PAREN_IN_MESSAGE_SEND);
			
			checkAndReplace(oldSettings, 
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPEN_PAREN_IN_PARENTHESIZED_EXPRESSION,
				DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION);
		default:
			for (final Iterator iter= oldSettings.keySet().iterator(); iter.hasNext(); ) {
				final String key= (String)iter.next();
				if (!newSettings.containsKey(key)) 
				    continue;
				
				final String value= (String)oldSettings.get(key);
				if (value != null) {
				    newSettings.put(key, value);
				}
			}
		}
		profile.setVersion(CURRENT_VERSION);
		profile.setSettings(newSettings);
	}
	
	public static int getVersionStatus(CustomProfile profile) {
		final int version= profile.getVersion();
		if (version < CURRENT_VERSION) 
			return -1;
		else if (version > CURRENT_VERSION)
			return 1;
		else 
			return 0;
	}
	
	
	private static void checkAndReplace(Map settings, String oldKey, String newKey) {
		checkAndReplace(settings, oldKey, new String [] {newKey});
	}
	
	private static void checkAndReplace(Map settings, String oldKey, String newKey1, String newKey2) {
		checkAndReplace(settings, oldKey, new String [] {newKey1, newKey2});
	}

	private static void checkAndReplace(Map settings, String oldKey, String [] newKeys) {
		if (!settings.containsKey(oldKey)) 
			return;
		
		final String value= (String)settings.get(oldKey);

		if (value == null) 
			return;
		
		for (int i = 0; i < newKeys.length; i++) {
			settings.put(newKeys[i], value);
		}
	}
}
