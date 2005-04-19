/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.javadoc;

import org.eclipse.osgi.util.NLS;

public final class JavaDocMessages extends NLS {

	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.ui.text.javadoc.JavaDocMessages";//$NON-NLS-1$

	private JavaDocMessages() {
		// Do not instantiate
	}

	public static String JavaDocAccess_error_no_library;
	public static String DocletPageBuffer_colon;
	public static String DocletPageBuffer_method;
	public static String DocletPageBuffer_type_description;
	public static String CompletionEvaluator_default_package;
	public static String JavaDoc2HTMLTextReader_parameters_section;
	public static String JavaDoc2HTMLTextReader_returns_section;
	public static String JavaDoc2HTMLTextReader_throws_section;
	public static String JavaDoc2HTMLTextReader_see_section;

	static {
		NLS.initializeMessages(BUNDLE_NAME, JavaDocMessages.class);
	}
}