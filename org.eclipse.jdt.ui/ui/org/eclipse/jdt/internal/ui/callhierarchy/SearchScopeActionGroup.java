/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation 
 * 			(report 36180: Callers/Callees view)
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.IContextMenuConstants;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;

class SearchScopeActionGroup extends ActionGroup {
    private SearchScopeAction fSelectedAction = null;
    private String fSelectedWorkingSetName = null;
    private CallHierarchyViewPart fView;
    private SearchScopeHierarchyAction fSearchScopeHierarchyAction;
    private SearchScopeProjectAction fSearchScopeProjectAction;
    private SearchScopeWorkspaceAction fSearchScopeWorkspaceAction;
    private SelectWorkingSetAction fSelectWorkingSetAction;

    private abstract class SearchScopeAction extends Action {
        public SearchScopeAction(String text) {
            super(text);
        }

        public abstract IJavaSearchScope getSearchScope();

        public void run() {
            setSelected(this);
        }
    }

    private class SearchScopeHierarchyAction extends SearchScopeAction {
        public SearchScopeHierarchyAction() {
            super(CallHierarchyMessages.getString("SearchScopeActionGroup.hierarchy.text")); //$NON-NLS-1$
            setToolTipText(CallHierarchyMessages.getString("SearchScopeActionGroup.hierarchy.tooltip")); //$NON-NLS-1$
            WorkbenchHelp.setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_SEARCH_SCOPE_ACTION);
        }

