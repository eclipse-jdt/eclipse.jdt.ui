/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
package org.eclipse.jdt.jeview.properties;

import org.eclipse.ui.views.properties.PropertyDescriptor;

import org.eclipse.jdt.core.JavaModelException;

public abstract class GenericProperty<T> {

	private final Class<T> fType;
	private final String fName;
	private final String fId;
	private final PropertyDescriptor fDescriptor;

	public GenericProperty(Class<T> type, String name) {
		fType= type;
		fName= name;
		fId= "org.eclipse.jdt.jeview." + type.getSimpleName() + "." + name;
		fDescriptor= new PropertyDescriptor(fId, fName);
		fDescriptor.setAlwaysIncompatible(true);
		fDescriptor.setCategory(type.getSimpleName());
	}

	public abstract Object compute(T element) throws JavaModelException;


	public Class<T> getType() {
		return fType;
	}


	public String getName() {
		return fName;
	}

	public String getId() {
		return fId;
	}

	public PropertyDescriptor getDescriptor() {
		return fDescriptor;
	}

}