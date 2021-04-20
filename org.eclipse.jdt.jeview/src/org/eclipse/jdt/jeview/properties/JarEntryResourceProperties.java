/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
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

import java.util.ArrayList;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import org.eclipse.jdt.core.IJarEntryResource;

public class JarEntryResourceProperties implements IPropertySource {

	private static final String C_JAR_ENTRY_RESOURCE= "IJarEntryResource";

	private static final String P_NAME= "org.eclipse.jdt.jeview.IJarEntryResource.name";
	private static final String P_FULL_PATH= "org.eclipse.jdt.jeview.IJarEntryResource.fullPath";
	private static final String P_IS_FILE= "org.eclipse.jdt.jeview.IJarEntryResource.isFile";
	private static final String P_IS_READ_ONLY= "org.eclipse.jdt.jeview.IJarEntryResource.isReadOnly";

	protected IJarEntryResource fJarEntryResource;

	private static final ArrayList<IPropertyDescriptor> JAR_ENTRY_RESOURCE_PROPERTY_DESCRIPTORS= new ArrayList<>();
	static {
		addResourceDescriptor(new PropertyDescriptor(P_NAME, "name"));
		addResourceDescriptor(new PropertyDescriptor(P_FULL_PATH, "fullPath"));
		addResourceDescriptor(new PropertyDescriptor(P_IS_FILE, "isFile"));
		addResourceDescriptor(new PropertyDescriptor(P_IS_READ_ONLY, "isReadOnly"));
	}

	private static void addResourceDescriptor(PropertyDescriptor descriptor) {
		descriptor.setAlwaysIncompatible(true);
		descriptor.setCategory(C_JAR_ENTRY_RESOURCE);
		JAR_ENTRY_RESOURCE_PROPERTY_DESCRIPTORS.add(descriptor);
	}


	public JarEntryResourceProperties(IJarEntryResource jarEntryResource) {
		fJarEntryResource= jarEntryResource;
	}

	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		ArrayList<IPropertyDescriptor> result= new ArrayList<>(JAR_ENTRY_RESOURCE_PROPERTY_DESCRIPTORS);
		return result.toArray(new IPropertyDescriptor[result.size()]);
	}

	@Override
	public Object getPropertyValue(Object name) {
		if (P_NAME.equals(name)) {
			return fJarEntryResource.getName();
		} else 	if (P_FULL_PATH.equals(name)) {
			return fJarEntryResource.getFullPath();
		} else 	if (P_IS_FILE.equals(name)) {
			return fJarEntryResource.isFile();
		} else 	if (P_IS_READ_ONLY.equals(name)) {
			return fJarEntryResource.isReadOnly();
		}

		return null;
	}

	@Override
	public void setPropertyValue(Object name, Object value) {
		// do nothing
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
		// do nothing
	}
}
