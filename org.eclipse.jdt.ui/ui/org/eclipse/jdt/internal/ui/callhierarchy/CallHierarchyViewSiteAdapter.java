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

import org.eclipse.core.runtime.PlatformObject;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IKeyBindingService;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * This class adapts a Call Hierarchy view site.
 * It converts selection of Call Hierarchy view entries to
 * be a selection of Java elements.
 * 
 */
class CallHierarchyViewSiteAdapter extends PlatformObject implements IViewSite {
    
    private ISelectionProvider fProvider;
    private IWorkbenchSite fSite;
    
    public CallHierarchyViewSiteAdapter(IWorkbenchSite site){
        fSite= site;
        setSelectionProvider(site.getSelectionProvider());
    }
    
    public IWorkbenchPage getPage() {
        return fSite.getPage();
    }

    public ISelectionProvider getSelectionProvider() {
        return fProvider;
    }

    public Shell getShell() {
        return fSite.getShell();
    }

    public IWorkbenchWindow getWorkbenchWindow() {
        return fSite.getWorkbenchWindow();
    }
    
    public void setSelectionProvider(final ISelectionProvider provider) {
        Assert.isNotNull(provider);
        fProvider= new SelectionProviderAdapter(provider);
    }

    // --------- only empty stubs below ---------
        
    /*
     * @see org.eclipse.ui.IWorkbenchPartSite#getId()
     */
    public String getId() {
        return null;
    }

    /*
     * @see org.eclipse.ui.IWorkbenchPartSite#getKeyBindingService()
     */
    public IKeyBindingService getKeyBindingService() {
        return null;
    }

    /*
     * @see org.eclipse.ui.IWorkbenchPartSite#getPluginId()
     */
    public String getPluginId() {
        return null;
    }

    /*
     * @see org.eclipse.ui.IWorkbenchPartSite#getRegisteredName()
     */
    public String getRegisteredName() {
        return null;
    }

    /*
     * @see org.eclipse.ui.IWorkbenchPartSite#registerContextMenu(MenuManager, ISelectionProvider)
     */
    public void registerContextMenu(MenuManager menuManager, ISelectionProvider selectionProvider) {
    }

    /*
     * @see org.eclipse.ui.IWorkbenchPartSite#registerContextMenu(String, MenuManager, ISelectionProvider)
     */
    public void registerContextMenu(String menuId, MenuManager menuManager, ISelectionProvider selectionProvider) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IViewSite#getActionBars()
     */
    public IActionBars getActionBars() {
        return null;
    }
}
