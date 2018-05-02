/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.manipulation;

import org.eclipse.osgi.util.NLS;

public class JavaElementLabelsMessages extends NLS {

	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsMessages";//$NON-NLS-1$

	private JavaElementLabelsMessages() {
		// Do not instantiate
	}

	public static String JavaElementLabels_default_package;
	public static String JavaElementLabels_anonym_type;
	public static String JavaElementLabels_anonym;
	public static String JavaElementLabels_import_container;
	public static String JavaElementLabels_initializer;
	public static String JavaElementLabels_category;
	public static String JavaElementLabels_concat_string;
	public static String JavaElementLabels_comma_string;
	public static String JavaElementLabels_declseparator_string;
	public static String JavaElementLabels_category_separator_string;
	public static String JavaElementLabels_onClassPathOf;

	static {
		NLS.initializeMessages(BUNDLE_NAME, JavaElementLabelsMessages.class);
	}

}
