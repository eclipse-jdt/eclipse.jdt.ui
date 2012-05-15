/*******************************************************************************
 * Copyright (c) 2001, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.jeview.properties;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IMarker;

import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.compiler.IProblem;

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
		boolean isJavaMarker= false;
		try {
			Map<String, Object> uncheckedAttributes= fMarker.getAttributes();
			attributes= uncheckedAttributes;
			isJavaMarker= IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER.equals(fMarker.getType());
		} catch (CoreException e) {
			// no attributes today
		}
		if (attributes == null) {
			fPropertyDescriptors= NO_DESCRIPTORS;
		} else {
			fPropertyDescriptors= new AttributeDescriptor[attributes.size() + 4];
			int i= 0;
			for (Entry<String, Object> entry : attributes.entrySet()) {
				String key= entry.getKey();
				Object value= entry.getValue();
				AttributeDescriptor propertyDescriptor;
				if (isJavaMarker && IJavaModelMarker.ID.equals(key)) {
					propertyDescriptor= new ProblemIdAttributeDescriptor(key, value);
				} else {
					propertyDescriptor= new AttributeDescriptor(key, value);
				}
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
	
	private static class ProblemIdAttributeDescriptor extends AttributeDescriptor {
		public ProblemIdAttributeDescriptor(String key, Object value) {
			super(key, value);
		}
		
		@Override
		public Object getValue() {
			return getErrorLabel();
		}
		
		private String getErrorLabel() {
			int id= (Integer) super.getValue();
			StringBuffer buf= new StringBuffer(getConstantName(id)).append(" = ");
				
			if ((id & IProblem.TypeRelated) != 0) {
				buf.append("TypeRelated + ");
			}
			if ((id & IProblem.FieldRelated) != 0) {
				buf.append("FieldRelated + ");
			}
			if ((id & IProblem.ConstructorRelated) != 0) {
				buf.append("ConstructorRelated + ");
			}
			if ((id & IProblem.MethodRelated) != 0) {
				buf.append("MethodRelated + ");
			}
			if ((id & IProblem.ImportRelated) != 0) {
				buf.append("ImportRelated + ");
			}
			if ((id & IProblem.Internal) != 0) {
				buf.append("Internal + ");
			}
			if ((id & IProblem.Syntax) != 0) {
				buf.append("Syntax + ");
			}
			if ((id & IProblem.Javadoc) != 0) {
				buf.append("Javadoc + ");
			}
			buf.append(id & IProblem.IgnoreCategoriesMask);
			
			buf.append(" = 0x").append(Integer.toHexString(id)).append(" = ").append(id);
			
			return buf.toString();
		}
		
		private static String getConstantName(int id) {
			Field[] fields= IProblem.class.getFields();
			for (int i= 0; i < fields.length; i++) {
				Field f= fields[i];
				try {
					if (f.getType() == int.class && f.getInt(f) == id) {
						return "IProblem." + f.getName();
					}
				} catch (IllegalArgumentException e) {
					// does not happen
				} catch (IllegalAccessException e) {
					// does not happen
				}
			}
			return "<UNKNOWN CONSTANT>";
		}
		
	}
	
	private static class MarkerPropertyDescriptor extends AttributeDescriptor {
		public MarkerPropertyDescriptor(String key, Object value) {
			super("org.eclipse.jdt.jeview.IMarker.property." + key, key, value);
		}
	}
	
}
