/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveMembersRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.ModifyParametersRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.actions.GroupContext;
import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.MoveMembersWizard;
import org.eclipse.jdt.internal.ui.refactoring.PullUpWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.ModifyParametersWizard;

/**
 * Refactoring menu group
 */
public class RefactoringGroup extends ContextMenuGroup {
	
	private RefactoringAction[] fRefactoringActions;
	private boolean fIntitialized= false;
	
	public void fill(IMenuManager manager, GroupContext context) {
		createActions(context.getSelectionProvider());
		
		for (int i= 0; i < fRefactoringActions.length; i++) {
			RefactoringAction action= fRefactoringActions[i];
			action.update();
			if (action.isEnabled())
				manager.add(action);
		}	
	}
	
	private void createActions(ISelectionProvider p) {
		if (fIntitialized)
			return;
		
		StructuredSelectionProvider provider= StructuredSelectionProvider.createFrom(p);	
		
		fRefactoringActions= new RefactoringAction[]{
			createModifyParametersAction(provider),
			createPullUpAction(provider),
			createMoveMembersAction(provider),
			new SelfEncapsulateFieldAction(provider)
		};
		
		fIntitialized= true;
	}
		
	// -------------------- method refactorings ----------------------
	
	static OpenRefactoringWizardAction createModifyParametersAction(StructuredSelectionProvider selectionProvider) {
		String label= "M&odify Parameters...";
		return new OpenRefactoringWizardAction(label, selectionProvider, IMethod.class) {
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new ModifyParametersRefactoring((IMethod)obj);
			}
			boolean canActivateRefactoring(Refactoring refactoring)  throws JavaModelException{
				return ((ModifyParametersRefactoring)refactoring).checkPreactivation().isOK();
			}
			protected RefactoringWizard createWizard(Refactoring ref){
				String title= "Modify Method Parameters";
				//FIX ME: wrong
				String helpId= IJavaHelpContextIds.RENAME_PARAMS_ERROR_WIZARD_PAGE;
				return new ModifyParametersWizard((ModifyParametersRefactoring)ref, title, helpId);
			}
		};
	}
	
	static OpenRefactoringWizardAction createPullUpAction(StructuredSelectionProvider selectionProvider) {
		String label= "Pull &Up...";
		return new OpenRefactoringWizardAction(label, selectionProvider, IMember.class) {
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
				String title= "Pull up";
				//FIX ME: wrong
				String helpId= "HELPID";
				return new PullUpWizard((PullUpRefactoring)ref, title, helpId);
			}
		};
	}
	
	static OpenRefactoringWizardAction createMoveMembersAction(StructuredSelectionProvider selectionProvider) {
		String label= "&Move...";
		return new OpenRefactoringWizardAction(label, selectionProvider, IMember.class) {
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
				String title= "Move Members";
				//FIX ME: wrong
				String helpId= "HELPID";
				return new MoveMembersWizard((MoveMembersRefactoring)ref, title, helpId);
			}
		};
	}	
}