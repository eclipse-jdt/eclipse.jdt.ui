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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IInputSelectionProvider;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.ui.IContextMenuConstants;

import org.eclipse.jdt.internal.ui.reorg.ReorgGroup;

/**
 * Action group that adds the copy, cut, paste actions to a view parts context
 * menu and retargets the corresponding global menu actions.
 * 
 * @since 2.0
 */
public class CCPActionGroup extends ActionGroup {

	private UnifiedSite fSite;

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
		this(UnifiedSite.create(part.getSite()));
	}
	
	public CCPActionGroup(Page page) {
		this(UnifiedSite.create(page.getSite()));
	}

	private CCPActionGroup(UnifiedSite site) {
		fSite= site;
		fActions= new SelectionDispatchAction[] {	
			fCutAction= ReorgGroup.createCutAction(fSite, fSite.getSelectionProvider()),
			fCopyAction= ReorgGroup.createCopyAction(fSite, fSite.getSelectionProvider()),
			fPasteAction= ReorgGroup.createPasteAction(fSite, fSite.getSelectionProvider()),
			fDeleteAction= ReorgGroup.createDeleteAction(fSite, fSite.getSelectionProvider()),
		};
	}
	
	/**
	 * Returns the delete action managed by this action
	 * group. 
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
			fActions[i].update();
			menu.appendToGroup(IContextMenuConstants.GROUP_REORGANIZE, fActions[i]);
		}		
	}		
}
