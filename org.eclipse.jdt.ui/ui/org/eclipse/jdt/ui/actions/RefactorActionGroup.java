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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.part.Page;

import org.eclipse.jdt.ui.IContextMenuConstants;

import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.refactoring.actions.ExtractMethodAction;
import org.eclipse.jdt.internal.ui.refactoring.actions.ExtractTempAction;
import org.eclipse.jdt.internal.ui.refactoring.actions.InlineTempAction;
import org.eclipse.jdt.internal.ui.refactoring.actions.ModifyParametersAction;
import org.eclipse.jdt.internal.ui.refactoring.actions.MoveAction;
import org.eclipse.jdt.internal.ui.refactoring.actions.PullUpAction;
import org.eclipse.jdt.internal.ui.refactoring.actions.RenameAction;

/**
 * Action group that adds refactor actions (e.g. Rename..., Move..., etc.)
 * to a context menu and the global menu bar.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class RefactorActionGroup extends ActionGroup {

	private IWorkbenchSite fSite;
	private boolean fIsEditorOwner;

 	private SelectionDispatchAction fSelfEncapsulateField;
 	private SelectionDispatchAction fMoveAction;
	private SelectionDispatchAction fRenameAction;
	private SelectionDispatchAction fModifyParametersAction;
	private SelectionDispatchAction fPullUpAction;
	private SelectionDispatchAction fInlineTempAction;
	private SelectionDispatchAction fExtractTempAction;
	private SelectionDispatchAction fExtractMethodAction;

	/**
	 * Creates a new <code>RefactorActionGroup</code>.
	 * 
	 * @param part the view part that owns this action group
	 */
	public RefactorActionGroup(IViewPart part) {
		this(part.getSite());
	}	
	
	/**
	 * Creates a new <code>RefactorActionGroup</code>.
	 * 
	 * @param page the page that owns this action group
	 */
	public RefactorActionGroup(Page page) {
		this(page.getSite());
	}
	
	/**
	 * Creates a new <code>RefactorActionGroup</code>.
	 * <p>
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * </p>
	 * 
	 * @param editor the editor that owns this action group
	 */
	public RefactorActionGroup(CompilationUnitEditor editor) {
		fSite= editor.getEditorSite();
		fIsEditorOwner= true;
		ISelectionProvider provider= editor.getSelectionProvider();
		ISelection selection= provider.getSelection();
		
		fRenameAction= new RenameAction(editor);
		fRenameAction.update(selection);
		
		fSelfEncapsulateField= new SelfEncapsulateFieldAction(editor);
		fSelfEncapsulateField.update(selection);
		
		fInlineTempAction= new InlineTempAction(editor);
		fInlineTempAction.update(selection);
		
		fExtractTempAction= new ExtractTempAction(editor);
		fExtractTempAction.update(selection);

		fExtractMethodAction= new ExtractMethodAction(editor);
		fExtractMethodAction.update(selection);
		
		fModifyParametersAction= new ModifyParametersAction(editor);
		fModifyParametersAction.update(selection);

		fPullUpAction= new PullUpAction(editor);
		fPullUpAction.update(selection);
		
		fMoveAction= new MoveAction(editor);
		fMoveAction.update(selection);
	}

	private RefactorActionGroup(IWorkbenchSite site) {
		fSite= site;
		fIsEditorOwner= false;
		ISelectionProvider provider= fSite.getSelectionProvider();
		ISelection selection= provider.getSelection();
		
		fMoveAction= new MoveAction(site);
		initAction(fMoveAction, provider, selection);
		
		fRenameAction= new RenameAction(site);
		initAction(fRenameAction, provider, selection);
		
		fModifyParametersAction= new ModifyParametersAction(fSite);
		initAction(fModifyParametersAction, provider, selection);
		
		fPullUpAction= new PullUpAction(fSite);
		initAction(fPullUpAction, provider, selection);
		
		fSelfEncapsulateField= new SelfEncapsulateFieldAction(fSite);
		initAction(fSelfEncapsulateField, provider, selection);
	}

	private static void initAction(SelectionDispatchAction action, ISelectionProvider provider, ISelection selection){
		action.update(selection);
		provider.addSelectionChangedListener(action);
	};
	
	private boolean isEditorOwner() {
		return fIsEditorOwner;
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		actionBars.setGlobalActionHandler(JdtActionConstants.SELF_ENCAPSULATE_FIELD, fSelfEncapsulateField);
		actionBars.setGlobalActionHandler(JdtActionConstants.MOVE, fMoveAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.RENAME, fRenameAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.MODIFY_PARAMETERS, fModifyParametersAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.PULL_UP, fPullUpAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.INLINE_TEMP, fInlineTempAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.EXTRACT_TEMP, fExtractTempAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.EXTRACT_METHOD, fExtractMethodAction);
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		addRefactorSubmenu(menu);
	}
	
	/*
	 * @see ActionGroup#dispose()
	 */
	public void dispose() {
		ISelectionProvider provider= fSite.getSelectionProvider();
		disposeAction(fSelfEncapsulateField, provider);
		disposeAction(fMoveAction, provider);
		disposeAction(fRenameAction, provider);
		disposeAction(fModifyParametersAction, provider);
		disposeAction(fPullUpAction, provider);
		disposeAction(fInlineTempAction, provider);
		disposeAction(fExtractTempAction, provider);
		disposeAction(fExtractMethodAction, provider);
		super.dispose();
	}
	
	private void disposeAction(ISelectionChangedListener action, ISelectionProvider provider) {
		if (action != null)
			provider.removeSelectionChangedListener(action);
	}
	
	private void addRefactorSubmenu(IMenuManager menu) {
		IMenuManager refactorSubmenu= new MenuManager(ActionMessages.getString("RefactorMenu.label"));  //$NON-NLS-1$
		addAction(refactorSubmenu, fMoveAction);
		addAction(refactorSubmenu, fRenameAction);
		addAction(refactorSubmenu, fModifyParametersAction);
		addAction(refactorSubmenu, fPullUpAction);
		addAction(refactorSubmenu, fSelfEncapsulateField);
		if (!refactorSubmenu.isEmpty())
			menu.appendToGroup(IContextMenuConstants.GROUP_REORGANIZE, refactorSubmenu);
	}
	
	private void addAction(IMenuManager menu, IAction action) {
		if (action.isEnabled())
			menu.add(action);
	}
}
