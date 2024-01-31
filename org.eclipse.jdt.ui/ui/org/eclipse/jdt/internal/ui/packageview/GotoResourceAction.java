/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

public class GotoResourceAction extends Action {

	private PackageExplorerPart fPackageExplorer;

	private static class GotoResourceDialog extends FilteredResourcesSelectionDialog {
		private IJavaModel fJavaModel;
		public GotoResourceDialog(Shell parentShell, IContainer container) {
			super(parentShell, false, container, IResource.FILE | IResource.FOLDER | IResource.PROJECT);
			fJavaModel= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
			setTitle(PackagesMessages.GotoResource_dialog_title);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(parentShell, IJavaHelpContextIds.GOTO_RESOURCE_DIALOG);
		}

		@Override
		protected ItemsFilter createFilter() {
			return new GotoResourceFilter();
		}

		private class GotoResourceFilter extends ResourceFilter {

			@Override
			public boolean matchItem(Object item) {
				IResource resource = (IResource) item;
				return super.matchItem(item) && select(resource);
			}

			/**
			 * This is the orignal <code>select</code> method. Since
			 * <code>GotoResourceDialog</code> needs to extend
			 * <code>FilteredResourcesSelectionDialog</code> result of this
			 * method must be combined with the <code>matchItem</code> method
			 * from super class (<code>ResourceFilter</code>).
			 *
			 * @param resource
			 *            A resource
			 * @return <code>true</code> if item matches against given
			 *         conditions <code>false</code> otherwise
			 */
			private boolean select(IResource resource) {
				IProject project= resource.getProject();
				try {
					if (project.hasNature(JavaCore.NATURE_ID))
						return fJavaModel.contains(resource);
				} catch (CoreException e) {
					// do nothing. Consider resource;
				}
				return true;
			}

			@Override
			public boolean equalsFilter(ItemsFilter filter) {
				if (!super.equalsFilter(filter)) {
					return false;
				}
				if (!(filter instanceof GotoResourceFilter)) {
					return false;
				}
				return true;
			}
		}

	}

	public GotoResourceAction(PackageExplorerPart explorer) {
		setText(PackagesMessages.GotoResource_action_label);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.GOTO_RESOURCE_ACTION);
		fPackageExplorer= explorer;
	}

	@Override
	public void run() {
		TreeViewer viewer= fPackageExplorer.getTreeViewer();
		GotoResourceDialog dialog= new GotoResourceDialog(fPackageExplorer.getSite().getShell(),
			ResourcesPlugin.getWorkspace().getRoot());
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
