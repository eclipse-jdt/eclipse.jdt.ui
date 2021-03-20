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

package org.eclipse.jdt.internal.ui;


import org.eclipse.jface.viewers.IBasicPropertyConstants;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import org.eclipse.jdt.core.IJavaElement;

public class JavaElementProperties implements IPropertySource {

	private IJavaElement fSource;

	// Property Descriptors
	private static final IPropertyDescriptor[] fgPropertyDescriptors= new IPropertyDescriptor[1];
	static {
		PropertyDescriptor descriptor;

		// resource name
		descriptor= new PropertyDescriptor(IBasicPropertyConstants.P_TEXT, JavaUIMessages.JavaElementProperties_name);
		descriptor.setAlwaysIncompatible(true);
		fgPropertyDescriptors[0]= descriptor;
	}

	public JavaElementProperties(IJavaElement source) {
		fSource= source;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		return fgPropertyDescriptors;
	}

	@Override
	public Object getPropertyValue(Object name) {
		if (IBasicPropertyConstants.P_TEXT.equals(name)) {
			return fSource.getElementName();
		}
		return null;
	}

	@Override
	public void setPropertyValue(Object name, Object value) {
	}

	@Override
	public Object getEditableValue() {
		return this;
	}

	@Override
	public boolean isPropertySet(Object property) {
		return false;
	}

	@Override
	public void resetPropertyValue(Object property) {
	}
}
