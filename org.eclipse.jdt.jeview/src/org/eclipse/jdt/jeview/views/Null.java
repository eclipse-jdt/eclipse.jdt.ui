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

import org.eclipse.core.runtime.Assert;



public class Null extends JEAttribute {

	private final JEAttribute fParent;
	private final String fName;
	
	public Null(JEAttribute parent, String name) {
		Assert.isNotNull(parent);
		Assert.isNotNull(name);
		fParent= parent;
		fName= name;
	}

	@Override
	public JEAttribute getParent() {
		return fParent;
	}

	@Override
	public JEAttribute[] getChildren() {
		return EMPTY;
	}

	@Override
	public Object getWrappedObject() {
		return null;
	}
	
	@Override
	public String getLabel() {
		return fName + ": null";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || !obj.getClass().equals(getClass())) {
			return false;
		}
		
		Null other= (Null) obj;
		if (! fParent.equals(other.fParent)) {
			return false;
		}
		if (! fName.equals(other.fName)) {
			return false;
		}
		
		return true;
	}
	
	@Override
	public int hashCode() {
		return fParent.hashCode() + fName.hashCode();
	}
}
