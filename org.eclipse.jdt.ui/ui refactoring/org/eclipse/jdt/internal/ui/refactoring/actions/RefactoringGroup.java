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
import org.eclipse.jdt.internal.ui.refactoring.changes.DocumentTextBufferChangeCreator;
import org.eclipse.jdt.internal.ui.refactoring.undo.RedoRefactoringAction;
import org.eclipse.jdt.internal.ui.refactoring.undo.UndoRefactoringAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Refactoring menu group
 */
public class RefactoringGroup extends ContextMenuGroup {

	private UndoRefactoringAction fUndoRefactoring;
	private RedoRefactoringAction fRedoRefactoring;
	
	//wizard actions
	private OpenRefactoringWizardAction[] openWizardActions;
	
	private boolean fIntitialized= false;
	
	private boolean isSelectionOk(ISelectionProvider selectionProvider){
		if (selectionProvider == null)
			return false;	
		
		ISelection selection= selectionProvider.getSelection();
		if (! (selection instanceof IStructuredSelection))
			return false;
		else if (((IStructuredSelection)selection).size() != 1)
			return false;
		else
			return true;		
	}
	
	public void fill(IMenuManager manager, GroupContext context) {
	
		ISelectionProvider selectionProvider= context.getSelectionProvider();
		if (!isSelectionOk(selectionProvider))
			return;
		
		createActions();
		
		for (int i= 0; i < openWizardActions.length; i++) {
			OpenRefactoringWizardAction action= openWizardActions[i];
			if (action != null && action.canActionBeAdded())
				manager.add(action);
		}
	
		if (!manager.isEmpty())
			manager.add(new Separator());

		if (fUndoRefactoring.canActionBeAdded()) {
			fUndoRefactoring.update();
			manager.add(fUndoRefactoring);
		}
		
		if (fRedoRefactoring.canActionBeAdded()) {
			fRedoRefactoring.update();
			manager.add(fRedoRefactoring);
		}
		
	}
	
	private void createActions() {
		if (fIntitialized)
			return;
			
		fUndoRefactoring= new UndoRefactoringAction();
		fRedoRefactoring= new RedoRefactoringAction();		
		
		ITextBufferChangeCreator changeCreator= createChangeCreator();
		
		openWizardActions= new OpenRefactoringWizardAction[]{
			createRenameParametersAction(changeCreator),
			createRenameMethodAction(changeCreator),
			createRenameTypeAction(changeCreator),
			createRenameFieldAction(changeCreator)
		};
		fIntitialized= true;
	}
	
	private static ITextBufferChangeCreator createChangeCreator(){
		return new DocumentTextBufferChangeCreator(JavaPlugin.getDefault().getCompilationUnitDocumentProvider());
	}
	
	// -------------------- method refactorings ----------------------
	
	private OpenRefactoringWizardAction createRenameParametersAction(final ITextBufferChangeCreator changeCreator) {
		String label= RefactoringMessages.getString("RefactoringGroup.rename_parameters"); //$NON-NLS-1$
		return new OpenRefactoringWizardAction(label, IMethod.class) {
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new RenameParametersRefactoring(changeCreator, (IMethod)obj);
			}
		};
	}
	
	private OpenRefactoringWizardAction createRenameMethodAction(final ITextBufferChangeCreator changeCreator) {
		String label= RefactoringMessages.getString("RefactoringGroup.rename"); //$NON-NLS-1$
		return new OpenRefactoringWizardAction(label, IMethod.class){
			protected Refactoring createNewRefactoringInstance(Object obj) throws JavaModelException{
				return RenameMethodRefactoring.createInstance(changeCreator, (IMethod)obj);
			}
		};
	}
	
	// -------------------- type refactorings ----------------------
	private OpenRefactoringWizardAction createRenameTypeAction(final ITextBufferChangeCreator changeCreator) {	
		String label= RefactoringMessages.getString("RefactoringGroup.rename"); //$NON-NLS-1$
		return new OpenRefactoringWizardAction(label, IType.class){
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new RenameTypeRefactoring(changeCreator, (IType)obj);
			}
		};		
	}
		
	// -------------------- field refactorings ----------------------
	
	private OpenRefactoringWizardAction createRenameFieldAction(final ITextBufferChangeCreator changeCreator) {	
		String label= RefactoringMessages.getString("RefactoringGroup.rename"); //$NON-NLS-1$
		return new OpenRefactoringWizardAction(label, IField.class){
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new RenameFieldRefactoring(changeCreator, (IField)obj);
			}
		};
	}
}