package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public abstract class TextSelectionRefactoringAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;
	private String fDialogMessageTitle;
	
	protected TextSelectionRefactoringAction(CompilationUnitEditor editor, String dialogMessageTitle) {
		super(editor.getEditorSite());
		Assert.isNotNull(dialogMessageTitle);
		fEditor= editor;
		fDialogMessageTitle= dialogMessageTitle;
	}

	protected abstract Refactoring createRefactoring(ICompilationUnit cunit, ITextSelection selection);
	protected abstract RefactoringWizard createWizard(Refactoring refactoring);

	protected void run(ITextSelection selection) {
		try{
			Refactoring refactoring= createRefactoring(SelectionConverter.getInputAsCompilationUnit(fEditor), selection);
			new RefactoringStarter().activate(refactoring, createWizard(refactoring), fDialogMessageTitle, false);
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, fDialogMessageTitle, RefactoringMessages.getString("NewTextRefactoringAction.exception")); //$NON-NLS-1$
		}	
	}

	protected void selectionChanged(ITextSelection selection) {
		setEnabled(selection.getLength() > 0);
	}
	
	protected CompilationUnitEditor getEditor(){
		return fEditor;
	}
}
