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

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;

import org.eclipse.ui.IWorkbenchPartSite;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.util.JavaUIHelp;

class LocationViewer extends TableViewer {
    private final String columnHeaders[] = {
        CallHierarchyMessages.getString("LocationViewer.ColumnIcon.header"),//$NON-NLS-1$
        CallHierarchyMessages.getString("LocationViewer.ColumnLine.header"),//$NON-NLS-1$
        CallHierarchyMessages.getString("LocationViewer.ColumnInfo.header")}; //$NON-NLS-1$
                                                
    private ColumnLayoutData columnLayouts[] = {
        new ColumnPixelData(19, false),
        new ColumnWeightData(60),
        new ColumnWeightData(300)};
    

    LocationViewer(Composite parent) {
        super(createTable(parent));

        setContentProvider(new ArrayContentProvider());
        setLabelProvider(new LocationLabelProvider());
        setInput(new ArrayList());

        createColumns();
        
        JavaUIHelp.setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_VIEW);
    }

    /**
     * Creates the table control.
     */
    private static Table createTable(Composite parent) {
        return new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
    }
    
    private void createColumns() {
        TableLayout layout = new TableLayout();
        getTable().setLayout(layout);
        getTable().setHeaderVisible(true);
        for (int i = 0; i < columnHeaders.length; i++) {
            layout.addColumnData(columnLayouts[i]);
            TableColumn tc = new TableColumn(getTable(), SWT.NONE,i);
            tc.setResizable(columnLayouts[i].resizable);
            tc.setText(columnHeaders[i]);
        }
    }

    /**
     * Attaches a contextmenu listener to the tree
     */
    void initContextMenu(IMenuListener menuListener, String popupId, IWorkbenchPartSite viewSite) {
        MenuManager menuMgr= new MenuManager();
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(menuListener);
        Menu menu= menuMgr.createContextMenu(getControl());
        getControl().setMenu(menu);
        viewSite.registerContextMenu(popupId, menuMgr, this);
    }

    /**
     * 
     */
    void clearViewer() {
        setInput(""); //$NON-NLS-1$
    }
}
