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

package org.eclipse.jsp;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Helper class to get NLSed messages.
 * 
 * @since 3.0
 */
class JspMessages {

	private static final String BUNDLE_NAME= JspMessages.class.getName();

	private static final ResourceBundle RESOURCE_BUNDLE= ResourceBundle
			.getBundle(BUNDLE_NAME);

	private JspMessages() {
	}

	public static String getString(String key) {
		// XXX Auto-generated method stub
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}