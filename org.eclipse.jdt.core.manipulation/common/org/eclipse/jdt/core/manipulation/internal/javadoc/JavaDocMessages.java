/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package org.eclipse.jdt.core.manipulation.internal.javadoc;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 */
final class JavaDocMessages extends NLS {

	private static final String BUNDLE_NAME= JavaDocMessages.class.getName();

	private JavaDocMessages() {
		// Do not instantiate
	}

	public static String JavaDoc2HTMLTextReader_parameters_section;

	public static String JavaDoc2HTMLTextReader_returns_section;

	public static String JavaDoc2HTMLTextReader_throws_section;

	public static String JavaDoc2HTMLTextReader_type_parameters_section;

	public static String JavaDoc2HTMLTextReader_author_section;

	public static String JavaDoc2HTMLTextReader_deprecated_section;

	public static String JavaDoc2HTMLTextReader_method_in_type;

	public static String JavaDoc2HTMLTextReader_overrides_section;

	public static String JavaDoc2HTMLTextReader_see_section;

	public static String JavaDoc2HTMLTextReader_since_section;

	public static String JavaDoc2HTMLTextReader_specified_by_section;

	public static String JavaDoc2HTMLTextReader_version_section;

	public static String JavaDoc2HTMLTextReader_api_note;

	public static String JavaDoc2HTMLTextReader_impl_spec;

	public static String JavaDoc2HTMLTextReader_impl_note;

	public static String JavaDoc2HTMLTextReader_uses;

	public static String JavaDoc2HTMLTextReader_provides;

	public static String JavadocContentAccess2_getproperty_message;

	public static String JavadocContentAccess2_setproperty_message;

	public static String JavadocContentAccess2_returns_pre;

	public static String JavadocContentAccess2_returns_post;

	static {
		NLS.initializeMessages(BUNDLE_NAME, JavaDocMessages.class);
	}
}
