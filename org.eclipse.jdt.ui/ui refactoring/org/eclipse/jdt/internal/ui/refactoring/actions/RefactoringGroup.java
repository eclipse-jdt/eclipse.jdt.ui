/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.texteditor.IDocumentProvider;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.cus.MoveCompilationUnitRefactoring;
import org.eclipse.jdt.internal.core.refactoring.cus.RenameCompilationUnitRefactoring;import org.eclipse.jdt.internal.core.refactoring.fields.RenameFieldRefactoring;
import org.eclipse.jdt.internal.core.refactoring.methods.RenameMethodInInterfaceRefactoring;
import org.eclipse.jdt.internal.core.refactoring.methods.RenameParametersRefactoring;import org.eclipse.jdt.internal.core.refactoring.methods.RenamePrivateMethodRefactoring;
import org.eclipse.jdt.internal.core.refactoring.methods.RenameStaticMethodRefactoring;
import org.eclipse.jdt.internal.core.refactoring.methods.RenameVirtualMethodRefactoring;
import org.eclipse.jdt.internal.core.refactoring.packages.RenamePackageRefactoring;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.core.refactoring.types.RenameTypeRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;import org.eclipse.jdt.internal.ui.IPreferencesConstants;import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;import org.eclipse.jdt.internal.ui.actions.AbstractOpenWizardAction;
import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.actions.GroupContext;
import org.eclipse.jdt.internal.ui.refactoring.AbstractOpenRefactoringWizardAction;
import org.eclipse.jdt.internal.ui.refactoring.MoveCompilationUnitWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RenameParametersWizard;import org.eclipse.jdt.internal.ui.refactoring.RenameRefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.changes.DocumentTextBufferChangeCreator;
import org.eclipse.jdt.internal.ui.refactoring.undo.RedoRefactoringAction;
import org.eclipse.jdt.internal.ui.refactoring.undo.UndoRefactoringAction;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;

/**
 * Refactoring menu group
 */
public class RefactoringGroup extends ContextMenuGroup {

	private UndoRefactoringAction fUndoRefactoring;
	private RedoRefactoringAction fRedoRefactoring;
	
	//wizard actions
	private AbstractOpenWizardAction[] openWizardActions;
	
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
			AbstractOpenWizardAction action= openWizardActions[i];
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
		
		IDocumentProvider documentProvider= JavaPlugin.getDefault().getCompilationUnitDocumentProvider();
		ITextBufferChangeCreator changeCreator= new DocumentTextBufferChangeCreator(documentProvider);
		
