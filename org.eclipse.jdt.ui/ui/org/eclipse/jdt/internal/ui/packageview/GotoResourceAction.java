/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.packageview;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ui.dialogs.ResourceListSelectionDialog;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

public class GotoResourceAction extends Action {

	private PackageExplorerPart fPackageExplorer;
	private Viewer fViewer;
	private int fSize;

	public GotoResourceAction(PackageExplorerPart explorer) {
		setText(PackagesMessages.getString("GotoResource.action.label")); //$NON-NLS-1$
		fPackageExplorer= explorer;
		fViewer= explorer.getViewer();
		fSize= 512;
	}
	
	public void run() {
		ViewerFilter[] filters= fPackageExplorer.getViewer().getFilters();
		List resources= new ArrayList(fSize);
		try {
			collect(resources, JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()), filters);
		} catch (CoreException e) {
		}
		IResource resourcesArray[]= (IResource[]) resources.toArray(new IResource[resources.size()]);
		
		ResourceListSelectionDialog dialog= new ResourceListSelectionDialog(
			fPackageExplorer.getViewSite().getShell(), resourcesArray); 
		dialog.setTitle(PackagesMessages.getString("GotoResource.dialog.title")); //$NON-NLS-1$
	 	dialog.open();
	 	Object[] result = dialog.getResult();
	 	if (result == null || result.length == 0 || result[0] instanceof IResource == false)
	 		return;
	 	StructuredSelection selection= null;
		IJavaElement element = JavaCore.create((IResource)result[0]);
		if (element != null)
			selection= new StructuredSelection(element);
		else 
			selection= new StructuredSelection(result[0]);
		fViewer.setSelection(selection, true);
	}
	
	private void collect(List result, IJavaModel parent, ViewerFilter[] filters) throws CoreException {
		IJavaProject[] projects= parent.getJavaProjects();
		for (int i = 0; i < projects.length; i++) {
			IJavaProject project= projects[i];
			if (isVisible(parent, project, filters)) {
				collect(result, project, filters);
			}
		}
	}
	
	private void collect(List result, IJavaProject parent, ViewerFilter[] filters) throws CoreException {
		boolean handleResources= true;
		IPackageFragmentRoot[] roots= parent.getPackageFragmentRoots();
		for (int i= 0; i < roots.length; i++) {
			IPackageFragmentRoot root= roots[i];
			if (isVisible(parent, root, filters) && !root.isArchive() && !root.isExternal()) {
				if ("".equals(root.getElementName()))
					handleResources= false;
				collect(result, root, filters);
			}
		}
		if (handleResources)	
			handleResources(result, parent, parent.getNonJavaResources(), filters);
	}
	
	private void collect(List result, IPackageFragmentRoot parent, ViewerFilter[] filters) throws CoreException {
		IJavaElement[] fragments= parent.getChildren();
		for (int i= 0; i < fragments.length; i++) {
			IJavaElement fragment= fragments[i];
			if (!isVisible(parent, fragment, filters))
				continue;
			if (fragment.getElementType() == IJavaElement.PACKAGE_FRAGMENT)
				collect(result, (IPackageFragment)fragment, filters);
		}
		handleResources(result, parent, parent.getNonJavaResources(), filters);
	}

	private void collect(List result, IPackageFragment parent, ViewerFilter[] filters) throws CoreException {
		ICompilationUnit[] units= parent.getCompilationUnits();
		for (int i= 0; i < units.length; i++) {
			ICompilationUnit unit= units[i];
			if (isVisible(parent, unit, filters))
				result.add(unit.getUnderlyingResource());
		}
		handleResources(result, parent, parent.getNonJavaResources(), filters);
	}
	
	private void handleResources(List result, Object parent, Object[] resources, ViewerFilter[] filters) {
		for (int i= 0; i < resources.length; i++) {
			Object object= resources[i];
			if (isVisible(parent, object, filters) && object instanceof IResource) {
				result.add(object);
			}
		}		
	}

	private boolean isVisible(Object parent, Object element, ViewerFilter[] filters) {
		for (int i= 0; i < filters.length; i++) {
			ViewerFilter filter= filters[i];
			if (!filter.select(fViewer, parent, element))
				return false;
		}
		return true;
	}
	
}
