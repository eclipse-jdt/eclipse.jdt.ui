/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved.   This program and the accompanying materials
are made available under the terms of the Common Public License v0.5
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v05.html
 
Contributors:
	Daniel Megert - Initial implementation
**********************************************************************/

package org.eclipse.jdt.internal.ui.workingsets;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetEditWizard;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Displays an IWorkingSetEditWizard for editing a working set.
 * 
 * @since 2.1
 */
public class EditWorkingSetAction extends Action {
	private Shell fShell;
	private IWorkingSet fWorkingSet;
	private WorkingSetFilterActionGroup fActionGroup;

	public EditWorkingSetAction(WorkingSetFilterActionGroup actionGroup, Shell shell) {
		super(WorkingSetMessages.getString("EditWorkingSetAction.text")); //$NON-NLS-1$
		Assert.isNotNull(actionGroup);
		setToolTipText(WorkingSetMessages.getString("EditWorkingSetAction.toolTip")); //$NON-NLS-1$
		
		fShell= shell;
		fActionGroup= actionGroup;
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.EDIT_WORKING_SET_ACTION);
	}
	
	/*
	 * Overrides method from Action
	 */
	public void run() {
		if (fShell == null)
			fShell= JavaPlugin.getActiveWorkbenchShell();
		IWorkingSetManager manager= PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSet workingSet= fActionGroup.getWorkingSet();
		IWorkingSetEditWizard wizard= manager.createWorkingSetEditWizard(workingSet);
		if (wizard == null) {
			String title= WorkingSetMessages.getString("EditWorkingSetAction.error.nowizard.title"); //$NON-NLS-1$
			String message= WorkingSetMessages.getString("EditWorkingSetAction.error.nowizard.message"); //$NON-NLS-1$
			MessageDialog.openError(fShell, title, message);
			return;
		}
		WizardDialog dialog= new WizardDialog(fShell, wizard);
	 	dialog.create();		
		if (dialog.open() == WizardDialog.OK)
			fActionGroup.setWorkingSet(wizard.getSelection(), true);
	}
}
