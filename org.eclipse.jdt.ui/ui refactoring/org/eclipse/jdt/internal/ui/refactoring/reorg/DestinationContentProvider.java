/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.ui.StandardJavaElementContentProvider;


public final class DestinationContentProvider extends StandardJavaElementContentProvider {
	
	private final int fStopExpandingAtType;

	public DestinationContentProvider(){
		this(IJavaElement.PACKAGE_FRAGMENT, false, false);//default
	}
	
	public DestinationContentProvider(int stopExpandingAtType, boolean provideMembers, boolean provideWorkingCopy){
		super(provideMembers, provideWorkingCopy);
		fStopExpandingAtType= stopExpandingAtType;
	}
	
	public boolean hasChildren(Object element) {
		if (element instanceof IJavaElement){
			IJavaElement javaElement= (IJavaElement)element;
			if ((javaElement).getElementType() == fStopExpandingAtType)
				return false;
			if (javaElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT_ROOT){
				if (((IPackageFragmentRoot)javaElement).isArchive())
					return false;
			}
		}
		return super.hasChildren(element);
	}
	
	public Object[] getChildren(Object parentElement) {
		try {
			if (parentElement instanceof IJavaModel) 
				return concatenate(getJavaProjects((IJavaModel)parentElement), getOpenNonJavaProjects((IJavaModel)parentElement));
			else
				return super.getChildren(parentElement);
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