/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.corext.refactoring.typeconstraints2;

/**
 * This interface is used to compare elements in a {@link CustomHashtable}for
 * equality, and to provide the hash code for an element. This allows the client
 * of the {@link CustomHashtable}to specify different equality criteria and a
 * different hash code implementation than the <code>equals</code> and
 * <code>hashCode</code> implementations of the elements themselves.
 * 
 * @see CustomHashtable
 */
public interface IElementComparer {

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