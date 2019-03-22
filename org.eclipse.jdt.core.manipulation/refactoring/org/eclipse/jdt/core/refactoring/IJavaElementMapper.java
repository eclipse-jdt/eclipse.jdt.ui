/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
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
package org.eclipse.jdt.core.refactoring;

import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;

import org.eclipse.jdt.core.IJavaElement;

/**
 * An <code>IJavaElementMapper</code> provides methods to map an original
 * elements to its refactored counterparts.
 * <p>
 * An <code>IJavaElementMapper</code> can be obtained via
 * {@link RefactoringProcessor#getAdapter(Class)}.
 * </p>
 *
 * @since 1.1
 */
public interface IJavaElementMapper {

	/**
	 * <p>
	 * Returns the refactored Java element for the given element.
	 * The returned Java element might not yet exist when the method
	 * is called.
	 * </p>
	 * <p>
	 * Note that local variables <strong>cannot</strong> be mapped
	 * using this method.
	 * </p>
	 *
	 * @param element the element to be refactored
	 *
	 * @return the refactored element for the given element
	 */
	IJavaElement getRefactoredJavaElement(IJavaElement element);
}