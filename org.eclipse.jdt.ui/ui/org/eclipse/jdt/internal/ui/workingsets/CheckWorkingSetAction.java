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
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;

/**
 * Action which allows to select the underlying working set.
 * 
 * @since 2.0
 */
public class CheckWorkingSetAction extends Action {
	private Shell fShell;
	private IWorkingSet fWorkingSet;
	private WorkingSetFilterActionGroup fActionGroup;

	/**
	 * Creates an instance of this class.
	 */
	public CheckWorkingSetAction(String id, WorkingSetFilterActionGroup actionGroup, IWorkingSet workingSet, boolean checked) {
		super(workingSet.getName()); //$NON-NLS-1$
		Assert.isNotNull(actionGroup);
		Assert.isNotNull(id);
		setId(id);
		fActionGroup= actionGroup;
		fWorkingSet= workingSet;
		setChecked(checked);
		
		// XXX
//		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.SELECT_WORKING_SET_ACTION);
//		fShell= shell;
//		fActionGroup= actionGroup;
	}
	
	/*
	 * Overrides method from Action
	 */
	public void run() {
		IWorkingSetManager manager= PlatformUI.getWorkbench().getWorkingSetManager();
		fActionGroup.setWorkingSet(fWorkingSet);
		manager.addRecentWorkingSet(fWorkingSet);
	}
}
