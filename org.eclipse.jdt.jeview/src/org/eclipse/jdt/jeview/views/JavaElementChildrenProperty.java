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


public abstract class JavaElementChildrenProperty extends JEAttribute {

	private final JEAttribute fParent;
	private final String fName;

	public JavaElementChildrenProperty(JEAttribute parent, String name) {
		fParent= parent;
		fName= name;
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
		
		JavaElementChildrenProperty other= (JavaElementChildrenProperty) obj;
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
	public Object getWrappedObject() {
		return getChildren().length;
	}
	
	@Override
	public JEAttribute[] getChildren() {
		try {
			return computeChildren();
		} catch (Exception e) {
			return new JEAttribute[] {new Error(this, "", e)};
		}
	}

	protected abstract JEAttribute[] computeChildren() throws Exception;

	@Override
	public String getLabel() {
		Object[] children= getChildren();
		String count;
		if (children.length == 1 && children[0] instanceof Error) {
			count= Error.ERROR;
		} else {
			count= String.valueOf(children.length);
		}
		return fName + " (" + count + ")";
	}

}
