/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.jdt.ui.text.java;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 *
 * @since 3.1
 */
final class JavaTextMessages extends NLS {

	private static final String BUNDLE_NAME= JavaTextMessages.class.getName();

	private JavaTextMessages() {
		// Do not instantiate
	}

	public static String ResultCollector_anonymous_type;
	public static String ResultCollector_overridingmethod;

	static {
		NLS.initializeMessages(BUNDLE_NAME, JavaTextMessages.class);
	}
}