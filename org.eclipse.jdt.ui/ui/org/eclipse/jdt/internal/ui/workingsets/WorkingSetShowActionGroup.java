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
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionGroup;

public class WorkingSetShowActionGroup extends ActionGroup implements IWorkingSetActionGroup {
	
	private List fContributions= new ArrayList();
	private final WorkingSetModel fWorkingSetMode;
	private final Shell fShell; 
	
	public WorkingSetShowActionGroup(WorkingSetModel workingSetMode, Shell shell) {
		fWorkingSetMode= workingSetMode;
		fShell= shell;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		IMenuManager menuManager= actionBars.getMenuManager();
		fillViewMenu(menuManager);
	}
	
	public void fillViewMenu(IMenuManager menuManager) {
		ConfigureWorkingSetAction configureWorkingSetAction= 
			new ConfigureWorkingSetAction(fWorkingSetMode, fShell);
		addAction(menuManager, configureWorkingSetAction);
	}
	
	public void cleanViewMenu(IMenuManager menuManager) {
		for (Iterator iter= fContributions.iterator(); iter.hasNext();) {
			menuManager.remove((IContributionItem)iter.next());
		}
		fContributions.clear();
	}

	private void addAction(IMenuManager menuManager, Action action) {
		IContributionItem item= new ActionContributionItem(action);
		menuManager.appendToGroup(ACTION_GROUP, item);
		fContributions.add(item);
	}
}
