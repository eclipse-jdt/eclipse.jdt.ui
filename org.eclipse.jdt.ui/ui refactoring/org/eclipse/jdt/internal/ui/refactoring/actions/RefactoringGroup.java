/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.texteditor.IDocumentProvider;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.refactoring.Refactoring;
import org.eclipse.jdt.core.refactoring.cus.MoveCompilationUnitRefactoring;
import org.eclipse.jdt.core.refactoring.fields.RenameNonPrivateFieldRefactoring;
import org.eclipse.jdt.core.refactoring.fields.RenamePrivateFieldRefactoring;
import org.eclipse.jdt.core.refactoring.fields.SafeDeletePrivateFieldRefactoring;
import org.eclipse.jdt.core.refactoring.methods.RenameMethodInInterfaceRefactoring;
import org.eclipse.jdt.core.refactoring.methods.RenamePrivateMethodRefactoring;
import org.eclipse.jdt.core.refactoring.methods.RenameStaticMethodRefactoring;
import org.eclipse.jdt.core.refactoring.methods.RenameVirtualMethodRefactoring;
import org.eclipse.jdt.core.refactoring.methods.SafeDeleteMethodRefactoring;
import org.eclipse.jdt.core.refactoring.packages.RenamePackageRefactoring;
import org.eclipse.jdt.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.core.refactoring.types.ExtractInterfaceRefactoring;
import org.eclipse.jdt.core.refactoring.types.RenameTypeRefactoring;
import org.eclipse.jdt.core.refactoring.types.SafeDeleteTypeRefactoring;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.AbstractOpenWizardAction;
import org.eclipse.jdt.internal.ui.actions.ContextMenuGroup;
import org.eclipse.jdt.internal.ui.actions.GroupContext;
import org.eclipse.jdt.internal.ui.refactoring.AbstractOpenRefactoringWizardAction;
import org.eclipse.jdt.internal.ui.refactoring.ExtractInterfaceWizard;
import org.eclipse.jdt.internal.ui.refactoring.MoveCompilationUnitWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringResources;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RenameRefactoringWizard;
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
	
	public void fill(IMenuManager manager, GroupContext context) {
	
		createActions(context.getSelectionProvider());
		
		for (int i= 0; i < openWizardActions.length; i++)
			if (openWizardActions[i].canActionBeAdded())
				manager.add(openWizardActions[i]);	

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
			createRenameMethodInClassAction(provider, changeCreator),
			createRenamePrivateMethodAction(provider, changeCreator),
			createRenameStaticMethodAction(provider, changeCreator),
			createRenameMethodInInterfaceAction(provider, changeCreator),
			createRenameTypeAction(provider, changeCreator),
			createRenamePackageAction(provider, changeCreator),
			createRenamePrivateFieldAction(provider, changeCreator),
			createRenameNonPrivateFieldAction(provider, changeCreator),
			createMoveCompilationUnitAction(provider, changeCreator),
			
			//not supported now
			//createSafeDeleteTypeAction(provider),
			//createSafeDeleteMethodAction(provider),
			//createSafeDeletePrivateFieldAction(provider),
			//createExtractInterfaceAction(provider)
		};
		fIntitialized= true;
	}
	
	// -------------------- method refactorings ----------------------
	private AbstractOpenRefactoringWizardAction createRenameMethodInClassAction(ISelectionProvider provider, final ITextBufferChangeCreator changeCreator) {
		String label= RefactoringResources.getResourceString("Refactoring.RenameMethod.label");
		return new AbstractOpenRefactoringWizardAction(provider, label, IMethod.class) {
			protected Wizard createWizard() {
				return new RenameRefactoringWizard("Rename Method");
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
				return new RenameRefactoringWizard("Rename Method");
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
				return new RenameRefactoringWizard("Rename Method");
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
				return new RenameRefactoringWizard("Rename Method");
			}
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new RenameStaticMethodRefactoring(changeCreator, (IMethod)obj);
			}
		};
	}
	
	private AbstractOpenRefactoringWizardAction createSafeDeleteMethodAction(ISelectionProvider provider, final ITextBufferChangeCreator changeCreator) {	
		String label= RefactoringResources.getResourceString("Refactoring.SafeDelete.label");
		return new AbstractOpenRefactoringWizardAction(provider, label, IMethod.class) {
			protected Wizard createWizard() { 
				return new RefactoringWizard("Refactoring.SafeDelete");
			}
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new SafeDeleteMethodRefactoring(changeCreator, (IMethod)obj);
			}
		};
	}	
	
	// -------------------- type refactorings ----------------------
	
	private AbstractOpenRefactoringWizardAction createRenameTypeAction(ISelectionProvider provider, final ITextBufferChangeCreator changeCreator) {	
		String label= RefactoringResources.getResourceString("Refactoring.RenameType.label");
		return new AbstractOpenRefactoringWizardAction(provider, label, IType.class) {
			protected Wizard createWizard() { 
				return new RenameRefactoringWizard("Rename Type"); 
			}
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new RenameTypeRefactoring(changeCreator, (IType)obj);
			}
		};
	}
		
	private AbstractOpenRefactoringWizardAction createSafeDeleteTypeAction(ISelectionProvider provider, final ITextBufferChangeCreator changeCreator) {	
		String label= RefactoringResources.getResourceString("Refactoring.SafeDelete.label");
		return new AbstractOpenRefactoringWizardAction(provider, label, IType.class) {
			protected Wizard createWizard() { 
				return new RefactoringWizard("Refactoring.SafeDelete"); 
			}
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new SafeDeleteTypeRefactoring(changeCreator, (IType)obj);
			}
		};
	}
	
	private AbstractOpenRefactoringWizardAction createExtractInterfaceAction(ISelectionProvider provider) {	
		String label= RefactoringResources.getResourceString("Refactoring.ExtractInterface.label");
		return new AbstractOpenRefactoringWizardAction(provider, label, IType.class) {
			protected Wizard createWizard() { 
				return new ExtractInterfaceWizard(); 
			}
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new ExtractInterfaceRefactoring((IType)obj);
			}
		};
	}
	
	// -------------------- package refactorings ----------------------
	
	private AbstractOpenRefactoringWizardAction createRenamePackageAction(ISelectionProvider provider, final ITextBufferChangeCreator changeCreator) {	
		String label= RefactoringResources.getResourceString("Refactoring.RenamePackage.label");
		return new AbstractOpenRefactoringWizardAction(provider, label, IPackageFragment.class) {
			protected Wizard createWizard() { 
				return new RenameRefactoringWizard("Rename Package"); 
			}
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new RenamePackageRefactoring(changeCreator, (IPackageFragment)obj);
			}
		};
	}
	
	// -------------------- field refactorings ----------------------
	
	private AbstractOpenRefactoringWizardAction createRenamePrivateFieldAction(ISelectionProvider provider, final ITextBufferChangeCreator changeCreator) {	
		String label= RefactoringResources.getResourceString("Refactoring.RenameField.label");
		return new AbstractOpenRefactoringWizardAction(provider, label, IField.class) {
			protected Wizard createWizard() { 
				return new RenameRefactoringWizard("Rename Field"); 
			}
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new RenamePrivateFieldRefactoring(changeCreator, (IField)obj);
			}
		};
	}
	
	private AbstractOpenRefactoringWizardAction createRenameNonPrivateFieldAction(ISelectionProvider provider, final ITextBufferChangeCreator changeCreator) {	
		String label= RefactoringResources.getResourceString("Refactoring.RenameField.label");
		return new AbstractOpenRefactoringWizardAction(provider, label, IField.class) {
			protected Wizard createWizard() { 
				return new RenameRefactoringWizard("Rename Field"); 
			}
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new RenameNonPrivateFieldRefactoring(changeCreator, (IField)obj);
			}
		};
	}
	
	private AbstractOpenRefactoringWizardAction createSafeDeletePrivateFieldAction(ISelectionProvider provider, final ITextBufferChangeCreator changeCreator) {	
		String label= RefactoringResources.getResourceString("Refactoring.SafeDelete.label");
		return new AbstractOpenRefactoringWizardAction(provider, label, IField.class) {
			protected Wizard createWizard() { 
				return new RefactoringWizard("Refactoring.SafeDelete"); 
			}
			protected Refactoring createNewRefactoringInstance(Object obj){
				return new SafeDeletePrivateFieldRefactoring(changeCreator, (IField)obj);
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
}