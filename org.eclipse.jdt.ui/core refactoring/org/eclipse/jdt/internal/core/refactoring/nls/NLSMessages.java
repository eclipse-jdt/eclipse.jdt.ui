/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.nls;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

class NLSMessages {

	private static final String RESOURCE_BUNDLE= NLSMessages.class.getName();

	private static ResourceBundle fgResourceBundle= ResourceBundle.getBundle(RESOURCE_BUNDLE);

	private NLSMessages() {
	}

	public static String getString(String key) {
		try {
			return fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
