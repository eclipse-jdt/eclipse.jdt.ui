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
package org.eclipse.jdt.internal.ui.text;


/**
 * Mix-in for any rule that changes its behavior based on the Java source
 * version.
 *
 * @since 3.1
 */
public interface ISourceVersionDependent {

	/**
	 * Sets the configured java source version to one of the
	 * <code>JavaCore.VERSION_X_Y</code> values.
	 *
	 * @param version the new java source version
	 * @see org.eclipse.jdt.core.JavaCore
	 */
	void setSourceVersion(String version);
}
