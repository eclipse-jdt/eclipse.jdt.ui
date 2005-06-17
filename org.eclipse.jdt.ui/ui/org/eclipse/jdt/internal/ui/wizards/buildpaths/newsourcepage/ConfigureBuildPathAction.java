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

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jdt.internal.ui.preferences.BuildPathsPropertyPage;

/**
 * 
 */
public class ConfigureBuildPathAction extends Action implements IUpdate {

	private final IWorkbenchSite fSite;
	private IProject fProject;

	public ConfigureBuildPathAction(IWorkbenchSite site) {
		fSite= site;
	}
	
	private Shell getShell() {
		return fSite.getShell();
	}
	
	public void run() {
		if (fProject != null) {
			PreferencesUtil.createPropertyDialogOn(getShell(), fProject, BuildPathsPropertyPage.PROP_ID, null, null).open();
		}
	}

	public void update() {
		ISelection selection= fSite.getSelectionProvider().getSelection();
		
		fProject= null;
		if (selection instanceof IStructuredSelection && !selection.isEmpty() ) {
			IStructuredSelection structSel= (IStructuredSelection) selection;
			if (structSel.size() == 1) {
				Object firstElement= structSel.getFirstElement();
				fProject= getProjectFromSelectedElement(firstElement);
			}
		}			
		setEnabled(fProject != null);	
	}

	private IProject getProjectFromSelectedElement(Object firstElement) {
		if (firstElement instanceof IJavaElement) {
			IJavaElement element= (IJavaElement) firstElement;
			IPackageFragmentRoot root= JavaModelUtil.getPackageFragmentRoot(element);
			if (root != null && root != element && root.isArchive()) {
				return null;
			}
			IJavaProject project= element.getJavaProject();
			if (project != null) {
				return project.getProject();
			}
			return null;
		} else if (firstElement instanceof ClassPathContainer) {
			return ((ClassPathContainer) firstElement).getJavaProject().getProject();
		} else if (firstElement instanceof IAdaptable) {
			IResource res= (IResource) ((IAdaptable) firstElement).getAdapter(IResource.class);
			if (res != null) {
				return res.getProject();
			}
		}
		return null;
	}

}
