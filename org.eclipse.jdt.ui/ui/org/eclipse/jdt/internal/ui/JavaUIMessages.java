/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class JavaUIMessages {

	private static final String RESOURCE_BUNDLE= "org.eclipse.jdt.internal.ui.JavaUIMessages";//$NON-NLS-1$

	private static ResourceBundle fgResourceBundle= ResourceBundle.getBundle(RESOURCE_BUNDLE);

	private JavaUIMessages() {
	}

	public static String getString(String key) {
		try {
			return fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
		}
	}
	
	public static String getFormattedString(String key, String arg) {
		try{
			return MessageFormat.format(fgResourceBundle.getString(key), new String[] { arg });
		} catch (MissingResourceException e) {
			return "!" + key + "!";//$NON-NLS-2$ //$NON-NLS-1$
		}	
	}
	
}
