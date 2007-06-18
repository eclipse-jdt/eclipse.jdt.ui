/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.jeview.views;


public class JavaElementProperty extends JEAttribute {

	private final JEAttribute fParent;
	private final String fName;
	private final String fValue;
	private final Object fValueObject;

	public JavaElementProperty(JEAttribute parent, String name) {
		fParent= parent;
		fName= name;
		fValue= null;
		fValueObject= null;
	}

	public JavaElementProperty(JEAttribute parent, String name, Object value) {
		fParent= parent;
		fName= name;
		fValueObject= value;
		if (value instanceof String)
			fValue= "\"" + value + "\"";
		else
			fValue= String.valueOf(value);
	}

	@Override
	public JEAttribute getParent() {
		return fParent;
	}

	@Override
	public Object getWrappedObject() {
		return fValueObject;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || !obj.getClass().equals(getClass())) {
			return false;
		}
		
		JavaElementProperty other= (JavaElementProperty) obj;
		if (fParent == null) {
			if (other.fParent != null)
				return false;
		} else if (! fParent.equals(other.fParent)) {
			return false;
		}
		
		if (fName == null) {
			if (other.fName != null)
				return false;
		} else if (! fName.equals(other.fName)) {
			return false;
		}
		
		return true;
	}
	
	@Override
	public int hashCode() {
		return (fParent != null ? fParent.hashCode() : 0)
				+ (fName != null ? fName.hashCode() : 0);
	}
	
	@Override
	public JEAttribute[] getChildren() {
		if (fValue != null)
			return EMPTY;
		
		try {
			computeValue();
			return EMPTY;
		} catch (Exception e) {
			return new Error[]{ new Error(this, "", e) };
		}
	}

	@Override
	public String getLabel() {
		String value= fValue;
		if (value == null) {
			try {
				value= String.valueOf(computeValue());
			} catch (Exception e) {
				return Error.ERROR;
			}
		}
		
		if (fName == null)
			return value;
		else
			return fName + ": " + value;
	}

	protected Object computeValue() throws Exception {
		return fValue;
	}

}
