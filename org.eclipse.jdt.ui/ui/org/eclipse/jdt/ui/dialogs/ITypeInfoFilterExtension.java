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
 * A filter to select {@link ITypeInfoRequestor} objects.
 * <p>
 * The interface should be implemented by clients wishing to provide special
 * filtering to the type selection dialog.
 * </p>
 *
 * @since 3.2
 */
public interface ITypeInfoFilterExtension {

	/**
	 * Returns whether the given type makes it into the list or
	 * not.
	 *
	 * @param typeInfoRequestor the <code>ITypeInfoRequestor</code> to
	 *  used to access data for the type under inspection
	 *
	 * @return whether the type is selected or not
	 */
	boolean select(ITypeInfoRequestor typeInfoRequestor);

}
