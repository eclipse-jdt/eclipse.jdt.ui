/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation 
 *          (report 36180: Callers/Callees view)
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jface.action.Action;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

class SelectWorkingSetAction extends Action {
	private final SearchScopeActionGroup fGroup;
	
	public SelectWorkingSetAction(SearchScopeActionGroup group) {
		super(CallHierarchyMessages.getString("SearchScopeActionGroup.workingset.select.text")); //$NON-NLS-1$
		this.fGroup = group;
		setToolTipText(CallHierarchyMessages.getString("SearchScopeActionGroup.workingset.select.tooltip")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_SEARCH_SCOPE_ACTION);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		try {
			IWorkingSet[] workingSets;
			workingSets = JavaSearchScopeFactory.getInstance().queryWorkingSets();
			if (workingSets != null) {
				this.fGroup.setActiveWorkingSets(workingSets);
				SearchUtil.updateLRUWorkingSets(workingSets);
			} else {
				this.fGroup.setActiveWorkingSets(null);
			}
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, JavaPlugin.getActiveWorkbenchShell(), 
					CallHierarchyMessages.getString("SelectWorkingSetAction.queryWorkingSets.title"), //$NON-NLS-1$
					CallHierarchyMessages.getString("SelectWorkingSetAction.queryWorkingSets.message")); //$NON-NLS-1$
		}
	}
}
