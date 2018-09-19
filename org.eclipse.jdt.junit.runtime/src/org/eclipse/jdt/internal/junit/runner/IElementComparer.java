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
 *     David Saff (saff@mit.edu) - bug 102632: [JUnit] Support for JUnit 4.
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.runner;

/**
 * This is a copy of IElementComparer from org.eclipse.jface.viewers
 */
public /* package */ interface IElementComparer {

	/**
	 * Compares two elements for equality
	 *
	 * @param a the first element
	 * @param b the second element
	 * @return whether a is equal to b
	 */
	boolean equals(Object a, Object b);

	/**
	 * Returns the hash code for the given element.
	 *
	 * @param element the element
	 * @return the hash code for the given element
	 */
	int hashCode(Object element);
}
