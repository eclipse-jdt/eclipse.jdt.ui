/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.ModifyParametersRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveMembersRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoring;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jdt.ui.actions.UnifiedSite;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.actions.GroupContext;
import org.eclipse.jdt.internal.ui.actions.RetargetActionIDs;
import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.ModifyParametersWizard;
import org.eclipse.jdt.internal.ui.refactoring.MoveMembersWizard;
import org.eclipse.jdt.internal.ui.refactoring.PullUpWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.reorg.RenameAction;

/**
 * Refactoring menu group
 */
public class RefactoringGroup extends ContextMenuGroup {
	
	private IAction[] fRefactoringActions;
	
	private SelectionDispatchAction fModifyParametersAction;
	private SelectionDispatchAction fPullUpAction;
	private SelectionDispatchAction fMoveAction;
	
	private boolean fIntitialized= false;
	
	private boolean fCreateSEF;
	
	private UnifiedSite fSite;
	
	public RefactoringGroup(UnifiedSite site) {
		this(true, site);
	}
	
	public RefactoringGroup(boolean createSEF, UnifiedSite site) {
		Assert.isNotNull(site);
		fCreateSEF= createSEF;
		fSite= site;
		
		fModifyParametersAction= createModifyParametersAction(fSite);
		fSite.getSelectionProvider().addSelectionChangedListener(fModifyParametersAction);
		
		fPullUpAction= createPullUpAction(fSite);
		fSite.getSelectionProvider().addSelectionChangedListener(fPullUpAction);
		
		fMoveAction= new NewMoveWrapper(site);
		fSite.getSelectionProvider().addSelectionChangedListener(fMoveAction);
	}
	
	public void fill(IMenuManager manager, GroupContext context) {
		createActions(context.getSelectionProvider());
		
		for (int i= 0; i < fRefactoringActions.length; i++) {
			IAction action= fRefactoringActions[i];
			if (action instanceof IUpdate)
				((IUpdate)action).update();
			if (action.isEnabled())
				manager.add(action);
		}	
	}
	
	public void fillActionBars(IActionBars actionBars) {
		actionBars.setGlobalActionHandler(RetargetActionIDs.MODIFY_PARAMETERS, fModifyParametersAction);
		actionBars.setGlobalActionHandler(RetargetActionIDs.PULL_UP, fPullUpAction);

		actionBars.setGlobalActionHandler(RetargetActionIDs.MOVE, fMoveAction);
	}
	
	private void createActions(ISelectionProvider p) {
		if (fIntitialized)
			return;
		
		StructuredSelectionProvider provider= StructuredSelectionProvider.createFrom(p);	
		
		if (fCreateSEF) {
			fRefactoringActions= new IAction[]{
				fModifyParametersAction,
				fPullUpAction,
				fMoveAction,
				new RenameAction(p),
				new SelfEncapsulateFieldAction(provider)
			};
		} else {
			fRefactoringActions= new IAction[]{
				fModifyParametersAction,
				fPullUpAction,
				fMoveAction,
				new RenameAction(p)
			};
		}
		
		fIntitialized= true;
	}
		
	// -------------------- method refactorings ----------------------

	private static NewOpenRefactoringWizardAction createModifyParametersAction(UnifiedSite site) {
		String label= RefactoringMessages.getString("RefactoringGroup.modify_Parameters_label"); //$NON-NLS-1$
		return new NewOpenRefactoringWizardAction(label, site, IMethod.class) {
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new ModifyParametersRefactoring((IMethod)obj);
			}
			boolean canActivateRefactoring(Refactoring refactoring)  throws JavaModelException{
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

	private static NewOpenRefactoringWizardAction createPullUpAction(UnifiedSite site) {
		String label= RefactoringMessages.getString("RefactoringGroup.pull_Up_label"); //$NON-NLS-1$
		return new NewOpenRefactoringWizardAction(label, site, IMember.class) {
			protected Refactoring createNewRefactoringInstance(Object obj){
				Set memberSet= new HashSet();
				memberSet.addAll(Arrays.asList((Object[])obj));
				IMember[] members= (IMember[]) memberSet.toArray(new IMember[memberSet.size()]);
				return new PullUpRefactoring(members, JavaPreferencesSettings.getCodeGenerationSettings());
			}
			boolean canActivateRefactoring(Refactoring refactoring)  throws JavaModelException{
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

	static NewOpenRefactoringWizardAction createMoveMembersAction(UnifiedSite site) {
		String label= RefactoringMessages.getString("RefactoringGroup.move_label"); //$NON-NLS-1$
		return new NewOpenRefactoringWizardAction(label, site, IMember.class) {
			protected Refactoring createNewRefactoringInstance(Object obj){
				Set memberSet= new HashSet();
				memberSet.addAll(Arrays.asList((Object[])obj));
				IMember[] methods= (IMember[]) memberSet.toArray(new IMember[memberSet.size()]);
				return new MoveMembersRefactoring(methods, JavaPreferencesSettings.getCodeGenerationSettings());
			}
			boolean canActivateRefactoring(Refactoring refactoring)  throws JavaModelException{
				return ((MoveMembersRefactoring)refactoring).checkActivation(new NullProgressMonitor()).isOK();
			}
			protected boolean canOperateOnMultiSelection(){
				return true;
			}	
			protected RefactoringWizard createWizard(Refactoring ref){
				String title= RefactoringMessages.getString("RefactoringGroup.move_Members"); //$NON-NLS-1$
				//FIX ME: wrong
				String helpId= "HELPID"; //$NON-NLS-1$
				return new MoveMembersWizard((MoveMembersRefactoring)ref, title, helpId);
			}
		};
	}	
	
}