        public IJavaSearchScope getSearchScope() {
            try {
                IMethod method = getView().getMethod();

                if (method != null) {
                    return SearchEngine.createHierarchyScope(method.getDeclaringType());
                } else {
                    return null;
                }
            } catch (JavaModelException e) {
                JavaPlugin.log(e);
            }

            return null;
        }
    }

    private class SearchScopeProjectAction extends SearchScopeAction {
        public SearchScopeProjectAction() {
            super(CallHierarchyMessages.getString("SearchScopeActionGroup.project.text")); //$NON-NLS-1$
            setToolTipText(CallHierarchyMessages.getString("SearchScopeActionGroup.project.tooltip")); //$NON-NLS-1$
            WorkbenchHelp.setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_SEARCH_SCOPE_ACTION);
        }

        public IJavaSearchScope getSearchScope() {
            IMethod method = getView().getMethod();
            IJavaProject project = null;

            if (method != null) {
                project = method.getJavaProject();
            }

            if (project != null) {
                return SearchEngine.createJavaSearchScope(new IJavaElement[] { project },
                    false);
            } else {
                return null;
            }
        }
    }

    private class SearchScopeWorkingSetAction extends SearchScopeAction {
        private IWorkingSet mWorkingSet;

        public SearchScopeWorkingSetAction(IWorkingSet workingSet) {
            super(workingSet.getName());
            setToolTipText(CallHierarchyMessages.getString("SearchScopeActionGroup.workingset.tooltip")); //$NON-NLS-1$
            WorkbenchHelp.setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_SEARCH_SCOPE_ACTION);

            this.mWorkingSet = workingSet;
        }

        public IJavaSearchScope getSearchScope() {
            return SearchEngine.createJavaSearchScope(getJavaElements(
                    mWorkingSet.getElements()));
        }

        /**
         *
         */
        public IWorkingSet getWorkingSet() {
            return mWorkingSet;
        }

        /**
         * @param adaptables
         * @return IResource[]
         */
        private IJavaElement[] getJavaElements(IAdaptable[] adaptables) {
            Collection result = new ArrayList();

            for (int i = 0; i < adaptables.length; i++) {
                IJavaElement element = (IJavaElement) adaptables[i].getAdapter(IJavaElement.class);

                if (element != null) {
                    result.add(element);
                }
            }

            return (IJavaElement[]) result.toArray(new IJavaElement[result.size()]);
        }
    }

    private class SearchScopeWorkspaceAction extends SearchScopeAction {
        public SearchScopeWorkspaceAction() {
            super(CallHierarchyMessages.getString("SearchScopeActionGroup.workspace.text")); //$NON-NLS-1$
            setToolTipText(CallHierarchyMessages.getString("SearchScopeActionGroup.workspace.tooltip")); //$NON-NLS-1$
            WorkbenchHelp.setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_SEARCH_SCOPE_ACTION);
        }

        public IJavaSearchScope getSearchScope() {
            return SearchEngine.createWorkspaceScope();
        }
    }

    private class SelectWorkingSetAction extends Action {
        public SelectWorkingSetAction() {
            super(CallHierarchyMessages.getString("SearchScopeActionGroup.workingset.select.text")); //$NON-NLS-1$
            setToolTipText(CallHierarchyMessages.getString("SearchScopeActionGroup.workingset.select.tooltip")); //$NON-NLS-1$
            WorkbenchHelp.setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_SEARCH_SCOPE_ACTION);
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.action.Action#run()
         */
        public void run() {
            IWorkingSetManager workingSetManager = getWorkingSetManager();
            IWorkingSetSelectionDialog dialog = workingSetManager.createWorkingSetSelectionDialog(PlatformUI.getWorkbench()
                                                                                                            .getActiveWorkbenchWindow()
                                                                                                            .getShell(),
                    false);
            IWorkingSet workingSet = getActiveWorkingSet();

            if (workingSet != null) {
                dialog.setSelection(new IWorkingSet[] { workingSet });
            }

            if (dialog.open() == Window.OK) {
                IWorkingSet[] result = dialog.getSelection();

                if ((result != null) && (result.length > 0)) {
                    setActiveWorkingSet(result[0]);
                    workingSetManager.addRecentWorkingSet(result[0]);
                } else {
                    setActiveWorkingSet(null);
                }
            }
        }
    }

    public SearchScopeActionGroup(CallHierarchyViewPart view) {
        this.fView = view;
        createActions();
    }

    /**
     * @return IJavaSearchScope
     */
    public IJavaSearchScope getSearchScope() {
        if (fSelectedAction != null) {
            return fSelectedAction.getSearchScope();
        }

        return null;
    }

    public void fillActionBars(IActionBars actionBars) {
        super.fillActionBars(actionBars);
        fillViewMenu(actionBars.getMenuManager());
    }

    public void fillContextMenu(IMenuManager menu) {
    }

    /**
     * @param set
     * @param b
     */
    protected void setActiveWorkingSet(IWorkingSet set) {
        if (set != null) {
            fSelectedWorkingSetName = set.getName();
            fSelectedAction = new SearchScopeWorkingSetAction(set);
        } else {
            fSelectedWorkingSetName = null;
            fSelectedAction = null;
        }
    }

    /**
     * @return
     */
    protected IWorkingSet getActiveWorkingSet() {
        if (fSelectedWorkingSetName != null) {
            return getWorkingSetManager().getWorkingSet(fSelectedWorkingSetName);
        }

        return null;
    }

    protected void setSelected(SearchScopeAction newSelection) {
        if (newSelection instanceof SearchScopeWorkingSetAction) {
            fSelectedWorkingSetName = ((SearchScopeWorkingSetAction) newSelection).getWorkingSet()
                                       .getName();
        } else {
            fSelectedWorkingSetName = null;
        }

        fSelectedAction = newSelection;
    }

    /**
     * @return CallHierarchyViewPart
     */
    protected CallHierarchyViewPart getView() {
        return fView;
    }

    protected IWorkingSetManager getWorkingSetManager() {
        IWorkingSetManager workingSetManager = PlatformUI.getWorkbench()
                                                         .getWorkingSetManager();

        return workingSetManager;
    }

    protected void fillSearchActions(IMenuManager javaSearchMM) {
        javaSearchMM.removeAll();

        Action[] actions = getActions();

        for (int i = 0; i < actions.length; i++) {
            Action action = actions[i];

            if (action.isEnabled()) {
                javaSearchMM.add(action);
            }
        }

        javaSearchMM.setVisible(!javaSearchMM.isEmpty());
    }

    void fillViewMenu(IMenuManager menu) {
        menu.add(new Separator(IContextMenuConstants.GROUP_SEARCH));

        MenuManager javaSearchMM = new MenuManager(CallHierarchyMessages.getString("SearchScopeActionGroup.searchScope"), //$NON-NLS-1$
                IContextMenuConstants.GROUP_SEARCH);

        javaSearchMM.addMenuListener(new IMenuListener() {
                /* (non-Javadoc)
                 * @see org.eclipse.jface.action.IMenuListener#menuAboutToShow(org.eclipse.jface.action.IMenuManager)
                 */
                public void menuAboutToShow(IMenuManager manager) {
                    fillSearchActions(manager);
                }
            });
        menu.appendToGroup(IContextMenuConstants.GROUP_SEARCH, javaSearchMM);
    }

    /**
     * @return SearchScopeAction[]
     */
    private Action[] getActions() {
        List actions = new ArrayList();
        addAction(actions, fSearchScopeWorkspaceAction);
        addAction(actions, fSearchScopeProjectAction);
        addAction(actions, fSearchScopeHierarchyAction);
        addAction(actions, fSelectWorkingSetAction);

        IWorkingSetManager workingSetManager = PlatformUI.getWorkbench()
                                                         .getWorkingSetManager();
        IWorkingSet[] sets = workingSetManager.getRecentWorkingSets();

        for (int i = 0; i < sets.length; i++) {
            SearchScopeWorkingSetAction workingSetAction = new SearchScopeWorkingSetAction(sets[i]);

            if (sets[i].getName().equals(fSelectedWorkingSetName)) {
                workingSetAction.setChecked(true);
            }

            actions.add(workingSetAction);
        }

        return (Action[]) actions.toArray(new Action[actions.size()]);
    }

    private void addAction(List actions, Action action) {
        if (action == fSelectedAction) {
            action.setChecked(true);
        } else {
            action.setChecked(false);
        }

        actions.add(action);
    }

    /**
     * @param view
     */
    private void createActions() {
        fSearchScopeWorkspaceAction = new SearchScopeWorkspaceAction();
        fSelectWorkingSetAction = new SelectWorkingSetAction();
        fSearchScopeHierarchyAction = new SearchScopeHierarchyAction();
        fSearchScopeProjectAction = new SearchScopeProjectAction();

        setSelected(fSearchScopeWorkspaceAction);
    }
}
