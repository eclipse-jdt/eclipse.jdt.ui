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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.part.Page;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IContextMenuConstants;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.ModifyParametersRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.RetargetActionIDs;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.ModifyParametersWizard;
import org.eclipse.jdt.internal.ui.refactoring.PullUpWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.actions.ExtractMethodAction;
import org.eclipse.jdt.internal.ui.refactoring.actions.ExtractTempAction;
import org.eclipse.jdt.internal.ui.refactoring.actions.InlineTempAction;
import org.eclipse.jdt.internal.ui.refactoring.actions.MoveAction;
import org.eclipse.jdt.internal.ui.refactoring.actions.OpenRefactoringWizardAction;
import org.eclipse.jdt.internal.ui.refactoring.actions.RenameAction;

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

 	private SelfEncapsulateFieldAction fSelfEncapsulateField;
 	private SelectionDispatchAction fMoveAction;
	private SelectionDispatchAction fRenameAction;
	private OpenRefactoringWizardAction fModifyParametersAction;
	private OpenRefactoringWizardAction fPullUpAction;

	private SelectionDispatchAction fInlineTempAction;
	private SelectionDispatchAction fExtractTempAction;
	private SelectionDispatchAction fExtractMethodAction;

	/**
	 * Creates a new <code>RefactorActionGroup</code>.
	 * 
	 * @param part the view part that owns this action group
	 */
	public RefactorActionGroup(IViewPart part) {
		this(UnifiedSite.create(part.getSite()));
	}	
	
	/**
	 * Creates a new <code>RefactorActionGroup</code>.
	 * 
	 * @param page the page that owns this action group
	 */
	public RefactorActionGroup(Page page) {
		this(UnifiedSite.create(page.getSite()));
	}
	
	public RefactorActionGroup(CompilationUnitEditor editor) {
		fRenameAction= new RenameAction(editor);
		initAction(fRenameAction, editor.getSelectionProvider());
		
		fSelfEncapsulateField= new SelfEncapsulateFieldAction(editor);
		initAction(fSelfEncapsulateField, editor.getSelectionProvider());
		
		fInlineTempAction= new InlineTempAction(editor);
		initAction(fInlineTempAction, editor.getSelectionProvider());
		
		fExtractTempAction= new ExtractTempAction(editor);
		initAction(fExtractTempAction, editor.getSelectionProvider());

		fExtractMethodAction= new ExtractMethodAction(editor);
		initAction(fExtractMethodAction, editor.getSelectionProvider());
	}

	private RefactorActionGroup(UnifiedSite site) {
		fSite= site;
		fMoveAction= new MoveAction(site);
		initAction(fMoveAction, fSite.getSelectionProvider());
		
		fRenameAction= new RenameAction(site);
		initAction(fRenameAction, fSite.getSelectionProvider());
		
		fModifyParametersAction= RefactorActionGroup.createModifyParametersAction(fSite);
		initAction(fModifyParametersAction, fSite.getSelectionProvider());
		
		fPullUpAction= RefactorActionGroup.createPullUpAction(fSite);
		initAction(fPullUpAction, fSite.getSelectionProvider());
		
		fSelfEncapsulateField= new SelfEncapsulateFieldAction(fSite);
		fSelfEncapsulateField.update();
		if (!isEditorOwner()) {
			fSite.getSelectionProvider().addSelectionChangedListener(fSelfEncapsulateField);
		}
	}

	private static void initAction(SelectionDispatchAction action, ISelectionProvider provider){
		action.update();
		provider.addSelectionChangedListener(action);
	};
	
	private boolean isEditorOwner() {
		return false;
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		actionBars.setGlobalActionHandler(RetargetActionIDs.SELF_ENCAPSULATE_FIELD, fSelfEncapsulateField);
		actionBars.setGlobalActionHandler(RetargetActionIDs.MOVE, fMoveAction);
		actionBars.setGlobalActionHandler(RetargetActionIDs.RENAME, fRenameAction);
		actionBars.setGlobalActionHandler(RetargetActionIDs.MODIFY_PARAMETERS, fModifyParametersAction);
		actionBars.setGlobalActionHandler(RetargetActionIDs.PULL_UP, fPullUpAction);
		actionBars.setGlobalActionHandler(RetargetActionIDs.INLINE_TEMP, fInlineTempAction);
		actionBars.setGlobalActionHandler(RetargetActionIDs.EXTRACT_TEMP, fExtractTempAction);
		actionBars.setGlobalActionHandler(RetargetActionIDs.EXTRACT_METHOD, fExtractMethodAction);
	}
	
	/* (non-Javadoc)
	 * Method declared in ActionGroup
	 */
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		menu.appendToGroup(IContextMenuConstants.GROUP_REORGANIZE, fMoveAction);
		menu.appendToGroup(IContextMenuConstants.GROUP_REORGANIZE, fRenameAction);
		addRefactorSubmenu(menu);
	}
	
	private void addRefactorSubmenu(IMenuManager menu) {
		IMenuManager refactorSubmenu= new MenuManager(ActionMessages.getString("RefactorMenu.label"));  //$NON-NLS-1$
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
	
	//----
	private static OpenRefactoringWizardAction createModifyParametersAction(UnifiedSite site) {
		String label= RefactoringMessages.getString("RefactoringGroup.modify_Parameters_label"); //$NON-NLS-1$
		return new OpenRefactoringWizardAction(label, site, IMethod.class) {
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new ModifyParametersRefactoring((IMethod)obj);
			}
			protected boolean canActivateRefactoring(Refactoring refactoring)  throws JavaModelException{
				return ((ModifyParametersRefactoring)refactoring).checkPreactivation().isOK();
			}
			protected RefactoringWizard createWizard(Refactoring ref){
				String title= RefactoringMessages.getString("RefactoringGroup.modify_method_parameters"); //$NON-NLS-1$
				//FIX ME: wrong
				String helpId= IJavaHelpContextIds.RENAME_PARAMS_ERROR_WIZARD_PAGE;
				return new ModifyParametersWizard((ModifyParametersRefactoring)ref, title, helpId);
			}
		};
	}

	private static OpenRefactoringWizardAction createPullUpAction(UnifiedSite site) {
		String label= RefactoringMessages.getString("RefactoringGroup.pull_Up_label"); //$NON-NLS-1$
		return new OpenRefactoringWizardAction(label, site, IMember.class) {
			protected Refactoring createNewRefactoringInstance(Object obj){
				Set memberSet= new HashSet();
				memberSet.addAll(Arrays.asList((Object[])obj));
				IMember[] members= (IMember[]) memberSet.toArray(new IMember[memberSet.size()]);
				return new PullUpRefactoring(members, JavaPreferencesSettings.getCodeGenerationSettings());
			}
			protected boolean canActivateRefactoring(Refactoring refactoring)  throws JavaModelException{
				return ((PullUpRefactoring)refactoring).checkPreactivation().isOK();
			}
			protected boolean canOperateOnMultiSelection(){
				return true;
			}	
			protected RefactoringWizard createWizard(Refactoring ref){
				String title= RefactoringMessages.getString("RefactoringGroup.pull_up"); //$NON-NLS-1$
				//FIX ME: wrong
				String helpId= "HELPID"; //$NON-NLS-1$
				return new PullUpWizard((PullUpRefactoring)ref, title, helpId);
			}
		};
	}
	
}
