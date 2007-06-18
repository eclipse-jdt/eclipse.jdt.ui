/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IMarker;


public class JEMarker extends JEAttribute {

	private JEAttribute fParent;
	private String fName;
	private IMarker fMarker;

	JEMarker(JEAttribute parent, String name, IMarker marker) {
		Assert.isNotNull(parent);
		Assert.isNotNull(name);
		Assert.isNotNull(marker);
		fParent= parent;
		fName= name;
		fMarker= marker;
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
	public String getLabel() {
		return fName +  ": (" + getMarkerAttribute(IMarker.SEVERITY) + ") " + getMarkerAttribute(IMarker.MESSAGE);
	}

	public Object getMarkerAttribute(String attributeName) {
		try {
			return fMarker.getAttribute(attributeName);
		} catch (CoreException e) {
			return e.getClass().getSimpleName();
		}
	}
	
	public IMarker getMarker() {
		return fMarker;
	}
	
	@Override
	public Object getWrappedObject() {
		return fMarker;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || !obj.getClass().equals(getClass())) {
			return false;
		}
		
		JEMarker other= (JEMarker) obj;
		if (! fParent.equals(other.fParent)) {
			return false;
		}
		if (! fName.equals(other.fName)) {
			return false;
		}
		if (! fMarker.equals(other.fMarker)) {
			return false;
		}
		
		return true;
	}
	
	@Override
	public int hashCode() {
		return fParent.hashCode() + fName.hashCode() + fMarker.hashCode();
	}
	
}
