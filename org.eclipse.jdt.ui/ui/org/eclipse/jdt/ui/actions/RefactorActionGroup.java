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
import org.eclipse.jface.action.Separator;
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
	private String fGroupName= IContextMenuConstants.GROUP_REORGANIZE;

 	private SelectionDispatchAction fSelfEncapsulateField;
 	private SelectionDispatchAction fMoveAction;
	private SelectionDispatchAction fRenameAction;
	private SelectionDispatchAction fModifyParametersAction;
	private SelectionDispatchAction fPullUpAction;
	private SelectionDispatchAction fInlineTempAction;
	private SelectionDispatchAction fExtractTempAction;
	private SelectionDispatchAction fExtractMethodAction;
	private SelectionDispatchAction fExtractInterfaceAction;
	private SelectionDispatchAction fMoveInnerToTopAction;
	private SelectionDispatchAction fUseSupertypeAction;
	private SelectionDispatchAction fInlineCallAction;	
	private SelectionDispatchAction fExtractConstantAction;
	
	/**
	 * Creates a new <code>RefactorActionGroup</code>. The group requires
	 * that the selection provided by the part's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param part the view part that owns this action group
	 */
	public RefactorActionGroup(IViewPart part) {
		this(part.getSite());
	}	
	
	/**
	 * Creates a new <code>RefactorActionGroup</code>. The action requires
	 * that the selection provided by the page's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param page the page that owns this action group
	 */
	public RefactorActionGroup(Page page) {
		this(page.getSite());
	}
	
	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public RefactorActionGroup(CompilationUnitEditor editor, String groupName) {
		fSite= editor.getEditorSite();
		fIsEditorOwner= true;
		fGroupName= groupName;
		ISelectionProvider provider= editor.getSelectionProvider();
		ISelection selection= provider.getSelection();
		
		fRenameAction= new RenameAction(editor);
		fRenameAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.RENAME_ELEMENT);
		fRenameAction.update(selection);
		editor.setAction("RenameElement", fRenameAction); //$NON-NLS-1$
		
		fSelfEncapsulateField= new SelfEncapsulateFieldAction(editor);
		fSelfEncapsulateField.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELF_ENCAPSULATE_FIELD);
		fSelfEncapsulateField.update(selection);
		editor.setAction("SelfEncapsulateField", fSelfEncapsulateField); //$NON-NLS-1$
		
		fModifyParametersAction= new ModifyParametersAction(editor);
		fModifyParametersAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.MODIFY_METHOD_PARAMETERS);
		fModifyParametersAction.update(selection);
		editor.setAction("ModifyParameters", fModifyParametersAction); //$NON-NLS-1$

		fPullUpAction= new PullUpAction(editor);
		fPullUpAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.PULL_UP);
		fPullUpAction.update(selection);
		editor.setAction("PullUp", fPullUpAction); //$NON-NLS-1$
		
		fMoveAction= new MoveAction(editor);
		fMoveAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.MOVE_ELEMENT);
		fMoveAction.update(selection);
		editor.setAction("MoveElement", fMoveAction); //$NON-NLS-1$
		
		fInlineTempAction= new InlineTempAction(editor);
		fInlineTempAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.INLINE_LOCAL_VARIABLE);
		fInlineTempAction.update(selection);
		editor.setAction("InlineLocalVariable", fInlineTempAction); //$NON-NLS-1$
		
		fExtractTempAction= new ExtractTempAction(editor);
		fExtractTempAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.EXTRACT_LOCAL_VARIABLE);
		initAction(fExtractTempAction, provider, selection);
		editor.setAction("ExtractLocalVariable", fExtractTempAction); //$NON-NLS-1$

		fExtractConstantAction= new ExtractConstantAction(editor);
		fExtractConstantAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.EXTRACT_CONSTANT);
		initAction(fExtractConstantAction, provider, selection);
		editor.setAction("ExtractConstant", fExtractConstantAction); //$NON-NLS-1$

		fExtractMethodAction= new ExtractMethodAction(editor);
		fExtractMethodAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.EXTRACT_METHOD);
		initAction(fExtractMethodAction, provider, selection);
		editor.setAction("ExtractMethod", fExtractMethodAction); //$NON-NLS-1$

		fExtractInterfaceAction= new ExtractInterfaceAction(editor);
		fExtractInterfaceAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.EXTRACT_INTERFACE);
		fExtractInterfaceAction.update(selection);
		editor.setAction("ExtractInterface", fExtractInterfaceAction); //$NON-NLS-1$

		fMoveInnerToTopAction= new MoveInnerToTopAction(editor);
		fMoveInnerToTopAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.MOVE_INNER_TO_TOP);
		fMoveInnerToTopAction.update(selection);
		editor.setAction("MoveInnerToTop", fMoveInnerToTopAction); //$NON-NLS-1$
		
		fUseSupertypeAction= new UseSupertypeAction(editor);
		fUseSupertypeAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.USE_SUPERTYPE);
		fUseSupertypeAction.update(selection);
		editor.setAction("UseSupertype", fUseSupertypeAction); //$NON-NLS-1$
		
		fInlineCallAction= new InlineMethodAction(editor);
		fInlineCallAction.update(selection);
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

		fExtractInterfaceAction= new ExtractInterfaceAction(fSite);
		initAction(fExtractInterfaceAction, provider, selection);

		fMoveInnerToTopAction= new MoveInnerToTopAction(fSite);
		initAction(fMoveInnerToTopAction, provider, selection);

		fUseSupertypeAction= new UseSupertypeAction(fSite);
		initAction(fUseSupertypeAction, provider, selection);
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
		actionBars.setGlobalActionHandler(JdtActionConstants.EXTRACT_CONSTANT, fExtractConstantAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.EXTRACT_METHOD, fExtractMethodAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.EXTRACT_INTERFACE, fExtractInterfaceAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.MOVE_INNER_TO_TOP, fMoveInnerToTopAction);
		actionBars.setGlobalActionHandler(JdtActionConstants.USE_SUPERTYPE, fUseSupertypeAction);
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
		disposeAction(fExtractConstantAction, provider);
		disposeAction(fExtractMethodAction, provider);
		disposeAction(fExtractInterfaceAction, provider);
		disposeAction(fMoveInnerToTopAction, provider);
		disposeAction(fUseSupertypeAction, provider);
		disposeAction(fInlineCallAction, provider);
		super.dispose();
	}
	
	private void disposeAction(ISelectionChangedListener action, ISelectionProvider provider) {
		if (action != null)
			provider.removeSelectionChangedListener(action);
	}
	
	private void addRefactorSubmenu(IMenuManager menu) {
		IMenuManager refactorSubmenu= new MenuManager(ActionMessages.getString("RefactorMenu.label"));  //$NON-NLS-1$
		addAction(refactorSubmenu, fRenameAction);
		addAction(refactorSubmenu, fMoveAction);
		addAction(refactorSubmenu, fPullUpAction);
		addAction(refactorSubmenu, fModifyParametersAction);
		addAction(refactorSubmenu, fExtractInterfaceAction);
		addAction(refactorSubmenu, fUseSupertypeAction);
		addAction(refactorSubmenu, fMoveInnerToTopAction);
		if (! refactorSubmenu.isEmpty())
			refactorSubmenu.add(new Separator());
		addAction(refactorSubmenu, fExtractMethodAction);
//		addAction(refactorSubmenu, fInlineCallAction);
		addAction(refactorSubmenu, fExtractTempAction);
		addAction(refactorSubmenu, fExtractConstantAction);
		addAction(refactorSubmenu, fInlineTempAction);
		addAction(refactorSubmenu, fSelfEncapsulateField);
		if (!refactorSubmenu.isEmpty())
			menu.appendToGroup(fGroupName, refactorSubmenu);
	}
	
	private void addAction(IMenuManager menu, IAction action) {
		if (action != null && action.isEnabled())
			menu.add(action);
	}
}
