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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.texteditor.MarkerUtilities;

import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

public class AddTaskAction extends SelectionDispatchAction {

	public AddTaskAction(IWorkbenchSite site) {
		super(site);
		setEnabled(false);
	}

	protected void selectionChanged(IStructuredSelection selection) {
		setEnabled(getElement(selection) != null);
	}

	protected void run(IStructuredSelection selection) {
		IResource resource= getElement(selection);
		if (resource == null)
			return;

		InputDialog dialog= new InputDialog(getShell(), getDialogTitle(), ActionMessages.getString("AddTaskAction.inputDialog.message"), "", null); //$NON-NLS-1$ //$NON-NLS-2$
		if (dialog.open() == IDialogConstants.CANCEL_ID)
			return;
			
		String message= dialog.getValue().trim();
		Map attributes= new HashMap(2);
		MarkerUtilities.setMessage(attributes, message);
		try {
			MarkerUtilities.createMarker(resource, attributes, IMarker.TASK);
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), getDialogTitle(), ActionMessages.getString("AddTaskAction.error.create_failed")); //$NON-NLS-1$
		}	
	}
	
	private IResource getElement(IStructuredSelection selection) {
		if (selection.size() != 1)
			return null;

		Object element= selection.getFirstElement();
		if (!(element instanceof IAdaptable))
			return null;
		return (IResource)((IAdaptable)element).getAdapter(IResource.class);
	}
	
	private String getDialogTitle() {
		return ActionMessages.getString("AddTaskAction.dialog.title"); //$NON-NLS-1$
	}
}
