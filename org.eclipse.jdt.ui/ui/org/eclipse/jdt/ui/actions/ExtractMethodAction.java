package org.eclipse.jdt.ui.actions;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;


import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.ExtractMethodRefactoring;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.refactoring.code.ExtractMethodWizard;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class ExtractMethodAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;
	private String fDialogMessageTitle;

	public ExtractMethodAction(CompilationUnitEditor editor) {
		super(editor.getEditorSite());
		fEditor= editor;
		fDialogMessageTitle= RefactoringMessages.getString("ExtractMethodAction.dialog.title");//$NON-NLS-1$
	}

	private Refactoring createRefactoring(ICompilationUnit cunit, ITextSelection selection) {
		return new ExtractMethodRefactoring(
			cunit, 
			selection.getOffset(), selection.getLength(),
			JavaPreferencesSettings.getCodeGenerationSettings());
	}

	private RefactoringWizard createWizard(Refactoring refactoring) {
		return new ExtractMethodWizard((ExtractMethodRefactoring)refactoring);
	}
	
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
	
}
