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
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IInputSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.part.Page;

import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.GroupContext;
import org.eclipse.jdt.internal.ui.actions.RetargetActionIDs;
import org.eclipse.jdt.internal.ui.packageview.PackagesMessages;
import org.eclipse.jdt.internal.ui.refactoring.actions.IRefactoringAction;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringGroup;
import org.eclipse.jdt.internal.ui.reorg.ReorgGroup;

import org.eclipse.jdt.ui.IContextMenuConstants;

/**
 * Action group that adds the reorganize (cut, copy, paste, ..) and the refactor actions 
 * to a context menu and the global menu bar.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class RefactorActionGroup extends ActionGroup {

	private UnifiedSite fSite;

	private RefactoringGroup fOldRefactorGroup;
	private GroupContext fOldContext;
 	
 	private SelfEncapsulateFieldAction fSelfEncapsulateField;

	/**
	 * Creates a new <code>RefactorActionGroup</code>.
	 * 
	 * @param part the view part that owns this action group
	 * <p>
	 * Note: this constructor will go away. The final constructor will only take
	 *  an <code>IViewPart</code>
	 * </p>
	 */
	public RefactorActionGroup(IViewPart part, IInputSelectionProvider provider) {
		this(UnifiedSite.create(part.getSite()), provider);
	}
	
	/**
	 * Creates a new <code>RefactorActionGroup</code>.
	 * 
	 * @param page the page that owns this action group
	 * <p>
	 * Note: this constructor will go away. The final constructor will only take
	 *  an <code>IViewPart</code>
	 * </p>
	 */
	public RefactorActionGroup(Page page, IInputSelectionProvider provider) {
		this(UnifiedSite.create(page.getSite()), provider);
	}
	
	private RefactorActionGroup(UnifiedSite site, IInputSelectionProvider provider) {
		fSite= site;
		fOldRefactorGroup= new RefactoringGroup(false);
		fOldContext= new GroupContext(provider);
		
		ISelectionProvider sp= fSite.getSelectionProvider();
		ISelection selection= sp.getSelection();
		fSelfEncapsulateField= new SelfEncapsulateFieldAction(fSite);
		fSelfEncapsulateField.update(selection);
		if (!isEditorOwner()) {
			sp.addSelectionChangedListener(fSelfEncapsulateField);
		}
	}

	private boolean isEditorOwner() {
		return false;
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		actionBars.setGlobalActionHandler(RetargetActionIDs.SELF_ENCAPSULATE_FIELD, fSelfEncapsulateField);
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		addRefactorMenu(menu);
	}
	
	private void addRefactorMenu(IMenuManager menu) {
		IMenuManager refactorMenu= new MenuManager(ActionMessages.getString("RefactorMenu.label"));  //$NON-NLS-1$
		fOldRefactorGroup.fill(refactorMenu, fOldContext);
		addAction(refactorMenu, fSelfEncapsulateField);
		if (!refactorMenu.isEmpty())
			menu.appendToGroup(IContextMenuConstants.GROUP_REORGANIZE, refactorMenu);
	}
	
	private void addAction(IMenuManager menu, IAction action) {
		if (action.isEnabled())
			menu.add(action);
	}
}
