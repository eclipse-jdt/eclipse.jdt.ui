/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgDestinationValidator;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.ui.StandardJavaElementContentProvider;


public final class DestinationContentProvider extends StandardJavaElementContentProvider {
	
	private IReorgDestinationValidator fValidator;
	
	public DestinationContentProvider(IReorgDestinationValidator validator) {
		super(true);
		fValidator= validator;
	}
	
	public boolean hasChildren(Object element) {
		if (element instanceof IJavaElement){
			IJavaElement javaElement= (IJavaElement) element;
			if (! fValidator.canChildrenBeDestinations(javaElement))
				return false;
			if (javaElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT_ROOT){
				if (((IPackageFragmentRoot)javaElement).isArchive())
					return false;
			}
		} else if (element instanceof IResource) {
			IResource resource= (IResource) element;
			if (! fValidator.canChildrenBeDestinations(resource))
				return false;
		}
		return super.hasChildren(element);
	}
	
	public Object[] getChildren(Object parentElement) {
		try {
			if (parentElement instanceof IJavaModel) {
				return concatenate(getJavaProjects((IJavaModel)parentElement), getOpenNonJavaProjects((IJavaModel)parentElement));
			} else {
				Object[] children= super.getChildren(parentElement);
				ArrayList result= new ArrayList(children.length);
				for (int i= 0; i < children.length; i++) {
					if (children[i] instanceof IJavaElement) {
						IJavaElement javaElement= (IJavaElement) children[i];
						if (fValidator.canElementBeDestination(javaElement) || fValidator.canChildrenBeDestinations(javaElement))
							result.add(javaElement);
					} else if (children[i] instanceof IResource) {
						IResource resource= (IResource) children[i];
						if (fValidator.canElementBeDestination(resource) || fValidator.canChildrenBeDestinations(resource))
							result.add(resource);
					}
				}
				return result.toArray();
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return new Object[0];
		}
	}

	private static Object[] getOpenNonJavaProjects(IJavaModel model) throws JavaModelException {
		Object[] nonJavaProjects= model.getNonJavaResources();
		ArrayList result= new ArrayList(nonJavaProjects.length);
		for (int i= 0; i < nonJavaProjects.length; i++) {
			IProject project = (IProject) nonJavaProjects[i];
			if (project.isOpen())
				result.add(project);
		}
		return result.toArray();
	}

}
