/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.views.tasklist.TaskPropertiesDialog;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

public class AddTaskAction extends SelectionDispatchAction {

	public AddTaskAction(IWorkbenchSite site) {
		super(site);
		setEnabled(false);
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.ADD_TASK_ACTION);
	}

	protected void selectionChanged(IStructuredSelection selection) {
		setEnabled(getElement(selection) != null);
	}

	protected void run(IStructuredSelection selection) {
		IResource resource= getElement(selection);
		if (resource == null)
			return;

		TaskPropertiesDialog dialog= new TaskPropertiesDialog(getShell());
		dialog.setResource(resource);
		dialog.open();
	}
	
	private IResource getElement(IStructuredSelection selection) {
		if (selection.size() != 1)
			return null;

		Object element= selection.getFirstElement();
		if (!(element instanceof IAdaptable))
			return null;
		return (IResource)((IAdaptable)element).getAdapter(IResource.class);
	}	
}
