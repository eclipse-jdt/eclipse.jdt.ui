package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineTempRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.refactoring.InlineTempWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jface.text.ITextSelection;

public class InlineTempAction extends TextSelectionBasedRefactoringAction {

	public InlineTempAction() {
		super("Inline Local Variable");
	}
	
	public InlineTempAction(JavaEditor editor) {
		this();
		setEditor(editor);
	}

	/*
	 * @see TextSelectionBasedRefactoringAction#createRefactoring(ICompilationUnit, ITextSelection, ITextBufferChangeCreator)
	 */
	Refactoring createRefactoring(ICompilationUnit cunit, ITextSelection selection, ITextBufferChangeCreator changeCreator) {
		return new InlineTempRefactoring(cunit, selection.getOffset(), selection.getLength());
	}

	/*
	 * @see TextSelectionBasedRefactoringAction#createWizard(Refactoring)
	 */
	RefactoringWizard createWizard(Refactoring refactoring) {
		//XXX wrong help
		String helpId= IJavaHelpContextIds.RENAME_TEMP_ERROR_WIZARD_PAGE;
		String pageTitle= "Inline Local Variable";
		return new InlineTempWizard((InlineTempRefactoring)refactoring, pageTitle, helpId);
	}

	/*
	 * @see TextSelectionBasedRefactoringAction#getDialogTitle()
	 */
	protected String getDialogTitle() {
		return "Inline Local Variable";
	}
}

