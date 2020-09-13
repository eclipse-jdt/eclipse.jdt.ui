/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
package org.eclipse.jdt.ui.cleanup;


/**
 * Initializes the default options for a clean up kind.
 *
 * @since 3.5
 */
public interface ICleanUpOptionsInitializer {

	/**
	 * Sets the default options of this initializer.
	 *
	 * @param options the clean up options
	 */
	void setDefaultOptions(CleanUpOptions options);
}
