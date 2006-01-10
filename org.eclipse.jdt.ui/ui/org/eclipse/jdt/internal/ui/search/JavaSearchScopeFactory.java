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
package org.eclipse.jdt.internal.ui.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;

import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.browsing.LogicalPackage;

public class JavaSearchScopeFactory {

	private static JavaSearchScopeFactory fgInstance;
	private final IJavaSearchScope EMPTY_SCOPE= SearchEngine.createJavaSearchScope(new IJavaElement[] {});
	private final Set EMPTY_SET= new HashSet(0);
	
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

	public IJavaSearchScope createJavaSearchScope(IWorkingSet[] workingSets, boolean includeJRE) {
		if (workingSets == null || workingSets.length < 1)
			return EMPTY_SCOPE;

		Set javaElements= new HashSet(workingSets.length * 10);
		for (int i= 0; i < workingSets.length; i++) {
			IWorkingSet workingSet= workingSets[i];
			if (workingSet.isEmpty() && workingSet.isAggregateWorkingSet()) {
				return createWorkspaceScope(includeJRE);
			}
			addJavaElements(javaElements, workingSet);
		}
		return createJavaSearchScope(javaElements, includeJRE);
	}
	
	public IJavaSearchScope createJavaSearchScope(IWorkingSet workingSet, boolean includeJRE) {
		Set javaElements= new HashSet(10);
		if (workingSet.isEmpty() && workingSet.isAggregateWorkingSet()) {
			return createWorkspaceScope(includeJRE);
		}
		addJavaElements(javaElements, workingSet);
		return createJavaSearchScope(javaElements, includeJRE);
	}

	public IJavaSearchScope createJavaSearchScope(IResource[] resources, boolean includeJRE) {
		if (resources == null)
			return EMPTY_SCOPE;
		Set javaElements= new HashSet(resources.length);
		addJavaElements(javaElements, resources);
		return createJavaSearchScope(javaElements, includeJRE);
	}
	
	public IJavaSearchScope createJavaSearchScope(ISelection selection, boolean includeJRE) {
		return createJavaSearchScope(getJavaElements(selection), includeJRE);
	}
		
	public IJavaSearchScope createJavaProjectSearchScope(String[] projectNames, boolean includeJRE) {
		ArrayList res= new ArrayList();
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		for (int i= 0; i < projectNames.length; i++) {
			IJavaProject project= JavaCore.create(root.getProject(projectNames[i]));
			if (project.exists()) {
				res.add(project);
			}
		}
		return createJavaSearchScope(res, includeJRE);
	}

	public IJavaSearchScope createJavaProjectSearchScope(IJavaProject project, boolean includeJRE) {
		return SearchEngine.createJavaSearchScope(new IJavaElement[] { project }, getSearchFlags(includeJRE));
	}
	
	public IJavaSearchScope createJavaProjectSearchScope(IEditorInput editorInput, boolean includeJRE) {
		IJavaElement elem= JavaUI.getEditorInputJavaElement(editorInput);
		if (elem != null) {
			IJavaProject project= elem.getJavaProject();
			if (project != null) {
				return createJavaProjectSearchScope(project, includeJRE);
			}
		}
		return EMPTY_SCOPE;
	}
	
	public String getProjectScopeDescription(IJavaProject project, boolean includeJRE) {
		if (includeJRE) {
			return Messages.format(SearchMessages.ProjectScope, project.getElementName());
		} else {
			return Messages.format(SearchMessages.ProjectScopeNoJRE, project.getElementName());
		}
	}
	
	public String getProjectScopeDescription(IEditorInput editorInput, boolean includeJRE) {
		IJavaElement elem= JavaUI.getEditorInputJavaElement(editorInput);
		if (elem != null) {
			IJavaProject project= elem.getJavaProject();
			if (project != null) {
				return getProjectScopeDescription(project, includeJRE);
			}
		}
		return Messages.format(SearchMessages.ProjectScope, "");  //$NON-NLS-1$
	}
	
	
	public IProject[] getProjects(IJavaSearchScope scope) {
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
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			return getJavaElements(((IStructuredSelection)selection).toArray());
		} else {
			return EMPTY_SET;
		}
	}

	private Set getJavaElements(Object[] elements) {
		if (elements.length == 0)
			return EMPTY_SET;
		
		Set result= new HashSet(elements.length);
		for (int i= 0; i < elements.length; i++) {
			Object selectedElement= elements[i];
			if (selectedElement instanceof IJavaElement) {
				addJavaElements(result, (IJavaElement) selectedElement);
			} else if (selectedElement instanceof IResource) {
				addJavaElements(result, (IResource) selectedElement);
			} else if (selectedElement instanceof LogicalPackage) {
				addJavaElements(result, (LogicalPackage) selectedElement);
			} else if (selectedElement instanceof IWorkingSet) {
				IWorkingSet ws= (IWorkingSet)selectedElement;
				addJavaElements(result, ws);
			} else if (selectedElement instanceof IAdaptable) {
				IResource resource= (IResource) ((IAdaptable) selectedElement).getAdapter(IResource.class);
				if (resource != null)
					addJavaElements(result, resource);
			}
			
		}
		return result;
	}

	private IJavaSearchScope createJavaSearchScope(Collection javaElements, boolean includeJRE) {
		if (javaElements.isEmpty())
			return EMPTY_SCOPE;
		IJavaElement[] elementArray= (IJavaElement[]) javaElements.toArray(new IJavaElement[javaElements.size()]);
		return SearchEngine.createJavaSearchScope(elementArray, getSearchFlags(includeJRE));
	}
	
	private static int getSearchFlags(boolean includeJRE) {
		int flags= IJavaSearchScope.SOURCES | IJavaSearchScope.APPLICATION_LIBRARIES;
		if (includeJRE)
			flags |= IJavaSearchScope.SYSTEM_LIBRARIES;
		return flags;
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
		
		if (workingSet.isAggregateWorkingSet() && workingSet.isEmpty()) {
			try {
				IJavaProject[] projects= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
				javaElements.addAll(Arrays.asList(projects));
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
			return;
		}
		
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
	
	public IJavaSearchScope createWorkspaceScope(boolean includeJRE) {
		if (!includeJRE) {
			try {
				IJavaProject[] projects= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
				return SearchEngine.createJavaSearchScope(projects, getSearchFlags(includeJRE));
			} catch (JavaModelException e) {
				// ignore, use workspace scope instead
			}
		}
		return SearchEngine.createWorkspaceScope();
	}

	public boolean isInsideJRE(IJavaElement element) {
		IPackageFragmentRoot root= (IPackageFragmentRoot) element.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
		if (root != null) {
			try {
				IClasspathEntry entry= root.getRawClasspathEntry();
				if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
					IClasspathContainer container= JavaCore.getClasspathContainer(entry.getPath(), root.getJavaProject());
					return container != null && container.getKind() == IClasspathContainer.K_DEFAULT_SYSTEM;
				}
				return false;
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		return true; // include JRE in doubt
	}
}
