/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.nls;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

class Messages {

	private static final String RESOURCE_BUNDLE= "org.eclipse.jdt.internal.ui.nls.nls"; //$NON-NLS-1$

	private static ResourceBundle fgResourceBundle= ResourceBundle.getBundle(RESOURCE_BUNDLE);

		public static String getString(String key) {
		try {
			return fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
