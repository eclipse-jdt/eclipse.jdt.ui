/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
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

package org.eclipse.jdt.internal.ui.workingsets;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetElementAdapter;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;

public class JavaWorkingSetElementAdapter implements IWorkingSetElementAdapter {

	@Override
	public IAdaptable[] adaptElements(IWorkingSet ws, IAdaptable[] elements) {
		ArrayList<Object> result= new ArrayList<>(elements.length);

		for (IAdaptable curr : elements) {
			if (curr instanceof IJavaElement) {
				result.add(curr);
			} else if (curr instanceof IResource) {
				result.add(adaptFromResource((IResource) curr));
			} else {
				Object elem= curr.getAdapter(IJavaElement.class);
				if (elem == null) {
					elem= curr.getAdapter(IResource.class);
					if (elem != null) {
						elem= adaptFromResource((IResource) elem);
					}
				}
				if (elem != null) {
					result.add(elem);
				} // ignore all others
			}
		}
		return result.toArray(new IAdaptable[result.size()]);
	}

	private Object adaptFromResource(IResource resource) {
		IProject project= resource.getProject();
		if (project != null && project.isAccessible()) {
			try {
				if (project.hasNature(JavaCore.NATURE_ID)) {
					IJavaElement elem= JavaCore.create(resource);
					if (elem != null) {
						return elem;
					}
				}
			} catch (CoreException e) {
				// ignore
			}
		}
		return resource;
	}


	@Override
	public void dispose() {
	}

}
