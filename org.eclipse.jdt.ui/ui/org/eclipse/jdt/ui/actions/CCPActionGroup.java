/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.actions;

import org.eclipse.swt.dnd.Clipboard;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IInputSelectionProvider;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.ui.IContextMenuConstants;

import org.eclipse.jdt.internal.ui.reorg.ReorgActionFactory;

/**
 * Action group that adds the copy, cut, paste actions to a view parts context
 * menu and installs handlers for the corresponding global menu actions.
 * 
 * @since 2.0
 */
public class CCPActionGroup extends ActionGroup {

	private IWorkbenchSite fSite;
	private Clipboard fClipboard;

 	private SelectionDispatchAction[] fActions;

 	private SelectionDispatchAction fDeleteAction;
	private SelectionDispatchAction fCopyAction;
	private SelectionDispatchAction fPasteAction;
	private SelectionDispatchAction fCutAction;
	
	/**
	 * Creates a new <code>CCPActionGroup</code>.
	 * 
	 * @param part the view part that owns this action group
	 */
	public CCPActionGroup(IViewPart  part) {
		this(part.getSite());
	}
	
	/**
	 * Creates a new <code>CCPActionGroup</code>.
	 * 
	 * @param page the page that owns this action group
	 */
	public CCPActionGroup(Page page) {
		this(page.getSite());
	}

	private CCPActionGroup(IWorkbenchSite site) {
		fSite= site;
		fClipboard= new Clipboard(site.getShell().getDisplay());
		fPasteAction= ReorgActionFactory.createPasteAction(fSite, fClipboard);
		fCopyAction= ReorgActionFactory.createCopyAction(fSite, fClipboard, fPasteAction);
		fActions= new SelectionDispatchAction[] {	
			fCutAction= ReorgActionFactory.createCutAction(fSite, fClipboard, fPasteAction),
			fCopyAction,
			fPasteAction,
			fDeleteAction= ReorgActionFactory.createDeleteAction(fSite),
		};
		registerActionsAsSelectionChangeListeners();
	}

	private void registerActionsAsSelectionChangeListeners() {
		ISelectionProvider provider = fSite.getSelectionProvider();
		for (int i= 0; i < fActions.length; i++) {
			provider.addSelectionChangedListener(fActions[i]);
		}
	}
	
	private void deregisterActionsAsSelectionChangeListeners() {
		ISelectionProvider provider = fSite.getSelectionProvider();
		for (int i= 0; i < fActions.length; i++) {
			provider.removeSelectionChangedListener(fActions[i]);
		}
	}
	
	
	/**
	 * Returns the delete action managed by this action group. 
	 * 
	 * @return the delete action. Returns <code>null</code> if the group
	 * 	doesn't provide any delete action
	 */
	public IAction getDeleteAction() {
		return fDeleteAction;
	}

	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.DELETE, fDeleteAction);
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.COPY, fCopyAction);
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.CUT, fCutAction);
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.PASTE, fPasteAction);
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		for (int i= 0; i < fActions.length; i++) {
			menu.appendToGroup(IContextMenuConstants.GROUP_REORGANIZE, fActions[i]);
		}		
	}		
	
	/*
	 * @see ActionGroup#dispose()
	 */
	public void dispose() {
		super.dispose();
		if (fClipboard != null){
			fClipboard.dispose();
			fClipboard= null;
		}
		deregisterActionsAsSelectionChangeListeners();
	}

}
