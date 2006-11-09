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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.JavaCore;


public class JEResource extends JEAttribute {
	
	private final JEAttribute fParent; // can be null
	private final String fName; // can be null
	private IResource fResource;
	
	JEResource(JEAttribute parent, String name, IResource resource) {
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
				+ fResource.hashCode();
	}
	
	@Override
	public Object getWrappedObject() {
		return fResource;
	}
	
	public IResource getResource() {
		return fResource;
	}

	@Override
	public JEAttribute[] getChildren() {
		ArrayList<JEAttribute> result= new ArrayList<JEAttribute>();
		
		IContainer parent= fResource.getParent();
		if (parent != null )
			result.add(new JEResource(this, "PARENT", parent));
		else
			result.add(new JavaElementProperty(this, "PARENT", parent));
		
		result.add(new JavaElement(this, "JavaCore.create(..)", JavaCore.create(fResource)));
		
		if (fResource instanceof IContainer) {
			final IContainer container= (IContainer) fResource;
//			result.add(new JavaElementProperty(this, "ModificationStamp") {
//				@Override protected Object computeValue() throws CoreException {
//					return container.getDefaultCharset();
//				}
//			});
			result.add(new JavaElementChildrenProperty(this, "MEMBERS") {
				@Override protected JEAttribute[] computeChildren() throws CoreException {
					IResource[] resources= container.members();
					JEAttribute[] children= new JEAttribute[resources.length];
					for (int i= 0; i < resources.length; i++) {
						children[i]= new JEResource(this, "", resources[i]);
					}
					return children;
				}
			});
		}
		result.add(new JavaElementChildrenProperty(this, "FIND MARKERS (DEPTH_ZERO)") {
			@Override protected JEAttribute[] computeChildren() throws CoreException {
				IMarker[] markers= getResource().findMarkers(null, true, IResource.DEPTH_ZERO);
				JEAttribute[] children= new JEAttribute[markers.length];
				for (int i= 0; i < markers.length; i++) {
					children[i]= new JEMarker(this, "[" + i + "]", markers[i]);
				}
				return children;
			}
		});
		return result.toArray(new JEAttribute[result.size()]);
	}

	@Override
	public String getLabel() {
		String label= fResource.getName();
		if (fName != null)
			label= fName +  ": " + label;
		return label;
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
