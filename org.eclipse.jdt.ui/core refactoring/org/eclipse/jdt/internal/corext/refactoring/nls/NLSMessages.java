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
package org.eclipse.jdt.internal.corext.refactoring.nls;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class NLSMessages {

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
	
	public static String getFormattedString(String key, String[] args) {
		try{
			return MessageFormat.format(fgResourceBundle.getString(key), args);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}	
	}
	
	public static String getFormattedString(String key, String arg) {
		try{
			return MessageFormat.format(fgResourceBundle.getString(key), new String[] { arg });
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}	
	}	
}
