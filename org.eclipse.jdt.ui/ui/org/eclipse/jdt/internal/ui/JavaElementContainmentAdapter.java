/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IResource;

import org.eclipse.ui.IContainmentAdapter;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class JavaElementContainmentAdapter implements IContainmentAdapter {

	@Override
	public boolean contains(Object workingSetElement, Object element, int flags) {
		if (!(workingSetElement instanceof IJavaElement) || element == null)
			return false;

		IJavaElement workingSetJavaElement= (IJavaElement)workingSetElement;
		IResource resource= null;
		IJavaElement jElement= null;
		if (element instanceof IJavaElement) {
			jElement= (IJavaElement)element;
			resource= jElement.getResource();
		} else {
			if (element instanceof IAdaptable) {
				resource= ((IAdaptable)element).getAdapter(IResource.class);
				if (resource != null) {
					jElement= findJavaElement(resource);
				}
			}
		}

		if (jElement != null) {
			if (contains(workingSetJavaElement, jElement, flags))
				return true;
			if (workingSetJavaElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT &&
				resource.getType() == IResource.FOLDER && checkIfDescendant(flags))
				return isChild(workingSetJavaElement, resource);
		} else if (resource != null) {
			return contains(workingSetJavaElement, resource, flags);
		}
		return false;
	}

	private static IJavaElement findJavaElement(IResource resource) {
		IJavaProject project= JavaCore.create(resource.getProject());
		if (project == null) {
			return null;
		}
		IJavaElement javaElement= JavaCore.create(resource, project);
		if (javaElement != null && javaElement.exists()) {
			return javaElement;
		}
		return null;
	}

	private boolean contains(IJavaElement workingSetElement, IJavaElement element, int flags) {
		if (checkContext(flags) && workingSetElement.equals(element)) {
			return true;
		}
		if (checkIfChild(flags) && workingSetElement.equals(element.getParent())) {
			return true;
		}
		if (checkIfDescendant(flags) && check(workingSetElement, element)) {
			return true;
		}
		if (checkIfAncestor(flags) && check(element, workingSetElement)) {
			return true;
		}
		return false;
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

	private boolean contains(IJavaElement workingSetElement, IResource element, int flags) {
		IResource workingSetResource= workingSetElement.getResource();
		if (workingSetResource == null)
			return false;
		if (checkContext(flags) && workingSetResource.equals(element)) {
			return true;
		}
		if (checkIfChild(flags) && workingSetResource.equals(element.getParent())) {
			return true;
		}
		if (checkIfDescendant(flags) && check(workingSetResource, element)) {
			return true;
		}
		if (checkIfAncestor(flags) && check(element, workingSetResource)) {
			return true;
		}
		return false;
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

	private boolean checkIfDescendant(int flags) {
		return (flags & CHECK_IF_DESCENDANT) != 0;
	}

	private boolean checkIfAncestor(int flags) {
		return (flags & CHECK_IF_ANCESTOR) != 0;
	}

	private boolean checkIfChild(int flags) {
		return (flags & CHECK_IF_CHILD) != 0;
	}

	private boolean checkContext(int flags) {
		return (flags & CHECK_CONTEXT) != 0;
	}
}
