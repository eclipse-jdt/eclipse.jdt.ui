/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.jeview.views;

import java.util.ArrayList;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IJavaElement;


public class JEJarEntryResource extends JEAttribute {
	
	private final JEAttribute fParent; // can be null
	private final String fName; // can be null
	private IJarEntryResource fJarEntryResource;
	
	JEJarEntryResource(JEAttribute parent, String name, IJarEntryResource jarEntryResource) {
		Assert.isNotNull(jarEntryResource);
		fParent= parent;
		fName= name;
		fJarEntryResource= jarEntryResource;
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
		
		JEJarEntryResource other= (JEJarEntryResource) obj;
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
				+ (fName != null ? fName.hashCode() : 0)
				+ fJarEntryResource.hashCode();
	}
	
	@Override
	public Object getWrappedObject() {
		return fJarEntryResource;
	}
	
	public IJarEntryResource getJarEntryResource() {
		return fJarEntryResource;
	}

	@Override
	public JEAttribute[] getChildren() {
		ArrayList<JEAttribute> result= new ArrayList<JEAttribute>();
		
		Object parent= fJarEntryResource.getParent();
		if (parent instanceof IJarEntryResource)
			result.add(new JEJarEntryResource(this, "PARENT", (IJarEntryResource) parent));
		else
			result.add(new JavaElement(this, "PARENT", (IJavaElement) parent));
		
		result.add(new JavaElement(this, "PACKAGE FRAGMENT ROOT", fJarEntryResource.getPackageFragmentRoot()));
		
		result.add(new JavaElementChildrenProperty(this, "CHILDREN") {
			@Override protected JEAttribute[] computeChildren() throws CoreException {
				IJarEntryResource[] jarEntryResources= getJarEntryResource().getChildren();
				JEAttribute[] children= new JEAttribute[jarEntryResources.length];
				for (int i= 0; i < jarEntryResources.length; i++) {
					children[i]= new JEJarEntryResource(this, null, jarEntryResources[i]);
				}
				return children;
			}
		});
		return result.toArray(new JEAttribute[result.size()]);
	}

	@Override
	public String getLabel() {
		String label= fJarEntryResource.getName();
		if (fName != null)
			label= fName +  ": " + label;
		return label;
	}

}
