/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.rename.RenameParametersRefactoring;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.actions.GroupContext;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RenameParametersWizard;
import org.eclipse.jdt.internal.ui.refactoring.changes.DocumentTextBufferChangeCreator;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.SelectionProviderAction;

/**
 * Refactoring menu group
 */
public class RefactoringGroup extends ContextMenuGroup {
	
	private RefactoringAction[] fOpenWizardActions;
	private boolean fIntitialized= false;
	
	public void fill(IMenuManager manager, GroupContext context) {
		createActions(context.getSelectionProvider());
		
		for (int i= 0; i < fOpenWizardActions.length; i++) {
			RefactoringAction action= fOpenWizardActions[i];
			action.update();
			if (action.isEnabled())
				manager.add(action);
		}	
	}
	
	private void createActions(ISelectionProvider p) {
		if (fIntitialized)
			return;
		
		StructuredSelectionProvider provider= StructuredSelectionProvider.createFrom(p);	
		ITextBufferChangeCreator changeCreator= createChangeCreator();
		
		fOpenWizardActions= new RefactoringAction[]{
			createRenameParametersAction(provider, changeCreator)
		};
		
		fIntitialized= true;
	}
	
	static ITextBufferChangeCreator createChangeCreator(){
		return new DocumentTextBufferChangeCreator(JavaPlugin.getDefault().getCompilationUnitDocumentProvider());
	}
	
	// -------------------- method refactorings ----------------------
	
	static OpenRefactoringWizardAction createRenameParametersAction(StructuredSelectionProvider selectionProvider, final ITextBufferChangeCreator changeCreator) {
		String label= RefactoringMessages.getString("RefactoringGroup.rename_parameters"); //$NON-NLS-1$
		return new OpenRefactoringWizardAction(label, selectionProvider, IMethod.class) {
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new RenameParametersRefactoring(changeCreator, (IMethod)obj);
			}
			boolean canActivateRefactoring(Refactoring refactoring)  throws JavaModelException{
				return ((RenameParametersRefactoring)refactoring).checkPreactivation().isOK();
			}
			protected RefactoringWizard createWizard(Refactoring ref){
				return new RenameParametersWizard((RenameParametersRefactoring)ref);
			}
		};
	}
}