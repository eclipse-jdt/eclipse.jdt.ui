/*******************************************************************************
 * Copyright (c) 2025 Daniel Schmid and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Daniel Schmid - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.text.folding;

import org.eclipse.core.runtime.preferences.IScopeContext;

/**
 * Extends {@link IJavaFoldingPreferenceBlock} for supporting preferences in a given scope (e.g. projects).
 * @since 3.34
 */
public interface IScopedJavaFoldingPreferenceBlock extends IJavaFoldingPreferenceBlock {
	/**
	 * Marks this preference block to be configured in an {@link IScopeContext}.
	 * @param context The scope context the preferences apply to.
	 */
	default void setScopeContext(IScopeContext context) {

	}
}
