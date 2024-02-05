/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 262748 [jar exporter] extract constants for string literals in JarRsrcLoader et al.
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.jarpackagerfat;


/**
 * Constants used in the fat Jar packager.
 */
final class JIJConstants {
	/** duplicates org.eclipse.jdt.internal.jarinjarloader.JIJConstants.REDIRECTED_CLASS_PATH_MANIFEST_NAME */
	static final String REDIRECTED_CLASS_PATH_MANIFEST_NAME  = "Rsrc-Class-Path";  //$NON-NLS-1$
	/** duplicates org.eclipse.jdt.internal.jarinjarloader.JIJConstants.REDIRECTED_MAIN_CLASS_MANIFEST_NAME */
	static final String REDIRECTED_MAIN_CLASS_MANIFEST_NAME  = "Rsrc-Main-Class";  //$NON-NLS-1$
	static final String CURRENT_DIR                          = "./";  //$NON-NLS-1$

	/**
	 * This is <code>{@link org.eclipse.jdt.internal.jarinjarloader.JarRsrcLoader}.class.getName()</code>,
	 * but that's not visible for the PDE builder when building the org.eclipse.jdt.ui plug-in.
	 */
	static final String LOADER_MAIN_CLASS                    = "org.eclipse.jdt.internal.jarinjarloader.JarRsrcLoader";  //$NON-NLS-1$

	private JIJConstants() {
	}
}
