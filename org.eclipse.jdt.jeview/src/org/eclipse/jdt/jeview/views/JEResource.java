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
import java.util.concurrent.Callable;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.util.Assert;


public class JEResource extends JEAttribute {
	
	private final JEAttribute fParent;
	private final String fName;
	private IResource fResource;
	
	JEResource(JEAttribute parent, String name, IResource resource) {
		Assert.isNotNull(parent);
		Assert.isNotNull(name);
		Assert.isNotNull(resource);
		fParent= parent;
		fName= name;
		fResource= resource;
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
		
		JEResource other= (JEResource) obj;
		if (! fParent.equals(other.fParent)) {
			return false;
		}
		if (! fName.equals(other.fName)) {
			return false;
		}
		if (! fResource.equals(other.fResource)) {
			return false;
		}
		
		return true;
	}
	
	@Override
	public int hashCode() {
		return fParent.hashCode() + fName.hashCode() + fResource.hashCode();
	}
	
	public IResource getResource() {
		return fResource;
	}

	@Override
	public JEAttribute[] getChildren() {
		if (fResource instanceof IContainer) {
			ArrayList<JEAttribute> result= new ArrayList<JEAttribute>();
			final IContainer container= (IContainer) fResource;
//			result.add(new JavaElementProperty(this, "ModificationStamp") {
//				@Override protected Object computeValue() throws CoreException {
//					return container.getDefaultCharset();
//				}
//			});
			result.add(new JavaElementChildrenProperty(this, "members") {
				@Override protected JEAttribute[] computeChildren() throws CoreException {
					IResource[] resources= container.members();
					JEAttribute[] children= new JEAttribute[resources.length];
					for (int i= 0; i < resources.length; i++) {
						children[i]= new JEResource(this, "", resources[i]);
					}
					return children;
				}
			});
			return result.toArray(new JEAttribute[result.size()]);
		}
		return EMPTY;
	}

	@Override
	public String getLabel() {
		return fName +  ": " + fResource.getName();
	}

	public static JEAttribute compute(JEAttribute parent, String name, Callable<IResource> computer) {
		try {
			IResource resource= computer.call();
			return create(parent, name, resource);
		} catch (Exception e) {
			return new Error(parent, name, e);
		}
	}

	public static JEAttribute create(JEAttribute parent, String name, IResource resource) {
		if (resource == null) {
			return new Null(parent, name);
		} else {
			return new JEResource(parent, name, resource);
		}
	}

}
