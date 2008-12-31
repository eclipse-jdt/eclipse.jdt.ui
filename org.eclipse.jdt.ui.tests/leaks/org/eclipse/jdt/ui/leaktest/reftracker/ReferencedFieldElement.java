/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	public ReferencedObject getReferenceHolder() {
		return fPrevious;
	}

	public Field getField() {
		return fField;
	}
}
