package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jdt.ui.actions.UnifiedSite;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public abstract class TextSelectionRefactoringAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;
	
	protected TextSelectionRefactoringAction(CompilationUnitEditor editor) {
		super(UnifiedSite.create(editor.getEditorSite()));
		fEditor= editor;
	}

	protected abstract Refactoring createRefactoring(ICompilationUnit cunit, ITextSelection selection);
	protected abstract RefactoringWizard createWizard(Refactoring refactoring);
	protected abstract String getMessageDialogTitle();

	public final void run(ITextSelection selection) {
		if (! canRun(selection)) {
			MessageDialog.openInformation(getShell(), getMessageDialogTitle(), RefactoringMessages.getString("NewTextRefactoringAction.not_available"));  //$NON-NLS-1$
			return;
		}
			
		try{
			Refactoring refactoring= createRefactoring(getCompilationUnit(), selection);
			new RefactoringStarter().activate(refactoring, createWizard(refactoring), getMessageDialogTitle(), false);
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, getMessageDialogTitle(), RefactoringMessages.getString("NewTextRefactoringAction.exception")); //$NON-NLS-1$
		}	
	}

	//override to add checks
	protected boolean canRun(ITextSelection selection){
		return canEnableOn(selection);
	}
	
	protected final ICompilationUnit getCompilationUnit() {
		Object editorInput= SelectionConverter.getInput(fEditor);
		if (editorInput instanceof ICompilationUnit)
			return (ICompilationUnit)editorInput;
		else
			return null;
	}

	protected final void selectionChanged(ITextSelection selection) {
		setEnabled(canEnableOn(selection));
	}

	protected boolean canEnableOn(ITextSelection selection) {
		return selection.getLength() > 0;
	}
}
