/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.nls;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

class NLSUIMessages {

	private static final String RESOURCE_BUNDLE= NLSUIMessages.class.getName();

	private static ResourceBundle fgResourceBundle= ResourceBundle.getBundle(RESOURCE_BUNDLE);

	private NLSUIMessages() {
	}
	
	public static String getString(String key) {
		try {
			return fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
