/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.jeview.properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.jeview.JEViewPlugin;

public class ClasspathEntryProperties implements IPropertySource {
	
	private static abstract class Property extends GenericProperty<IClasspathEntry> {
		public Property(String name) {
			super(IClasspathEntry.class, name);
		}
	}
	
	private static HashMap<String, Property> fgIdToProperty= new HashMap<String, Property>();
	private static LinkedHashMap<Class<?>, List<Property>> fgTypeToProperty= new LinkedHashMap<Class<?>, List<Property>>();
	
	static {
		addProperty(new Property("combineAccessRules") {
			@Override public Object compute(IClasspathEntry entry) {
				return entry.combineAccessRules();
			}
		});
		addProperty(new Property("getContentKind") {
			@Override public Object compute(IClasspathEntry entry) {
				return getContentKindString(entry.getContentKind());
			}
		});
		addProperty(new Property("getEntryKind") {
			@Override public Object compute(IClasspathEntry entry) {
				return getEntryKindString(entry.getEntryKind());
			}
		});
		addProperty(new Property("getOutputLocation") {
			@Override public Object compute(IClasspathEntry entry) {
				return entry.getOutputLocation();
			}
		});
		addProperty(new Property("getPath") {
			@Override public Object compute(IClasspathEntry entry) {
				return entry.getPath();
			}
		});
		addProperty(new Property("getSourceAttachmentPath") {
			@Override public Object compute(IClasspathEntry entry) {
				return entry.getSourceAttachmentPath();
			}
		});
		addProperty(new Property("getSourceAttachmentRootPath") {
			@Override public Object compute(IClasspathEntry entry) {
				return entry.getSourceAttachmentRootPath();
			}
		});
		addProperty(new Property("isExported") {
			@Override public Object compute(IClasspathEntry entry) {
				return entry.isExported();
			}
		});
	}
	
	private static void addProperty(Property property) {
		fgIdToProperty.put(property.getId(), property);
		List<Property> properties= fgTypeToProperty.get(property.getType());
		if (properties == null) {
			properties= new ArrayList<Property>();
			fgTypeToProperty.put(property.getType(), properties);
		}
		properties.add(property);
	}
	
	static String getContentKindString(int kind) {
		String name;
		switch (kind) {
			case IPackageFragmentRoot.K_SOURCE :
				name= "K_SOURCE";
				break;
			case IPackageFragmentRoot.K_BINARY :
				name= "K_BINARY";
				break;
			default :
				name= "UNKNOWN";
				break;
		}
		return kind + " (" + name + ")";
	}
	
	static String getEntryKindString(int kind) {
		String name;
		switch (kind) {
			case IClasspathEntry.CPE_CONTAINER :
				name= "CPE_CONTAINER";
				break;
			case IClasspathEntry.CPE_LIBRARY :
				name= "CPE_LIBRARY";
				break;
			case IClasspathEntry.CPE_PROJECT :
				name= "CPE_PROJECT";
				break;
			case IClasspathEntry.CPE_SOURCE :
				name= "CPE_SOURCE";
				break;
			case IClasspathEntry.CPE_VARIABLE :
				name= "CPE_VARIABLE";
				break;
			default :
				name= "UNKNOWN";
				break;
		}
		return kind + " (" + name + ")";
	}
	
	
	protected IClasspathEntry fEntry;

	public ClasspathEntryProperties(IClasspathEntry entry) {
		fEntry= entry;
	}
	
	public IPropertyDescriptor[] getPropertyDescriptors() {
		List<IPropertyDescriptor> result= new ArrayList<IPropertyDescriptor>();
		for (Entry<Class<?>, List<Property>> entry : fgTypeToProperty.entrySet()) {
			if (entry.getKey().isAssignableFrom(fEntry.getClass())) {
				for (Property property : entry.getValue()) {
					result.add(property.getDescriptor());
				}
			}
		}
		return result.toArray(new IPropertyDescriptor[result.size()]);
	}
	
	public Object getPropertyValue(Object id) {
		Property property= fgIdToProperty.get(id);
		if (property == null) {
			return null;
		} else {
			try {
				return property.compute(fEntry);
			} catch (JavaModelException e) {
				if (e.isDoesNotExist()) {
					return "JavaModelException: " + e.getLocalizedMessage();
				} else {
					JEViewPlugin.log("error calculating property '" + property.getType().getSimpleName() + '#' + property.getName() + '\'', e);
					return "Error: " + e.getLocalizedMessage();
				}
			}
		}
	}
	
	public void setPropertyValue(Object name, Object value) {
		// do nothing
	}
	
	public Object getEditableValue() {
		return this;
	}
	
	public boolean isPropertySet(Object property) {
		return false;
	}
	
	public void resetPropertyValue(Object property) {
		// do nothing
	}
}
