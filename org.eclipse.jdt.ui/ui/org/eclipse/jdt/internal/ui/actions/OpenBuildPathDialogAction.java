/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others. All
 * rights reserved. This program and the accompanying materials are made
 * available under the terms of the Common Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/cpl-v10.
 * html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.core.resources.IResource;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.dialogs.BuildPathDialog;

public class OpenBuildPathDialogAction implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow fWindow;
	private IJavaProject fProject;

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		fWindow= window;
	}

	public void run(IAction action) {
		BuildPathDialog dialog= new BuildPathDialog(fWindow.getShell(), fProject);
		dialog.open();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		boolean enable= false;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection s= (IStructuredSelection)selection;
			if (s.size() == 1) {
				Object element= s.getFirstElement();
				if (element instanceof IJavaElement) {
					fProject= (IJavaProject)((IJavaElement)element).getAncestor(IJavaElement.JAVA_PROJECT);
					enable= fProject != null;
				} else if (element instanceof IAdaptable) {
					IResource resource= (IResource)((IAdaptable)element).getAdapter(IResource.class);
					if (resource != null) {
						IJavaProject p= JavaCore.create(resource.getProject());
						if (p.exists()) {
							fProject= p;
							enable= true;
						}
					}
				}
			}
		}
		action.setEnabled(enable);
	}
}
