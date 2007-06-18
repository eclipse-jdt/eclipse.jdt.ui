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



public class Error extends JEAttribute {
	public static final String ERROR= "ERROR";

	private final JEAttribute fParent;
	private final String fName;
	private final Exception fException;

	public Error(JEAttribute parent, String name, Exception exception) {
		fParent= parent;
		fName= name;
		fException= exception;
	}

	@Override
	public JEAttribute getParent() {
		return fParent;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || !obj.getClass().equals(getClass())) {
			return false;
		}
		
		Error other= (Error) obj;
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
		
		if (fException == null) {
			if (other.fException != null)
				return false;
		} else if (! fException.equals(other.fException)) {
			return false;
		}
		
		return true;
	}
	
	@Override
	public int hashCode() {
		return (fParent != null ? fParent.hashCode() : 0)
				+ (fName != null ? fName.hashCode() : 0)
				+ (fException != null ? fException.hashCode() : 0);
	}
	
	@Override
	public JEAttribute[] getChildren() {
		return EMPTY;
	}

	@Override
	public String getLabel() {
		return (fName == null ? "" : fName + ": ") + fException.toString();
	}

	public Exception getException() {
		return fException;
	}
	
	@Override
	public Object getWrappedObject() {
		return fException;
	}
}
