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
package org.eclipse.jdt.internal.ui.search;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;

import org.eclipse.search.ui.ISearchResultViewEntry;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.browsing.LogicalPackage;

public class JavaSearchScopeFactory {

	private static JavaSearchScopeFactory fgInstance;
	private static final IJavaSearchScope EMPTY_SCOPE= SearchEngine.createJavaSearchScope(new IJavaElement[] {});
	private static final Set EMPTY_SET= new HashSet(0);
	
	private JavaSearchScopeFactory() {
	}

	public static JavaSearchScopeFactory getInstance() {
		if (fgInstance == null)
			fgInstance= new JavaSearchScopeFactory();
		return fgInstance;
	}

	public IWorkingSet[] queryWorkingSets() throws JavaModelException {
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		if (shell == null)
			return null;
		IWorkingSetSelectionDialog dialog= PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSetSelectionDialog(shell, true);
		if (dialog.open() == Window.OK) {
			IWorkingSet[] workingSets= dialog.getSelection();
			if (workingSets.length > 0)
				return workingSets;
		}
		return null;
	}

	public IJavaSearchScope createJavaSearchScope(IWorkingSet[] workingSets) {
		if (workingSets == null || workingSets.length < 1)
			return EMPTY_SCOPE;

		Set javaElements= new HashSet(workingSets.length * 10);
		for (int i= 0; i < workingSets.length; i++)
			addJavaElements(javaElements, workingSets[i]);
		return createJavaSearchScope(javaElements);
	}
	
	public IJavaSearchScope createJavaSearchScope(IWorkingSet workingSet) {
		Set javaElements= new HashSet(10);
		addJavaElements(javaElements, workingSet);
		return createJavaSearchScope(javaElements);
	}

	public IJavaSearchScope createJavaSearchScope(IResource[] resources) {
		if (resources == null)
			return EMPTY_SCOPE;
		Set javaElements= new HashSet(resources.length);
		addJavaElements(javaElements, resources);
		return createJavaSearchScope(javaElements);
	}
	
	public IJavaSearchScope createJavaSearchScope(ISelection selection) {
		return createJavaSearchScope(getJavaElements(selection));
	}
	
	public IJavaSearchScope createJavaProjectSearchScope(ISelection selection) {
		Set javaElements= getJavaElements(selection);
		Set javaProjects= new HashSet(javaElements.size());
		Iterator elements= javaElements.iterator();
		while (elements.hasNext()) {
			IJavaProject jp= ((IJavaElement)elements.next()).getJavaProject();
			if (jp != null)
				javaProjects.add(jp);	
		}
		return createJavaSearchScope(javaProjects);
	}
	
	public IProject[] getJavaProjects(IJavaSearchScope scope) {
		IPath[] paths= scope.enclosingProjectsAndJars();
		HashSet temp= new HashSet();
		for (int i= 0; i < paths.length; i++) {
			IResource resource= ResourcesPlugin.getWorkspace().getRoot().findMember(paths[i]);
			if (resource != null && resource.getType() == IResource.PROJECT)
				temp.add(resource);
		}
		return (IProject[]) temp.toArray(new IProject[temp.size()]);
	}

	private Set getJavaElements(ISelection selection) {
		Set javaElements;
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Iterator iter= ((IStructuredSelection) selection).iterator();
			javaElements= new HashSet(((IStructuredSelection) selection).size());
			while (iter.hasNext()) {
				Object selectedElement= iter.next();

				// Unpack search result view entry
				if (selectedElement instanceof ISearchResultViewEntry)
					selectedElement= ((ISearchResultViewEntry) selectedElement).getGroupByKey();

				if (selectedElement instanceof IJavaElement)
					addJavaElements(javaElements, (IJavaElement) selectedElement);
				else if (selectedElement instanceof IResource)
					addJavaElements(javaElements, (IResource) selectedElement);
				else if (selectedElement instanceof LogicalPackage)
					addJavaElements(javaElements, (LogicalPackage) selectedElement);
				else if (selectedElement instanceof IAdaptable) {
					IResource resource= (IResource) ((IAdaptable) selectedElement).getAdapter(IResource.class);
					if (resource != null)
						addJavaElements(javaElements, resource);
				}
			}
		} else {
			javaElements= EMPTY_SET;
		}
		return javaElements;
	}

	private IJavaSearchScope createJavaSearchScope(Set javaElements) {
		if (javaElements.isEmpty())
			return EMPTY_SCOPE;
		return SearchEngine.createJavaSearchScope((IJavaElement[])javaElements.toArray(new IJavaElement[javaElements.size()]));
	}

	private void addJavaElements(Set javaElements, IResource[] resources) {
		for (int i= 0; i < resources.length; i++)
			addJavaElements(javaElements, resources[i]);
	}

	private void addJavaElements(Set javaElements, IResource resource) {
		IJavaElement javaElement= (IJavaElement)resource.getAdapter(IJavaElement.class);
		if (javaElement == null)
			// not a Java resource
			return;
		
		if (javaElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
			// add other possible package fragments
			try {
				addJavaElements(javaElements, ((IFolder)resource).members());
			} catch (CoreException ex) {
				// don't add elements
			}
		}
			
		javaElements.add(javaElement);
	}

	private void addJavaElements(Set javaElements, IJavaElement javaElement) {
		javaElements.add(javaElement);
	}
	
	private void addJavaElements(Set javaElements, IWorkingSet workingSet) {
		if (workingSet == null)
			return;
		
		IAdaptable[] elements= workingSet.getElements();
		for (int i= 0; i < elements.length; i++) {
			IJavaElement javaElement=(IJavaElement) elements[i].getAdapter(IJavaElement.class);
			if (javaElement != null) { 
				addJavaElements(javaElements, javaElement);
				continue;
			}
			IResource resource= (IResource)elements[i].getAdapter(IResource.class);
			if (resource != null) {
				addJavaElements(javaElements, resource);
			}
			
			// else we don't know what to do with it, ignore.
		}
	}

	public void addJavaElements(Set javaElements, LogicalPackage selectedElement) {
		IPackageFragment[] packages= selectedElement.getFragments();
		for (int i= 0; i < packages.length; i++)
			addJavaElements(javaElements, packages[i]);
	}
}
