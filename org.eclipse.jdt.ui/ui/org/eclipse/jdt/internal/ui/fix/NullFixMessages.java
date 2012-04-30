/*******************************************************************************
 * Copyright (c) 2011, 2012 GK Software AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephan Herrmann <stephan@cs.tu-berlin.de> - [quick fix] Add quick fixes for null annotations - https://bugs.eclipse.org/337977
 *     IBM Corporation - bug fixes
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import org.eclipse.osgi.util.NLS;

public class NullFixMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.jdt.internal.ui.fix.NullFixMessages"; //$NON-NLS-1$
	
	
	public static String NullAnnotationsCleanUp_add_nullable_annotation;
	public static String NullAnnotationsCleanUp_add_nonnull_annotation;
	
	public static String QuickFixes_add_annotation_change_name;
	public static String QuickFixes_change_method_parameter_nullness;
	public static String QuickFixes_change_method_return_nullness;
	public static String QuickFixes_change_overridden_parameter_nullness;
	public static String QuickFixes_change_overridden_return_nullness;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, NullFixMessages.class);
	}

	private NullFixMessages() {
	}
}
