/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.internal.core.refactoring;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Utility class which helps managing the resources
 */
public class Resources {
	
	private static final String RESOURCE_BUNDLE= "org.eclipse.jdt.internal.core.refactoring.NLS";
	
	private static ResourceBundle fgResourceBundle= ResourceBundle.getBundle(RESOURCE_BUNDLE);
	
	private Resources(){
		// prevent instantiation of class
	}
	
	public static ResourceBundle getBundle() {
		return fgResourceBundle;
	}

	public static String getString(String key) {
		try {
			return fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";
		}
	}
		
	public static String getFormattedString(String key, String arg) {
		return MessageFormat.format(getString(key), new String[] { arg });
	}
	
	public static String getFormattedString(String key, String[] args) {
		return MessageFormat.format(getString(key), args);
	}	
}
