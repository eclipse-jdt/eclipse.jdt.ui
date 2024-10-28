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
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation
 *          (report 36180: Callers/Callees view)
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionGroup;

/**
 * Action group to add the filter actions to a view part's toolbar
 * menu.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public class CallHierarchyFiltersActionGroup extends ActionGroup {

    class ShowExpandWithConstructorsDialogAction extends Action {
    	ShowExpandWithConstructorsDialogAction() {
    		setText(CallHierarchyMessages.ShowExpandWithConstructorsDialogAction_text);
    	}

    	@Override
		public void run() {
    		openExpandWithConstructorsDialog();
    	}
    }

    private CallHierarchyViewPart fPart;

    /**
     * Creates a new <code>CustomFiltersActionGroup</code>.
     *
	 * @param part the call hierarchy view part
	 * @param viewer the call hierarchy viewer
     */
    public CallHierarchyFiltersActionGroup(CallHierarchyViewPart part, CallHierarchyViewer viewer) {
        Assert.isNotNull(part);
        Assert.isNotNull(viewer);
        fPart= part;
    }

    @Override
	public void fillActionBars(IActionBars actionBars) {
        fillViewMenu(actionBars.getMenuManager());
    }

    private void fillViewMenu(IMenuManager viewMenu) {
        viewMenu.add(new Separator("filters")); //$NON-NLS-1$
        viewMenu.add(new ShowCallHierarchyFilterDialogAction(fPart, null));
        viewMenu.add(new ShowExpandWithConstructorsDialogAction());
    }

    @Override
	public void dispose() {
        super.dispose();
    }

    // ---------- dialog related code ----------

    private void openExpandWithConstructorsDialog() {
    	Shell parentShell= fPart.getViewSite().getShell();
		new ExpandWithConstructorsDialog(parentShell).open();
    }
}
