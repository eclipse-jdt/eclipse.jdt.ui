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
import org.eclipse.jdt.core.refactoring.Refactoring;
import org.eclipse.jdt.core.refactoring.cus.MoveCompilationUnitRefactoring;
import org.eclipse.jdt.core.refactoring.cus.RenameCompilationUnitRefactoring;import org.eclipse.jdt.core.refactoring.fields.RenameFieldRefactoring;
import org.eclipse.jdt.core.refactoring.methods.RenameMethodInInterfaceRefactoring;
import org.eclipse.jdt.core.refactoring.methods.RenameParametersRefactoring;import org.eclipse.jdt.core.refactoring.methods.RenamePrivateMethodRefactoring;
import org.eclipse.jdt.core.refactoring.methods.RenameStaticMethodRefactoring;
import org.eclipse.jdt.core.refactoring.methods.RenameVirtualMethodRefactoring;
import org.eclipse.jdt.core.refactoring.packages.RenamePackageRefactoring;
import org.eclipse.jdt.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.core.refactoring.types.RenameTypeRefactoring;

import org.eclipse.jdt.internal.ui.IPreferencesConstants;import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.AbstractOpenWizardAction;
import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.actions.GroupContext;
import org.eclipse.jdt.internal.ui.refactoring.AbstractOpenRefactoringWizardAction;
import org.eclipse.jdt.internal.ui.refactoring.MoveCompilationUnitWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringResources;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RenameParametersWizard;import org.eclipse.jdt.internal.ui.refactoring.RenameRefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.changes.DocumentTextBufferChangeCreator;
import org.eclipse.jdt.internal.ui.refactoring.undo.RedoRefactoringAction;
import org.eclipse.jdt.internal.ui.refactoring.undo.UndoRefactoringAction;

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
		
		createActions(selectionProvider);
		
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
	
	private void createActions(ISelectionProvider provider) {
		if (fIntitialized)
			return;
			
		fUndoRefactoring= new UndoRefactoringAction();
		fRedoRefactoring= new RedoRefactoringAction();		
		
		IDocumentProvider documentProvider= JavaPlugin.getDefault().getCompilationUnitDocumentProvider();
		ITextBufferChangeCreator changeCreator= new DocumentTextBufferChangeCreator(documentProvider);
		
		openWizardActions= new AbstractOpenRefactoringWizardAction[]{
			createRenameParametersAction(provider, changeCreator),
			createRenameMethodInClassAction(provider, changeCreator),
			createRenamePrivateMethodAction(provider, changeCreator),
			createRenameStaticMethodAction(provider, changeCreator),
			createRenameMethodInInterfaceAction(provider, changeCreator),
			createRenameTypeAction(provider, changeCreator),
			createRenamePackageAction(provider, changeCreator),
			createRenameFieldAction(provider, changeCreator),
			createMoveCompilationUnitAction(provider, changeCreator),
			createRenameCUAction(provider, changeCreator)
		};
		fIntitialized= true;
	}
	
	// -------------------- method refactorings ----------------------
	
	private AbstractOpenRefactoringWizardAction createRenameParametersAction(ISelectionProvider provider, final ITextBufferChangeCreator changeCreator) {
		String label= RefactoringResources.getResourceString("Refactoring.RenameParameters.label");
		return new AbstractOpenRefactoringWizardAction(provider, label, IMethod.class) {
			protected Wizard createWizard() {
				return new RenameParametersWizard("Rename Method Parameters");
			}
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new RenameParametersRefactoring(changeCreator, (IMethod)obj);
			}
		};
	}
	
	private AbstractOpenRefactoringWizardAction createRenameMethodInClassAction(ISelectionProvider provider, final ITextBufferChangeCreator changeCreator) {
		String label= RefactoringResources.getResourceString("Refactoring.RenameMethod.label");
		return new AbstractOpenRefactoringWizardAction(provider, label, IMethod.class) {
			protected Wizard createWizard() {
				return new RenameRefactoringWizard("Refactoring.RenameMethod");
			}
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new RenameVirtualMethodRefactoring(changeCreator, (IMethod)obj);
			}
		};
	}

	private AbstractOpenRefactoringWizardAction createRenameMethodInInterfaceAction(ISelectionProvider provider, final ITextBufferChangeCreator changeCreator) {
		String label= RefactoringResources.getResourceString("Refactoring.RenameMethod.label");
		return new AbstractOpenRefactoringWizardAction(provider, label, IMethod.class) {
			protected Wizard createWizard() {
				return new RenameRefactoringWizard("Refactoring.RenameMethod");
			}
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new RenameMethodInInterfaceRefactoring(changeCreator, (IMethod)obj);
			}
		};
	}
	
	private AbstractOpenRefactoringWizardAction createRenamePrivateMethodAction(ISelectionProvider provider, final ITextBufferChangeCreator changeCreator) {
		String label= RefactoringResources.getResourceString("Refactoring.RenameMethod.label");
		return new AbstractOpenRefactoringWizardAction(provider, label, IMethod.class) {
			protected Wizard createWizard() {
				return new RenameRefactoringWizard("Refactoring.RenameMethod");
			}
			
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new RenamePrivateMethodRefactoring(changeCreator, (IMethod)obj);
			}
		};
	}
	
	private AbstractOpenRefactoringWizardAction createRenameStaticMethodAction(ISelectionProvider provider, final ITextBufferChangeCreator changeCreator) {
		String label= RefactoringResources.getResourceString("Refactoring.RenameMethod.label");
		return new AbstractOpenRefactoringWizardAction(provider, label, IMethod.class) {
			protected Wizard createWizard() {
				return new RenameRefactoringWizard("Refactoring.RenameMethod");
			}
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new RenameStaticMethodRefactoring(changeCreator, (IMethod)obj);
			}
		};
	}
		
	// -------------------- type refactorings ----------------------
	
	private AbstractOpenRefactoringWizardAction createRenameTypeAction(ISelectionProvider provider, final ITextBufferChangeCreator changeCreator) {	
		String label= RefactoringResources.getResourceString("Refactoring.RenameType.label");
		return new AbstractOpenRefactoringWizardAction(provider, label, IType.class) {
			protected Wizard createWizard() { 
				return new RenameRefactoringWizard("Refactoring.RenameType"); 
			}
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new RenameTypeRefactoring(changeCreator, (IType)obj);
			}
		};
	}
	
	// -------------------- package refactorings ----------------------
	
	private AbstractOpenRefactoringWizardAction createRenamePackageAction(ISelectionProvider provider, final ITextBufferChangeCreator changeCreator) {	
		if (linkRenameInPackagesView())
			return null;
			
		String label= RefactoringResources.getResourceString("Refactoring.RenamePackage.label");
		return new AbstractOpenRefactoringWizardAction(provider, label, IPackageFragment.class) {
			protected Wizard createWizard() { 
				return new RenameRefactoringWizard("Refactoring.RenamePackage"); 
			}
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new RenamePackageRefactoring(changeCreator, (IPackageFragment)obj);
			}
		};
	}
	
	// -------------------- field refactorings ----------------------
	
	private AbstractOpenRefactoringWizardAction createRenameFieldAction(ISelectionProvider provider, final ITextBufferChangeCreator changeCreator) {	
		String label= RefactoringResources.getResourceString("Refactoring.RenameField.label");
		return new AbstractOpenRefactoringWizardAction(provider, label, IField.class) {
			protected Wizard createWizard() { 
				return new RenameRefactoringWizard("Refactoring.RenameField"); 
			}
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new RenameFieldRefactoring(changeCreator, (IField)obj);
			}
		};
	}
			
	// --------- compilation unit refactorings  --------------------
	
	private AbstractOpenRefactoringWizardAction createMoveCompilationUnitAction(ISelectionProvider provider, final ITextBufferChangeCreator changeCreator) {
		String label= RefactoringResources.getResourceString("Refactoring.MoveCompilationUnit.label");
		return new AbstractOpenRefactoringWizardAction(provider, label, ICompilationUnit.class) {
			protected Wizard createWizard() { 
				return new MoveCompilationUnitWizard(); 
			}
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new MoveCompilationUnitRefactoring(changeCreator, (ICompilationUnit)obj);
			}
		};
	}
	
	private AbstractOpenRefactoringWizardAction createRenameCUAction(ISelectionProvider provider, final ITextBufferChangeCreator changeCreator) {	
		if (linkRenameInPackagesView())
			return null;
			
		String label= RefactoringResources.getResourceString("Refactoring.RenameCompilationUnit.label");
		return new AbstractOpenRefactoringWizardAction(provider, label, ICompilationUnit.class) {
			protected Wizard createWizard() { 
				return new RenameRefactoringWizard("Refactoring.RenameCompilationUnit"); 
			}
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new RenameCompilationUnitRefactoring(changeCreator, (ICompilationUnit)obj);
			}
		};
	}
	
	//---- Helpers --------------------------------------------------
	private boolean linkRenameInPackagesView() {
		return JavaPlugin.getDefault().getPreferenceStore().getBoolean(
			IPreferencesConstants.LINK_RENAME_IN_PACKAGES_TO_REFACTORING);
	}
}