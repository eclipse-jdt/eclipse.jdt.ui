/*******************************************************************************
 * Copyright (c) 2000, 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Common Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/cpl-v10.
 * html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ui.IContainmentAdapter;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.JavaCore;

public class JavaElementContainmentAdapter implements IContainmentAdapter {
	
	private IJavaModel fJavaModel= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());

	public boolean contains(Object workingSetElement, Object element, boolean checkIfDescendent, boolean checkIfAncestor) {
		if (!(workingSetElement instanceof IJavaElement) || element == null)
			return false;
						
		IJavaElement workingSetJavaElement= (IJavaElement)workingSetElement;
		IResource resource= null;		
		IJavaElement jElement= null;
		if (element instanceof IJavaElement) {
			jElement= (IJavaElement)element;	
		} else {
			if (element instanceof IAdaptable) {
				resource= (IResource)((IAdaptable)element).getAdapter(IResource.class);
				if (resource != null) {
					if (fJavaModel.contains(resource)) {
						jElement= JavaCore.create(resource);
						if (jElement != null && !jElement.exists())
							jElement= null;		
					}
				}
			}
		}
		
		if (jElement != null) {
			if (contains(workingSetJavaElement, jElement, checkIfDescendent, checkIfAncestor))
				return true;
			if (workingSetJavaElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT && 
				resource.getType() == IResource.FOLDER && checkIfDescendent)
				return isChild(workingSetJavaElement, resource);
		} else if (resource != null) {
			return contains(workingSetJavaElement, resource, checkIfDescendent, checkIfAncestor);
		}
		return false;
	}
	
	private boolean contains(IJavaElement workingSetElement, IJavaElement element, boolean checkIfDescendent, boolean checkIfAncestor) {
		if (workingSetElement.equals(element))
			return true;
		if (checkIfDescendent && check(workingSetElement, element)) {
			return true;
		}
		if (checkIfAncestor && check(element, workingSetElement)) {
			return true;
		}
		return workingSetElement.equals(element.getParent());
	}
	
	private boolean check(IJavaElement ancestor, IJavaElement descendent) {
		descendent= descendent.getParent();
		while (descendent != null) {
			if (ancestor.equals(descendent))
				return true;
			descendent= descendent.getParent();
		}
		return false;
	}
	
	private boolean isChild(IJavaElement workingSetElement, IResource element) {
		IResource resource= workingSetElement.getResource();
		if (resource == null)
			return false;
		return check(element, resource);
	}
	
	private boolean contains(IJavaElement workingSetElement, IResource element, boolean checkIfDescendent, boolean checkIfAncestor) {
		IResource workingSetResource= workingSetElement.getResource();
		if (workingSetResource == null)
			return false;
		if (workingSetResource.equals(element))
			return true;
		if (checkIfDescendent && check(workingSetResource, element)) {
			return true;
		}
		if (checkIfAncestor && check(element, workingSetResource)) {
			return true;
		}
		return workingSetResource.equals(element.getParent());
	}
	
	private boolean check(IResource ancestor, IResource descendent) {
		descendent= descendent.getParent();
		while(descendent != null) {
			if (ancestor.equals(descendent))
				return true;
			descendent= descendent.getParent();
		}
		return false;
	}
}
