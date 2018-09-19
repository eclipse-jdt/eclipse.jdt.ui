/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
