/*******************************************************************************
 * Copyright (c) 2026 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.text.folding;


/**
 * Extends {@link IJavaFoldingPreferenceBlock} to allow implementations to be notified when the
 * enablement state of the folding preferences on the Java &gt; Editor &gt; Folding preference page
 * changes.
 * <p>
 * Clients may implement this interface.
 * </p>
 *
 * @since 3.40
 */
public interface IJavaFoldingPreferenceBlock2 extends IJavaFoldingPreferenceBlock {

	/**
	 * Called when the enablement state of the folding preferences changes, for example when the
	 * checkbox controlling the containing preference page section is toggled. Implementations
	 * should enable or disable their controls accordingly.
	 *
	 * @param enabled <code>true</code> if the controls should be enabled, <code>false</code>
	 *            otherwise
	 */
	void updateEnablements(boolean enabled);

}
