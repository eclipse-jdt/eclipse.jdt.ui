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
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ResourceListSelectionDialog;
import org.eclipse.ui.PlatformUI;

public class GotoResourceAction extends Action {

	private PackageExplorerPart fPackageExplorer;

	private static class GotoResourceDialog extends ResourceListSelectionDialog {
		private IJavaModel fJavaModel;
		public GotoResourceDialog(Shell parentShell, IContainer container, StructuredViewer viewer) {
			super(parentShell, container, IResource.FILE | IResource.FOLDER | IResource.PROJECT);
			fJavaModel= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
			setTitle(PackagesMessages.getString("GotoResource.dialog.title")); //$NON-NLS-1$
			PlatformUI.getWorkbench().getHelpSystem().setHelp(parentShell, IJavaHelpContextIds.GOTO_RESOURCE_DIALOG);
		}
		protected boolean select(IResource resource) {
			IProject project= resource.getProject();
			try {
				if (project.getNature(JavaCore.NATURE_ID) != null)
					return fJavaModel.contains(resource);
			} catch (CoreException e) {
				// do nothing. Consider resource;
			}
			return true;
		}
	}

	public GotoResourceAction(PackageExplorerPart explorer) {
		setText(PackagesMessages.getString("GotoResource.action.label")); //$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.GOTO_RESOURCE_ACTION);
		fPackageExplorer= explorer;
	}
	
	public void run() {
		TreeViewer viewer= fPackageExplorer.getViewer();
		GotoResourceDialog dialog= new GotoResourceDialog(fPackageExplorer.getSite().getShell(), 
			ResourcesPlugin.getWorkspace().getRoot(), viewer);
	 	dialog.open();
	 	Object[] result = dialog.getResult();
	 	if (result == null || result.length == 0 || !(result[0] instanceof IResource))
	 		return;
	 	StructuredSelection selection= null;
		IJavaElement element = JavaCore.create((IResource)result[0]);
		if (element != null && element.exists())
			selection= new StructuredSelection(element);
		else 
			selection= new StructuredSelection(result[0]);
		viewer.setSelection(selection, true);
	}	
}
