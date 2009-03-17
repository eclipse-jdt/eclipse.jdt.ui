/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.ui.part.Page;


import org.eclipse.jdt.internal.ui.actions.CopyQualifiedNameAction;
import org.eclipse.jdt.internal.ui.refactoring.reorg.CopyToClipboardAction;
import org.eclipse.jdt.internal.ui.refactoring.reorg.CutAction;
import org.eclipse.jdt.internal.ui.refactoring.reorg.DeleteAction;
import org.eclipse.jdt.internal.ui.refactoring.reorg.PasteAction;

/**
 * Action group that adds the copy, cut, paste actions to a view part's context
 * menu and installs handlers for the corresponding global menu actions.
 *
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * @since 2.0
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class CCPActionGroup extends ActionGroup {

 	private final SelectionDispatchAction[] fActions;

 	private final SelectionDispatchAction fDeleteAction;
	private final SelectionDispatchAction fCopyAction;
	private final SelectionDispatchAction fCopyQualifiedNameAction;
	private final SelectionDispatchAction fPasteAction;
	private final SelectionDispatchAction fCutAction;
	private final ISelectionProvider fSelectionProvider;

	/**
	 * Creates a new <code>CCPActionGroup</code>. The group requires that
	 * the selection provided by the view part's selection provider is of type
	 * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 *
	 * @param part the view part that owns this action group
	 */
	public CCPActionGroup(IViewPart  part) {
		this(part.getSite(), null);
	}

	/**
	 * Creates a new <code>CCPActionGroup</code>.  The group requires that
	 * the selection provided by the page's selection provider is of type
	 * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 *
	 * @param page the page that owns this action group
	 */
	public CCPActionGroup(Page page) {
		this(page.getSite(), null);
	}

	/**
	 * Creates a new <code>CCPActionGroup</code>. The group requires
	 * that the selection provided by the given selection provider is of type
	 * {@link IStructuredSelection}.
	 *
	 * @param site the site that will own the action group.
	 * @param specialSelectionProvider the selection provider used instead of the
	 *  sites selection provider.
	 *
	 * @since 3.4
	 */
	public CCPActionGroup(IWorkbenchSite site, ISelectionProvider specialSelectionProvider) {
		fSelectionProvider= specialSelectionProvider == null ? site.getSelectionProvider() : specialSelectionProvider;

		fPasteAction= new PasteAction(site);
		fPasteAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_PASTE);

		fCopyAction= new CopyToClipboardAction(site);
		fCopyAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);

		fCopyQualifiedNameAction= new CopyQualifiedNameAction(site);
		fCopyQualifiedNameAction.setActionDefinitionId(CopyQualifiedNameAction.ACTION_DEFINITION_ID);

		fCutAction= new CutAction(site);
		fCutAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_CUT);

		fDeleteAction= new DeleteAction(site);
		fDeleteAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_DELETE);

		fActions= new SelectionDispatchAction[] { fCutAction, fCopyAction, fCopyQualifiedNameAction, fPasteAction, fDeleteAction };
		if (specialSelectionProvider != null) {
			for (int i= 0; i < fActions.length; i++) {
				fActions[i].setSpecialSelectionProvider(specialSelectionProvider);
			}
		}

		registerActionsAsSelectionChangeListeners();
	}

	private void registerActionsAsSelectionChangeListeners() {
		ISelectionProvider provider= fSelectionProvider;
		ISelection selection= provider.getSelection();
		for (int i= 0; i < fActions.length; i++) {
			SelectionDispatchAction action= fActions[i];
			action.update(selection);
			provider.addSelectionChangedListener(action);
		}
	}

	private void deregisterActionsAsSelectionChangeListeners() {
		ISelectionProvider provider= fSelectionProvider;
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
		actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), fDeleteAction);
		actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), fCopyAction);
		actionBars.setGlobalActionHandler(CopyQualifiedNameAction.ACTION_HANDLER_ID, fCopyQualifiedNameAction);
		actionBars.setGlobalActionHandler(ActionFactory.CUT.getId(), fCutAction);
		actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), fPasteAction);
	}

	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		for (int i= 0; i < fActions.length; i++) {
			SelectionDispatchAction action= fActions[i];
			if (action == fCutAction && !fCutAction.isEnabled())
				continue;
			menu.appendToGroup(ICommonMenuConstants.GROUP_EDIT, action);
		}
	}

	/*
	 * @see ActionGroup#dispose()
	 */
	public void dispose() {
		super.dispose();
		deregisterActionsAsSelectionChangeListeners();
	}

}
