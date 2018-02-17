/*******************************************************************************
 * Copyright (c) 2018 Angelo Zerr and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Angelo Zerr: initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor.codemining;

import org.eclipse.osgi.util.NLS;

/**
 * Helper class to get NLSed messages.
 */
final class JavaCodeMiningMessages extends NLS {

	private static final String BUNDLE_NAME= JavaCodeMiningMessages.class.getName();


	private JavaCodeMiningMessages() {
		// Do not instantiate
	}

	public static String JavaReferenceCodeMining_label;

	public static String JavaImplementationCodeMining_label;

	static {
		NLS.initializeMessages(BUNDLE_NAME, JavaCodeMiningMessages.class);
	}

}
