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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;

import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkingSet;

public class ConfigureWorkingSetAction extends Action {

	private final IWorkbenchPartSite fSite;
	private WorkingSetModel fWorkingSetModel;

	public ConfigureWorkingSetAction(IWorkbenchPartSite site) {
		super(WorkingSetMessages.getString("ConfigureWorkingSetAction.label")); //$NON-NLS-1$
		fSite= site;
	}
	
	public void setWorkingSetModel(WorkingSetModel model) {
		fWorkingSetModel= model;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void run() {
		List workingSets= new ArrayList(Arrays.asList(fWorkingSetModel.getAllWorkingSets()));
		WorkingSetConfigurationDialog dialog= new WorkingSetConfigurationDialog(
			fSite.getShell(), 
			(IWorkingSet[])workingSets.toArray(new IWorkingSet[workingSets.size()])); //$NON-NLS-1$
		dialog.setSelection(fWorkingSetModel.getActiveWorkingSets());
		if (dialog.open() == IDialogConstants.OK_ID) {
			IWorkingSet[] selection= dialog.getSelection();
			fWorkingSetModel.setActiveWorkingSets(selection);
		}
	}
}
