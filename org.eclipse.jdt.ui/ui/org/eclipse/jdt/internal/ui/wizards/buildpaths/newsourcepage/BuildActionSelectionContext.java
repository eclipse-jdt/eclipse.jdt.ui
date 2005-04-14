/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;

public class BuildActionSelectionContext {

	private IStructuredSelection fSelection;
	private IJavaProject fJavaProject;
	private List fElements;
	private int[] fTypes;

	public BuildActionSelectionContext() {
		fSelection= null;
		fElements= null;
		fTypes= null;
	}
	
	public void init(IStructuredSelection selection) {
		if (selection == null || !selection.equals(fSelection)) {
			initContextValues(selection);
		}
	}
	
	private void initContextValues(IStructuredSelection selection) {
		fSelection= selection;

		fJavaProject= null;
		fElements= Collections.EMPTY_LIST;
		fTypes= new int[0];
		
		IJavaProject project= getJavaProjectFromSelection(selection);
		if (project != null && project.exists()) {
			List elements= selection.toList();
			try {
				int[] types= new int[elements.size()];
				for (int i= 0; i < elements.size(); i++) {
					Object curr= elements.get(i);
					if (i > 0 && !project.equals(getJavaProjectFromSelectedElement(curr))) {
						return;
					}
					
					types[i]= DialogPackageExplorerActionGroup.getType(elements.get(i), project);
				}
				
				fJavaProject= project;
				fElements= elements;
				fTypes= types;
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}

	}

	
	/**
	 * Get the Java project from the first element in the provided selection.
	 * 
	 * @param selection the selection containing a list of elements 
	 * @return the Java project of the first element of the selection, or 
	 * <code>null</code> if the selection is empty or no Java project could 
	 * be found.
	 */
	private IJavaProject getJavaProjectFromSelection(IStructuredSelection selection) {
		if (selection.isEmpty())
			return null;
		Object element= selection.getFirstElement();
		return getJavaProjectFromSelectedElement(element);
	}

	/**
	 * For a given element, try to get it's Java project
	 * 
	 * @param element the element to get the Java project from
	 * 
	 * @return the Java project of the provided element, or 
	 * <code>null</code> if no Java project could be found.
	 */
	private IJavaProject getJavaProjectFromSelectedElement(Object element) {

		if (element instanceof IJavaElement)
			return ((IJavaElement) element).getJavaProject();
		if (element instanceof ClassPathContainer)
			return ((ClassPathContainer) element).getJavaProject();
		if (element instanceof IResource)
			return JavaCore.create(((IResource) element).getProject());
		
		if (element instanceof IAdaptable) {
			IResource resource= (IResource) ((IAdaptable) element).getAdapter(IResource.class);
			if (resource != null) {
				return JavaCore.create(resource.getProject());
			}
		}
		return null;
	}

	public List getElements() {
		return fElements;
	}
	

	public IJavaProject getJavaProject() {
		return fJavaProject;
	}
	

	public IStructuredSelection getSelection() {
		return fSelection;
	}
	

	public int[] getTypes() {
		return fTypes;
	}

}
