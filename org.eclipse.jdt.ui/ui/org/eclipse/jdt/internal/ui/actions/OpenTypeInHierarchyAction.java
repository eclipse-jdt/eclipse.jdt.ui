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
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.OpenTypeSelectionDialog;
import org.eclipse.jdt.internal.ui.util.OpenTypeHierarchyUtil;

public class OpenTypeInHierarchyAction extends Action implements IWorkbenchWindowActionDelegate {
	
	private IWorkbenchWindow fWindow;
	
	public OpenTypeInHierarchyAction() {
		super();
		setText(ActionMessages.getString("OpenTypeInHierarchyAction.label")); //$NON-NLS-1$
		setDescription(ActionMessages.getString("OpenTypeInHierarchyAction.description")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("OpenTypeInHierarchyAction.tooltip")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.OPEN_TYPE_IN_HIERARCHY_ACTION);
	}

	public void run() {
		Shell parent= JavaPlugin.getActiveWorkbenchShell();
		OpenTypeSelectionDialog dialog= new OpenTypeSelectionDialog(parent, new ProgressMonitorDialog(parent), 
			IJavaSearchConstants.TYPE, SearchEngine.createWorkspaceScope());
		
		dialog.setMatchEmptyString(true);	
		dialog.setTitle(ActionMessages.getString("OpenTypeInHierarchyAction.dialogTitle")); //$NON-NLS-1$
		dialog.setMessage(ActionMessages.getString("OpenTypeInHierarchyAction.dialogMessage")); //$NON-NLS-1$
		int result= dialog.open();
		if (result != IDialogConstants.OK_ID)
			return;
		
		Object[] types= dialog.getResult();
		if (types != null && types.length > 0) {
			IType type= (IType)types[0];
			OpenTypeHierarchyUtil.open(new IType[] { type }, fWindow);
		}
	}

	//---- IWorkbenchWindowActionDelegate ------------------------------------------------

	public void run(IAction action) {
		run();
	}
	
	public void dispose() {
		fWindow= null;
	}
	
	public void init(IWorkbenchWindow window) {
		fWindow= window;
	}
	
	public void selectionChanged(IAction action, ISelection selection) {
		// do nothing. Action doesn't depend on selection.
	}
}
