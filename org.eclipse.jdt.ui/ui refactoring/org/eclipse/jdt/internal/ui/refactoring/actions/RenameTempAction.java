/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.internal.core.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.core.refactoring.rename.RenameTempRefactoring;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RenameRefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RenameRefactoringWizard2;
import org.eclipse.jface.text.ITextSelection;

/**
 * Extracts a new method from the text editor's text selection by using the
 * extract method refactoing.
 */
public class RenameTempAction extends TextSelectionBasedRefactoringAction{

	/**
	 * Creates a new extract method action when used as an action delegate.
	 */
	public RenameTempAction() {
		super("Rename Local Variable");
	}
	
	/**
	 * Creates a new extract method action for the given text editor. The text
	 * editor's selection marks the set of statements to be extracted into a new
	 * method.
	 * @param editor the text editor.
	 */
	public RenameTempAction(JavaEditor editor) {
		this();
		setEditor(editor);
	}

	/*
	 * @see TextSelectionBasedRefactoringAction#createRefactoring
	 */	
	Refactoring createRefactoring(ICompilationUnit cunit, ITextSelection selection, ITextBufferChangeCreator changeCreator) {
		return new RenameTempRefactoring(cunit, selection.getOffset(), selection.getLength());
	}
	
	/*
	 * @see TextSelectionBasedRefactoringAction#getDialogTitle()
	 */
	protected String getDialogTitle() {
		return "Rename Local Variable";
	}

	/*
	 * @see TextSelectionBasedRefactoringAction#createWizard(Refactoring)
	 */
	RefactoringWizard createWizard(Refactoring refactoring) {
		String message= "Choose a new name for the local variable.";
		String wizardPageHelp= IJavaHelpContextIds.RENAME_TEMP_WIZARD_PAGE; 
		String errorPageHelp= IJavaHelpContextIds.RENAME_TEMP_ERROR_WIZARD_PAGE;
		return new RenameRefactoringWizard2((RenameTempRefactoring)refactoring, getDialogTitle(), message, wizardPageHelp, errorPageHelp);
	}
}