		openWizardActions= new AbstractOpenRefactoringWizardAction[]{
			createRenameParametersAction(changeCreator),
			createRenameMethodInClassAction(changeCreator),
			createRenamePrivateMethodAction(changeCreator),
			createRenameStaticMethodAction(changeCreator),
			createRenameMethodInInterfaceAction(changeCreator),
			createRenameTypeAction(changeCreator),
			createRenameFieldAction(changeCreator),
			createMoveCompilationUnitAction(changeCreator),
		};
		fIntitialized= true;
	}
	
	// -------------------- method refactorings ----------------------
	
	private AbstractOpenRefactoringWizardAction createRenameParametersAction(final ITextBufferChangeCreator changeCreator) {
		String label= RefactoringMessages.getString("RefactoringGroup.rename_parameters"); //$NON-NLS-1$
		return new AbstractOpenRefactoringWizardAction(label, IMethod.class) {
			protected Wizard createWizard() {
				return new RenameParametersWizard();
			}
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new RenameParametersRefactoring(changeCreator, (IMethod)obj);
			}
		};
	}
	
	private AbstractOpenRefactoringWizardAction createRenameMethodInClassAction(final ITextBufferChangeCreator changeCreator) {
		String label= RefactoringMessages.getString("RefactoringGroup.rename"); //$NON-NLS-1$
		return new AbstractOpenRefactoringWizardAction(label, IMethod.class) {
			protected Wizard createWizard() {
				String title= RefactoringMessages.getString("RefactoringGroup.rename_method_title"); //$NON-NLS-1$
				String message= RefactoringMessages.getString("RefactoringGroup.rename_method_message"); //$NON-NLS-1$
				RenameRefactoringWizard w= new RenameRefactoringWizard(title, message, IJavaHelpContextIds.RENAME_METHOD_WIZARD_PAGE, IJavaHelpContextIds.RENAME_METHOD_ERROR_WIZARD_PAGE);
				w.setInputPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_METHOD);
				return w;
			}
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new RenameVirtualMethodRefactoring(changeCreator, (IMethod)obj);
			}
		};
	}

	private AbstractOpenRefactoringWizardAction createRenameMethodInInterfaceAction(final ITextBufferChangeCreator changeCreator) {
		String label= RefactoringMessages.getString("RefactoringGroup.rename"); //$NON-NLS-1$
		return new AbstractOpenRefactoringWizardAction(label, IMethod.class) {
			protected Wizard createWizard() {
				String title= RefactoringMessages.getString("RefactoringGroup.rename_method_title"); //$NON-NLS-1$
				String message= RefactoringMessages.getString("RefactoringGroup.rename_method_message"); //$NON-NLS-1$
				RenameRefactoringWizard w= new RenameRefactoringWizard(title, message, IJavaHelpContextIds.RENAME_METHOD_WIZARD_PAGE, IJavaHelpContextIds.RENAME_METHOD_ERROR_WIZARD_PAGE);
				w.setInputPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_METHOD);
				return w;
			}
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new RenameMethodInInterfaceRefactoring(changeCreator, (IMethod)obj);
			}
		};
	}
	
	private AbstractOpenRefactoringWizardAction createRenamePrivateMethodAction(final ITextBufferChangeCreator changeCreator) {
		String label= RefactoringMessages.getString("RefactoringGroup.rename"); //$NON-NLS-1$
		return new AbstractOpenRefactoringWizardAction(label, IMethod.class) {
			protected Wizard createWizard() {
				String title= RefactoringMessages.getString("RefactoringGroup.rename_method_title"); //$NON-NLS-1$
				String message= RefactoringMessages.getString("RefactoringGroup.rename_method_message"); //$NON-NLS-1$
				RenameRefactoringWizard w= new RenameRefactoringWizard(title, message, IJavaHelpContextIds.RENAME_METHOD_WIZARD_PAGE, IJavaHelpContextIds.RENAME_METHOD_ERROR_WIZARD_PAGE);
				w.setInputPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_METHOD);
				return w;
			}
			
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new RenamePrivateMethodRefactoring(changeCreator, (IMethod)obj);
			}
		};
	}
	
	private AbstractOpenRefactoringWizardAction createRenameStaticMethodAction(final ITextBufferChangeCreator changeCreator) {
		String label= RefactoringMessages.getString("RefactoringGroup.rename"); //$NON-NLS-1$
		return new AbstractOpenRefactoringWizardAction(label, IMethod.class) {
			protected Wizard createWizard() {
				String title= RefactoringMessages.getString("RefactoringGroup.rename_method_title"); //$NON-NLS-1$
				String message= RefactoringMessages.getString("RefactoringGroup.rename_method_message"); //$NON-NLS-1$
				RenameRefactoringWizard w= new RenameRefactoringWizard(title, message, IJavaHelpContextIds.RENAME_METHOD_WIZARD_PAGE, IJavaHelpContextIds.RENAME_METHOD_ERROR_WIZARD_PAGE);
				w.setInputPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_METHOD);
				return w;
			}
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new RenameStaticMethodRefactoring(changeCreator, (IMethod)obj);
			}
		};
	}
		
	// -------------------- type refactorings ----------------------
	
	private AbstractOpenRefactoringWizardAction createRenameTypeAction(final ITextBufferChangeCreator changeCreator) {	
		String label= RefactoringMessages.getString("RefactoringGroup.rename"); //$NON-NLS-1$
		return new AbstractOpenRefactoringWizardAction(label, IType.class) {
			protected Wizard createWizard() {
				String title= RefactoringMessages.getString("RefactoringGroup.rename_type_title"); //$NON-NLS-1$
				String message= RefactoringMessages.getString("RefactoringGroup.rename_type_message"); //$NON-NLS-1$
				RenameRefactoringWizard w= new RenameRefactoringWizard(title, message, IJavaHelpContextIds.RENAME_TYPE_WIZARD_PAGE, IJavaHelpContextIds.RENAME_TYPE_ERROR_WIZARD_PAGE); 
				w.setInputPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_TYPE);
				return w;
			}
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new RenameTypeRefactoring(changeCreator, (IType)obj);
			}
		};
	}
		
	// -------------------- field refactorings ----------------------
	
	private AbstractOpenRefactoringWizardAction createRenameFieldAction(final ITextBufferChangeCreator changeCreator) {	
		String label= RefactoringMessages.getString("RefactoringGroup.rename"); //$NON-NLS-1$
		return new AbstractOpenRefactoringWizardAction(label, IField.class) {
			protected Wizard createWizard() {
				String title= RefactoringMessages.getString("RefactoringGroup.rename_field_title"); //$NON-NLS-1$
				String message= RefactoringMessages.getString("RefactoringGroup.rename_field_message"); //$NON-NLS-1$
				RenameRefactoringWizard w= new RenameRefactoringWizard(title, message, IJavaHelpContextIds.RENAME_FIELD_WIZARD_PAGE, IJavaHelpContextIds.RENAME_FIELD_ERROR_WIZARD_PAGE); 
				//XXX: missing icon for field
				w.setInputPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_CU);
				return w;
			}
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new RenameFieldRefactoring(changeCreator, (IField)obj);
			}
		};
	}
			
	// --------- compilation unit refactorings  --------------------
	
	private AbstractOpenRefactoringWizardAction createMoveCompilationUnitAction(final ITextBufferChangeCreator changeCreator) {
		String label= RefactoringMessages.getString("RefactoringGroup.move_to_another_package"); //$NON-NLS-1$
		return new AbstractOpenRefactoringWizardAction(label, ICompilationUnit.class) {
			protected Wizard createWizard() { 
				return new MoveCompilationUnitWizard(); 
			}
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new MoveCompilationUnitRefactoring(changeCreator, (ICompilationUnit)obj);
			}
		};
	}
}