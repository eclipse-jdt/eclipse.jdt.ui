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

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IInputSelectionProvider;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.part.Page;

import org.eclipse.jdt.internal.ui.actions.GroupContext;
import org.eclipse.jdt.internal.ui.refactoring.actions.IRefactoringAction;
import org.eclipse.jdt.internal.ui.reorg.ReorgGroup;

/**
 * Action group that adds the copy, cut, paste actions to a view parts context
 * menu and retargets the corresponding global menu actions.
 * 
 * @since 2.0
 */
public class CCPActionGroup extends ActionGroup {

	private UnifiedSite fSite;

	private ReorgGroup fOldReorgGroup;
	private GroupContext fOldContext;
 	private IRefactoringAction fDeleteAction;
	
	/**
	 * Creates a new <code>CCPActionGroup</code>.
	 * 
	 * @param part the view part that owns this action group
	 * <p>
	 * Note: this constructor will go away. The final constructor will only take
	 *  an <code>IViewPart</code>
	 * </p>
	 */
	public CCPActionGroup(IViewPart  part, IInputSelectionProvider provider) {
		this(UnifiedSite.create(part.getSite()), provider);
	}
	
	/**
	 * Creates a new <code>CCPActionGroup</code>.
	 * 
	 * @param page the page that owns this action group
	 * <p>
	 * Note: this constructor will go away. The final constructor will only take
	 *  an <code>IViewPart</code>
	 * </p>
	 */
	public CCPActionGroup(Page page, IInputSelectionProvider provider) {
		this(UnifiedSite.create(page.getSite()), provider);
	}
	
	private CCPActionGroup(UnifiedSite site, IInputSelectionProvider provider) {
		fSite= site;
		fOldReorgGroup= new ReorgGroup();
		fOldContext= new GroupContext(provider);
		fDeleteAction= ReorgGroup.createDeleteAction(provider);
	}
	
	/**
	 * Returns the delete action managed by this action
	 * group. 
	 * 
	 * @return the delete action. Returns <code>null</code> if the group
	 * 	doesn't provide any delete action
	 */
	public IRefactoringAction getDeleteAction() {
		return fDeleteAction;
	}

	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		actionBars.setGlobalActionHandler(IWorkbenchActionConstants.DELETE, fDeleteAction);
		ReorgGroup.addGlobalReorgActions(actionBars, fSite.getSelectionProvider());
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		fOldReorgGroup.fill(menu, fOldContext);
	}	
}
