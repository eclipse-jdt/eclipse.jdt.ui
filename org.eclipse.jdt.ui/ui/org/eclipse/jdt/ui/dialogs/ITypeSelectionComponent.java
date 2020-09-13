/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.jdt.ui.dialogs;

/**
 * Interface to access the type selection component hosting a
 * type selection extension.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 * @since 3.2
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ITypeSelectionComponent {

	/**
	 * Triggers a search inside the type component with the
	 * current settings.
	 */
	void triggerSearch();
}