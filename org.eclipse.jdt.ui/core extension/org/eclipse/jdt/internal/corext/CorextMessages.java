package org.eclipse.jdt.internal.corext;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class CorextMessages {

	private static final String BUNDLE_NAME= CorextMessages.class.getName();

	private static final ResourceBundle RESOURCE_BUNDLE =
		ResourceBundle.getBundle(BUNDLE_NAME);

	private CorextMessages() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}