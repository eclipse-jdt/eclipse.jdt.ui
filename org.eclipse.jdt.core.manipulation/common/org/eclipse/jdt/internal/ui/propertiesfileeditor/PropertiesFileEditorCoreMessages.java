/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.propertiesfileeditor;

import org.eclipse.osgi.util.NLS;


/**
 * Helper class to get NLSed messages.
 *
 * @since 3.1
 */
public final class PropertiesFileEditorCoreMessages extends NLS {
	private static final String BUNDLE_NAME= PropertiesFileEditorCoreMessages.class.getName();

	private PropertiesFileEditorCoreMessages() {
		// Do not instantiate
	}

	public static String PropertiesFileHover_MalformedEncoding;

	static {
		NLS.initializeMessages(BUNDLE_NAME, PropertiesFileEditorCoreMessages.class);
	}
}