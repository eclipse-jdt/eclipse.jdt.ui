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
 * An interfaces to give access to the type presented in type
 * selection dialogs like the open type dialog.
 * <p>
 * Please note that <code>ITypeInfoRequestor</code> objects <strong>don't
 * </strong> have value semantic. The state of the object might change over
 * time especially since objects are reused for different call backs.
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * @since 3.2
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ITypeInfoRequestor {

	/**
	 * Returns the type's modifiers. The modifiers can be
	 * inspected using the class {@link org.eclipse.jdt.core.Flags}.
	 *
	 * @return the type's modifiers
	 */
	int getModifiers();

	/**
	 * Returns the type name.
	 *
	 * @return the info's type name.
	 */
	String getTypeName();

	/**
	 * Returns the package name.
	 *
	 * @return the info's package name.
	 */
	String getPackageName();

	/**
	 * Returns a dot separated string of the enclosing types or an
	 * empty string if the type is a top level type.
	 *
	 * @return a dot separated string of the enclosing types
	 */
	String getEnclosingName();
}