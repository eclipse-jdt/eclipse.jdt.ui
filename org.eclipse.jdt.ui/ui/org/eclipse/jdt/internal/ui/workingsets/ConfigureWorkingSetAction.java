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
package org.eclipse.jdt.internal.ui.workingsets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;
import org.eclipse.ui.internal.WorkbenchPlugin;


public class ConfigureWorkingSetAction extends Action {

	private final Shell fParent;
	private final WorkingSetModel fWorkingSetModel;

	public ConfigureWorkingSetAction(WorkingSetModel workingSetModel, Shell parent) {
		super(WorkingSetMessages.getString("ConfigureWorkingSetAction.label")); //$NON-NLS-1$
		fWorkingSetModel= workingSetModel;
		fParent= parent;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void run() {
		List workingSets= new ArrayList(Arrays.asList(fWorkingSetModel.getWorkingSets()));
    	IWorkingSet[] others= WorkbenchPlugin.getDefault().getWorkingSetManager().getWorkingSets();
    	for (int i= 0; i < others.length; i++) {
			IWorkingSet workingSet= others[i];
			if ("org.eclipse.jdt.ui.JavaWorkingSetPage".equals(workingSet.getId()) && !workingSets.contains(workingSet)) { //$NON-NLS-1$
				workingSets.add(workingSet);
			}
		}
		IWorkingSetSelectionDialog dialog= new WorkingSetSelectionDialog(
			fParent, 
			(IWorkingSet[])workingSets.toArray(new IWorkingSet[workingSets.size()]), 
			true, false); //$NON-NLS-1$
		dialog.setSelection(fWorkingSetModel.getWorkingSets());
		if (dialog.open() == IDialogConstants.OK_ID) {
			IWorkingSet[] selection= dialog.getSelection();
			fWorkingSetModel.setWorkingSets(selection);
		}
	}
}
