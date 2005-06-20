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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;


public class JEResource extends JEAttribute {
	
	private final JEAttribute fParent;
	private final String fName;
	private IResource fResource;
	
	private JEAttribute[] fChildren;
	
	public JEResource(JEAttribute parent, String name, IResource resource) {
		fParent= parent;
		fName= name;
		fResource= resource;
	}

	public JEResource(JEAttribute parent, String name) {
		this(parent, name, null);
	}

	@Override
	public JEAttribute getParent() {
		return fParent;
	}
	
	public IResource getResource() {
		internalComputeResource();
		return fResource;
	}

	@Override
	public JEAttribute[] getChildren() {
		internalComputeResource();
		if (fChildren != null)
			return fChildren;
		
		if (fResource == null) {
			fChildren= EMPTY;
			return fChildren;
		}
		
		ArrayList<JEAttribute> result= new ArrayList<JEAttribute>();
		
		result.add(new JavaElementProperty(this, "Name", fResource.getName()));
		result.add(new JavaElementProperty(this, "FullPath", fResource.getFullPath()));
		result.add(new JavaElementProperty(this, "Location", fResource.getLocation()));
		result.add(new JavaElementProperty(this, "ProjectRelativePath", fResource.getProjectRelativePath()));
		result.add(new JavaElementProperty(this, "RawLocation", fResource.getRawLocation()));
		result.add(new JavaElementProperty(this, "LocalTimeStamp", fResource.getLocalTimeStamp()));
		result.add(new JavaElementProperty(this, "ModificationStamp", fResource.getModificationStamp()));
		//TODO: etc.
		
		if (fResource instanceof IContainer) {
			final IContainer container= (IContainer) fResource;
			result.add(new JavaElementProperty(this, "ModificationStamp") {
				@Override protected Object computeValue() throws CoreException {
					return container.getDefaultCharset();
				}
			});
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
		}
		fChildren= result.toArray(new JEAttribute[result.size()]);
		return fChildren;
	}

	@Override
	public String getLabel() {
		internalComputeResource();
		StringBuffer sb= new StringBuffer();
		if (fName.length() != 0) {
			sb.append(fName).append(": ");
		}
		if (isError())
			sb.append(Error.ERROR);
		else if (fResource == null) {
			sb.append("null");
		} else {
			sb.append(fResource.getName());
		}
		return sb.toString();
	}

	private void internalComputeResource() {
		if (fResource == null && ! isError()) {
			try {
				fResource= computeResource();
			} catch (Exception e) {
				fChildren= new Error[] { new Error(this, "", e) };
				fResource= null;
			}
		}
	}

	private boolean isError() {
		return fResource == null && fChildren != null && fChildren.length == 1 && fChildren[0] instanceof Error;
	}
	
	protected IResource computeResource() throws Exception {
		return fResource;
	}
	
}
