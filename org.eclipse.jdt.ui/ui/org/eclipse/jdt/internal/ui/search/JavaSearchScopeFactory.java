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
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.browsing.LogicalPackage;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;

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

	public IJavaSearchScope createJavaSearchScope(IWorkingSet[] workingSets, boolean includeJRE) {
		if (workingSets == null || workingSets.length < 1)
			return EMPTY_SCOPE;

		Set javaElements= new HashSet(workingSets.length * 10);
		for (int i= 0; i < workingSets.length; i++)
			addJavaElements(javaElements, workingSets[i]);
		return createJavaSearchScope(javaElements, includeJRE);
	}
	
	public IJavaSearchScope createJavaSearchScope(IWorkingSet workingSet, boolean includeJRE) {
		Set javaElements= new HashSet(10);
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
	
	private IJavaSearchScope internalCreateProjectScope(ISelection selection, boolean includeJRE) {
		Set javaProjects= getJavaProjects(selection);
		return createJavaSearchScope(javaProjects, includeJRE);
	}
	
	public IJavaSearchScope createJavaProjectSearchScope(IJavaElement selection, boolean includeJRE) {
		return createJavaProjectSearchScope(new StructuredSelection(selection), includeJRE);
	}
	
	
	public IJavaSearchScope createJavaProjectSearchScope(ISelection selection, boolean includeJRE) {
		IEditorInput input= getActiveEditorInput();
		if (input != null)
			return JavaSearchScopeFactory.getInstance().internalCreateProjectScope(input, includeJRE);
		return internalCreateProjectScope(selection, includeJRE);
		
	}
	public String getProjectScopeDescription(IJavaElement element) {
		IJavaProject project= element.getJavaProject();
		IEditorInput input= getActiveEditorInput();
		if (input != null) {
			IAdaptable inputElement = getEditorInputElement(input);
			if (inputElement != null) {
				IJavaProject project2= getJavaProject(inputElement);
				if (project2 != null)
					project= project2;
			}
		}

		if (project != null)
			return SearchMessages.getFormattedString("ProjectScope", project.getElementName()); //$NON-NLS-1$
		else 
			return SearchMessages.getFormattedString("ProjectScope", ""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private IEditorInput getActiveEditorInput() {
		IWorkbenchPage page= JavaPlugin.getActivePage();
		if (page != null) {
			IEditorPart editor= page.getActiveEditor();
			if (editor != null && editor.equals(page.getActivePart())) {
				return editor.getEditorInput();
			}
		}
		return null;
	}

	private IJavaSearchScope internalCreateProjectScope(IEditorInput editorInput, boolean includeJRE) {
		IAdaptable inputElement = getEditorInputElement(editorInput);
		StructuredSelection selection;
		if (inputElement != null) {
			selection= new StructuredSelection(inputElement);
		} else {
			selection= StructuredSelection.EMPTY;
		}
		return internalCreateProjectScope(selection, includeJRE);
	}
	
	private IAdaptable getEditorInputElement(IEditorInput editorInput) {
		IAdaptable inputElement= null;
		if (editorInput instanceof IClassFileEditorInput) {
			inputElement= ((IClassFileEditorInput)editorInput).getClassFile();
		} else if (editorInput instanceof IFileEditorInput) {
			inputElement= ((IFileEditorInput)editorInput).getFile();
		}
		return inputElement;
	}

	private Set getJavaProjects(ISelection selection) {
		Set javaProjects;
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Iterator iter= ((IStructuredSelection) selection).iterator();
			javaProjects= new HashSet(((IStructuredSelection) selection).size());
			while (iter.hasNext()) {
				Object selectedElement= iter.next();

				// Unpack search result view entry
				Object oldSearchResultEntry= SearchUtil.getGroupByKeyFromPossibleSearchResultViewEntry(selectedElement);
				if (oldSearchResultEntry != null) {
					selectedElement= oldSearchResultEntry;
				}
				
				if (selectedElement instanceof LogicalPackage)
					// must check this first, since it's adaptable, but doesn't adapt to anything useful
					javaProjects.add(((LogicalPackage) selectedElement).getJavaProject());
				else if (selectedElement instanceof IAdaptable) {
					IJavaProject javaProject= getJavaProject((IAdaptable) selectedElement);
					if (javaProject != null)
						javaProjects.add(javaProject);
				}
			}
		} else {
			javaProjects= EMPTY_SET;
		}
		return javaProjects;
	}

	private IJavaProject getJavaProject(IAdaptable selectedElement) {
		IJavaProject javaProject= (IJavaProject) selectedElement.getAdapter(IJavaProject.class);
		if (javaProject != null)
			return javaProject;
		IJavaElement javaElement= (IJavaElement) selectedElement.getAdapter(IJavaElement.class);
		if (javaElement != null) {
			javaProject= javaElement.getJavaProject();
			if (javaProject != null)
				return javaProject;
		}
		IResource resource= (IResource) selectedElement.getAdapter(IResource.class);
		if (resource != null) {
			IProject project= resource.getProject();
			try {
				if (project != null && project.isAccessible() && project.hasNature(JavaCore.NATURE_ID)) {
					return JavaCore.create(project);
				}
			} catch (CoreException e) {
				// Since the java project is accessible, this should not happen, anyway, don't search this project
			}
		}
		return null;
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
		Set javaElements;
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Iterator iter= ((IStructuredSelection) selection).iterator();
			javaElements= new HashSet(((IStructuredSelection) selection).size());
			while (iter.hasNext()) {
				Object selectedElement= iter.next();

				// Unpack search result view entry
				Object oldSearchResultEntry= SearchUtil.getGroupByKeyFromPossibleSearchResultViewEntry(selectedElement);
				if (oldSearchResultEntry != null) {
					selectedElement= oldSearchResultEntry;
				}

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

	private IJavaSearchScope createJavaSearchScope(Set javaElements, boolean includeJRE) {
		if (javaElements.isEmpty())
			return EMPTY_SCOPE;
		IJavaElement[] elementArray= (IJavaElement[])javaElements.toArray(new IJavaElement[javaElements.size()]);
		return SearchEngine.createJavaSearchScope(elementArray, getSearchFlags(includeJRE));
	}
	
	static int getSearchFlags(boolean includeJRE) {
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
