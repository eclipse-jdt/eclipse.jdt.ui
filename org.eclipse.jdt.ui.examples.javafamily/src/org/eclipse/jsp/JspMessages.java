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
package org.eclipse.jsp;

import org.eclipse.osgi.util.NLS;

public final class JspMessages extends NLS {

	private static final String BUNDLE_NAME= "org.eclipse.jsp.JspMessages";//$NON-NLS-1$

	private JspMessages() {
		// Do not instantiate
	}

	public static String RenameTypeParticipant_name;

	static {
		NLS.initializeMessages(BUNDLE_NAME, JspMessages.class);
	}
}