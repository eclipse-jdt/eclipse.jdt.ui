/*******************************************************************************
 * Copyright (c) 2001, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.jeview.properties;

import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IMarker;

import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

public class MarkerProperties implements IPropertySource {

	private static final AttributeDescriptor[] NO_DESCRIPTORS= new AttributeDescriptor[0];
	
	private final IMarker fMarker;

	private AttributeDescriptor[] fPropertyDescriptors;

	public MarkerProperties(IMarker marker) {
		fMarker= marker;
	}

	public AttributeDescriptor[] getPropertyDescriptors() {
		if (fPropertyDescriptors != null)
			return fPropertyDescriptors;
		
		Map<String, Object> attributes= null;
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> uncheckedAttributes= fMarker.getAttributes();
			attributes= uncheckedAttributes;
		} catch (CoreException e) {
			// no attributes today
		}
		if (attributes == null) {
			fPropertyDescriptors= NO_DESCRIPTORS;
		} else {
			fPropertyDescriptors= new AttributeDescriptor[attributes.size() + 4];
			int i= 0;
			for (Entry<String, Object> entry : attributes.entrySet()) {
				AttributeDescriptor propertyDescriptor= new AttributeDescriptor(entry.getKey(), entry.getValue());
				propertyDescriptor.setAlwaysIncompatible(true);
				propertyDescriptor.setCategory("Attributes");
				fPropertyDescriptors[i++]= propertyDescriptor;
			}
			
			MarkerPropertyDescriptor propertyDescriptor= new MarkerPropertyDescriptor("exists", fMarker.exists());
			propertyDescriptor.setAlwaysIncompatible(true);
			fPropertyDescriptors[i++]= propertyDescriptor;
			
			String type;
			try {
				type= fMarker.getType();
			} catch (CoreException e) {
				type= e.getLocalizedMessage();
			}
			propertyDescriptor= new MarkerPropertyDescriptor("type", type);
			propertyDescriptor.setAlwaysIncompatible(true);
			fPropertyDescriptors[i++]= propertyDescriptor;
			
			String time;
			try {
				time= DateFormat.getDateTimeInstance().format(new Date(fMarker.getCreationTime()));
			} catch (CoreException e) {
				time= e.getLocalizedMessage();
			}
			propertyDescriptor= new MarkerPropertyDescriptor("creationTime", time);
			propertyDescriptor.setAlwaysIncompatible(true);
			fPropertyDescriptors[i++]= propertyDescriptor;
			
			propertyDescriptor= new MarkerPropertyDescriptor("markerId", fMarker.getId());
			propertyDescriptor.setAlwaysIncompatible(true);
			fPropertyDescriptors[i++]= propertyDescriptor;
			
		}
		return fPropertyDescriptors;
	}

	public Object getPropertyValue(Object id) {
		AttributeDescriptor[] propertyDescriptors= getPropertyDescriptors();
		for (int i= 0; i < propertyDescriptors.length; i++) {
			AttributeDescriptor descriptor= propertyDescriptors[i];
			if (descriptor.getId().equals(id))
				return descriptor.getValue();
		}
		return null;
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
	
	
	private static class AttributeDescriptor extends PropertyDescriptor {
		private final Object fValue;

		public AttributeDescriptor(String name, Object value) {
			this("org.eclipse.jdt.jeview.IMarker." + name, name, value);
		}
		
		protected AttributeDescriptor(String key, String name, Object value) {
			super(key, name);
			fValue= value;
		}
		
		public Object getValue() {
			return fValue;
		}
	}
	
	private static class MarkerPropertyDescriptor extends AttributeDescriptor {
		public MarkerPropertyDescriptor(String key, Object value) {
			super("org.eclipse.jdt.jeview.IMarker.property." + key, key, value);
		}
	}
	
}
