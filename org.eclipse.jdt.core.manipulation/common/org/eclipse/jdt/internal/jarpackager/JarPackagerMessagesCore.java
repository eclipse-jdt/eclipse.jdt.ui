/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
 *     Microsoft Corporation - based this file on JarPackagerMessages and FatJarPackagerMessages
 *******************************************************************************/
package org.eclipse.jdt.internal.jarpackager;

import org.eclipse.osgi.util.NLS;

public class JarPackagerMessagesCore extends NLS {

	private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.jarpackager.JarPackagerMessagesCore";//$NON-NLS-1$

	public static String FatJarBuilder_error_readingArchiveFile;

	public static String JarWriter_writeProblem;

	public static String JarWriter_writeProblemWithMessage;

	static {
		NLS.initializeMessages(BUNDLE_NAME, JarPackagerMessagesCore.class);
	}

	private JarPackagerMessagesCore() {
		// Do not instantiate
	}
}
