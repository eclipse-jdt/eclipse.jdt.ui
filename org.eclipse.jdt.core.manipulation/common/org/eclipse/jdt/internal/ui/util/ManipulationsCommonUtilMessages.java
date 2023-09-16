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
package org.eclipse.jdt.internal.ui.util;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 */
public final class ManipulationsCommonUtilMessages extends NLS {

	private static final String BUNDLE_NAME= ManipulationsCommonUtilMessages.class.getName();
	static {
		NLS.initializeMessages(BUNDLE_NAME, ManipulationsCommonUtilMessages.class);
	}
	private ManipulationsCommonUtilMessages() {
		// Do not instantiate
	}

	public static String BuildPathsBlock_operationdesc_project;
}
