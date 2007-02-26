/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java.hover;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 */
final class JavaHoverMessages extends NLS {

	private static final String BUNDLE_NAME= JavaHoverMessages.class.getName();

	private JavaHoverMessages() {
		// Do not instantiate
	}

	public static String JavadocHover_noAttachedInformation;
	public static String JavadocHover_noAttachedJavadocInformation;
	public static String JavadocHover_noAttachedSourceInformation;
	public static String JavadocHover_noInformation;
	public static String JavaTextHover_createTextHover;
	public static String JavaTextHover_makeStickyHint;
	public static String NoBreakpointAnnotation_addBreakpoint;
	public static String NLSStringHover_NLSStringHover_missingKeyWarning;
	public static String NLSStringHover_NLSStringHover_PropertiesFileNotDetectedWarning;

	static {
		NLS.initializeMessages(BUNDLE_NAME, JavaHoverMessages.class);
	}
}
