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

package org.eclipse.jdt.ui.leaktest.reftracker;

/**
 * Represents an object that is referenced by an other object.
 */
public abstract class ReferencedObject {

	private final Object fReferenced;

	public ReferencedObject(Object object) {
		fReferenced= object;
	}

	/**
	 * Returns the instance that keeps a reference to the current object or <code>null</code>
	 * if the element is a root element
	 * @return returns the instance that keeps a reference to the current object or <code>null</code>
	 * if the element is a root element
	 */
	public abstract ReferencedObject getReferenceHolder();

	/**
	 * Returns the object
	 * @return returns the object
	 */
	public Object getValue() {
		return fReferenced;
	}
}
