package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineTempRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;

public class InlineTempAction extends TextSelectionBasedRefactoringAction {

	public InlineTempAction() {
		super("Inline Local Variable", "Inline Local Variable", "This action is unavailable on the current text selection. Select a local variable declaration or reference.");
	}
	
	public InlineTempAction(JavaEditor editor) {
		this();
		setEditor(editor);
	}

	/*
	 * @see TextSelectionBasedRefactoringAction#createRefactoring(ICompilationUnit, ITextSelection)
	 */
	Refactoring createRefactoring(ICompilationUnit cunit, ITextSelection selection) {
		return new InlineTempRefactoring(cunit, selection.getOffset(), selection.getLength());
	}

	/*
	 * @see TextSelectionBasedRefactoringAction#createWizard(Refactoring)
	 */
	RefactoringWizard createWizard(Refactoring refactoring) {
		//XXX wrong help
		String helpId= IJavaHelpContextIds.RENAME_TEMP_ERROR_WIZARD_PAGE;
		String pageTitle= "Inline Local Variable";
		return new RefactoringWizard((InlineTempRefactoring)refactoring, pageTitle, helpId);
	}
}

