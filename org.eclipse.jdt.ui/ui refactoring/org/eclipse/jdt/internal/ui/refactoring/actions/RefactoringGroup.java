/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.fields.RenameFieldRefactoring;
import org.eclipse.jdt.internal.core.refactoring.methods.RenameMethodRefactoring;
import org.eclipse.jdt.internal.core.refactoring.methods.RenameParametersRefactoring;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.core.refactoring.types.RenameTypeRefactoring;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.actions.GroupContext;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RenameParametersWizard;
import org.eclipse.jdt.internal.ui.refactoring.changes.DocumentTextBufferChangeCreator;
import org.eclipse.jdt.internal.ui.refactoring.undo.RedoRefactoringAction;
import org.eclipse.jdt.internal.ui.refactoring.undo.UndoRefactoringAction;
import org.eclipse.jdt.internal.ui.reorg.RenameAction;
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
	private UndoRefactoringAction fUndoRefactoring;
	private RedoRefactoringAction fRedoRefactoring;
	
	//wizard actions
	private SelectionProviderAction[] fOpenWizardActions;
	
	private boolean fIntitialized= false;
	
	public void fill(IMenuManager manager, GroupContext context) {
		createActions(context.getSelectionProvider());
		
		for (int i= 0; i < fOpenWizardActions.length; i++) {
			if (fOpenWizardActions[i].isEnabled())
				manager.add( fOpenWizardActions[i]);
		}
	
		if (!manager.isEmpty())
			manager.add(new Separator());

		if (fUndoRefactoring.isEnabled()) {
			fUndoRefactoring.update();
			manager.add(fUndoRefactoring);
		}
		
		if (fRedoRefactoring.isEnabled()) {
			fRedoRefactoring.update();
			manager.add(fRedoRefactoring);
		}
		
	}
	
	private void createActions(ISelectionProvider selectionProvider) {
		if (fIntitialized)
			return;
			
		fUndoRefactoring= new UndoRefactoringAction();
		fRedoRefactoring= new RedoRefactoringAction();		
		
		ITextBufferChangeCreator changeCreator= createChangeCreator();
		
		fOpenWizardActions= new  	SelectionProviderAction[]{
			createRenameParametersAction(selectionProvider, changeCreator)
		};
		fIntitialized= true;
		
		ISelection sel= selectionProvider.getSelection();
		if (sel instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection= (IStructuredSelection)sel;
			for (int i= 0; i < fOpenWizardActions.length; i++) {
				fOpenWizardActions[i].selectionChanged(structuredSelection);
			}
		}
	}
	
	private static ITextBufferChangeCreator createChangeCreator(){
		return new DocumentTextBufferChangeCreator(JavaPlugin.getDefault().getCompilationUnitDocumentProvider());
	}
	
	// -------------------- method refactorings ----------------------
	
	private OpenRefactoringWizardAction createRenameParametersAction(ISelectionProvider selectionProvider, final ITextBufferChangeCreator changeCreator) {
		String label= RefactoringMessages.getString("RefactoringGroup.rename_parameters"); //$NON-NLS-1$
		return new OpenRefactoringWizardAction(selectionProvider, label, IMethod.class) {
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new RenameParametersRefactoring(changeCreator, (IMethod)obj);
			}
			protected RefactoringWizard createWizard(){
				return new RenameParametersWizard();
			}
		};
	}
}