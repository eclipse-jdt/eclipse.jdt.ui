/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring;

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.text.MessageFormat;

public class RefactoringMessages {

	private static final String RESOURCE_BUNDLE= "org.eclipse.jdt.internal.ui.refactoring.refactoringui";//$NON-NLS-1$

	private static ResourceBundle fgResourceBundle= ResourceBundle.getBundle(RESOURCE_BUNDLE);

	private RefactoringMessages() {
	}

	public static String getString(String key) {
		try {
			return fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
		}
	}
	
	public static String[] getStrings(String keys[]) {
		String[] result= new String[keys.length];
		for (int i= 0; i < keys.length; i++) {
			result[i]= getString(keys[i]);
		}
		return result;
	}
	
	public static String getFormattedString(String key, String arg) {
		return getFormattedString(key, new String[] { arg });
	}
	
	public static String getFormattedString(String key, String[] args) {
		return MessageFormat.format(getString(key), args);	
	}	
}
