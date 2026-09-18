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
 * 			(report 36180: Callers/Callees view)
 *   Stephan Herrmann (stephan@cs.tu-berlin.de):
 *          - bug 75800: [call hierarchy] should allow searches for fields
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.window.Window;

import org.eclipse.jdt.core.IMember;

import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchyCore;

import org.eclipse.jdt.internal.ui.JavaPluginImages;


class FilterDropDownAction extends Action implements IMenuCreator {

    private CallHierarchyViewPart fView;
    private Menu fMenu;
    private String activeFilterString;

    public FilterDropDownAction(CallHierarchyViewPart view) {
        fView = view;
        fMenu = null;


        updateFilterString();
        setToolTipText(activeFilterString);
		setImageDescriptor(JavaPluginImages.DESC_ELCL_FILTER);

		setText(CallHierarchyMessages.ShowFilterDialogAction_text);

        setMenuCreator(this);
    }

    @Override
	public Menu getMenu(Menu parent) {
        return null;
    }

    public void setActiveFilterString() {
    	updateFilterString();
    }

    private void updateFilterString() {
    	activeFilterString = getString(CallHierarchyCore.getDefault().getCurrentSelection());
    	setToolTipText(activeFilterString);
    }

    private String getString(String s) {
    	if(s == CallHierarchyCore.PREF_SHOW_ALL_CODE) {
    		return "Show All Code"; //$NON-NLS-1$
    	} else if(s == CallHierarchyCore.PREF_HIDE_TEST_CODE) {
    		return "Hide Test Code"; //$NON-NLS-1$
    	} else {
    		return "Test Code Only"; //$NON-NLS-1$
    	}
    }
    @Override
	public Menu getMenu(Control parent) {
        if (fMenu != null) {
            fMenu.dispose();
        }
        fMenu= new Menu(parent);
        IMember[][] elements= fView.getHistoryEntries();
        addEntries(fMenu);
        return fMenu;
    }

    @Override
	public void dispose() {
        fView = null;

        if (fMenu != null) {
            fMenu.dispose();
            fMenu = null;
        }
    }

    protected void addActionsToMenu(Menu parent, Action action) {
        ActionContributionItem item = new ActionContributionItem(action);
        item.fill(parent, -1);
    }

    private boolean addEntries(Menu menu) {
        boolean checked = false;

        FiltersAction action = new FiltersAction(fView, CallHierarchyCore.PREF_SHOW_ALL_CODE);
        addActionsToMenu(menu, action);
        action.setChecked(CallHierarchy.getDefault().isShowAll());

        FiltersAction actionTwo = new FiltersAction(fView, CallHierarchyCore.PREF_HIDE_TEST_CODE);
        addActionsToMenu(menu, actionTwo);
        actionTwo.setChecked(CallHierarchy.getDefault().isHideTestCode());


        FiltersAction actionThree = new FiltersAction(fView, CallHierarchyCore.PREF_SHOW_TEST_CODE_ONLY);
        addActionsToMenu(menu, actionThree);
        actionThree.setChecked(CallHierarchy.getDefault().isShowTestCode());

		new MenuItem(menu, SWT.SEPARATOR);
		addActionsToMenu(menu, new ShowCallHierarchyFilterDialogAction(fView, "Filters")); //$NON-NLS-1$



        return checked;

    }

    @Override
	public void run() {
    	openFiltersDialog();
    	updateFilterString();
//        activeFilterString = CallHierarchyCore.getDefault().getActiveFilter();
//        setToolTipText(activeFilterString);
    }

    private void openFiltersDialog() {
    	FiltersDialog dialog = new FiltersDialog(fView.getViewSite().getShell());
    	if (Window.OK == dialog.open()) {
			fView.refresh();
		}
    }
}
