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
 *
 */
public class ReferencedArrayElement extends ReferencedObject {

	private final ReferencedObject fPrevious;
	private final int fIndex;

	public ReferencedArrayElement(ReferencedObject previous, int index, Object value) {
		super(value);
		fPrevious= previous;
		fIndex= index;
	}

	@Override
	public ReferencedObject getReferenceHolder() {
		return fPrevious;
	}

	public int getIndex() {
		return fIndex;
	}

}
