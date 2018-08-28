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

import java.lang.reflect.Field;


/**
 *
 */
public class ReferencedFieldElement extends ReferencedObject {

	private final ReferencedObject fPrevious;
	private final Field fField;

	public ReferencedFieldElement(ReferencedObject previous, Field field, Object value) {
		super(value);
		fPrevious= previous;
		fField= field;
	}

	@Override
	public ReferencedObject getReferenceHolder() {
		return fPrevious;
	}

	public Field getField() {
		return fField;
	}
}
