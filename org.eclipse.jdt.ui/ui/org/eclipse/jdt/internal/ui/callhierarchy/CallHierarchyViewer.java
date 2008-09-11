/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation
 * 			(report 36180: Callers/Callees view)
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.ui.IWorkbenchPartSite;

import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

import org.eclipse.jdt.internal.ui.viewsupport.ColoringLabelProvider;


class CallHierarchyViewer extends TreeViewer {

	private CallHierarchyViewPart fPart;
	private CallHierarchyContentProvider fContentProvider;


    /**
     * @param parent the parent composite
     * @param part the call hierarchy view part
     */
    CallHierarchyViewer(Composite parent, CallHierarchyViewPart part) {
        super(new Tree(parent, SWT.MULTI));

        fPart = part;

        getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
        setUseHashlookup(true);
        setAutoExpandLevel(2);
        fContentProvider = new CallHierarchyContentProvider(fPart);
        setContentProvider(fContentProvider);
        setLabelProvider(new ColoringLabelProvider(new CallHierarchyLabelProvider()));


        clearViewer();
    }

    void setMethodWrappers(MethodWrapper[] wrappers) {
        setInput(getTreeRoot(wrappers));

        setFocus();
        if (wrappers != null && wrappers.length > 0)
        	setSelection(new StructuredSelection(wrappers[0]), true);
    }

    CallHierarchyViewPart getPart() {
        return fPart;
    }

    /**
     *
     */
    void setFocus() {
        getControl().setFocus();
    }

    boolean isInFocus() {
        return getControl().isFocusControl();
    }

    void addKeyListener(KeyListener keyListener) {
        getControl().addKeyListener(keyListener);
    }

    /**
     * Wraps the roots of a MethodWrapper tree in a dummy root in order to show
     * it in the tree.
     *
     * @param roots The visible roots of the MethodWrapper tree.
     * @return A new TreeRoot which is a dummy root above the specified root.
     */
    private TreeRoot getTreeRoot(MethodWrapper[] roots) {
        TreeRoot dummyRoot = new TreeRoot(roots);

        return dummyRoot;
    }

    /**
     * Attaches a context menu listener to the tree
     * @param menuListener the menu listener
     * @param viewSite the view site
     * @param selectionProvider the selection provider
     */
    void initContextMenu(IMenuListener menuListener, IWorkbenchPartSite viewSite, ISelectionProvider selectionProvider) {
        MenuManager menuMgr= new MenuManager();
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(menuListener);
        Menu menu= menuMgr.createContextMenu(getTree());
        getTree().setMenu(menu);
        viewSite.registerContextMenu(menuMgr, selectionProvider);
    }

    void clearViewer() {
        setInput(TreeRoot.EMPTY_ROOT);
    }

    void cancelJobs() {
    	if (fPart == null)
    		return;
        fContentProvider.cancelJobs(fPart.getCurrentMethodWrappers());
    }

}
