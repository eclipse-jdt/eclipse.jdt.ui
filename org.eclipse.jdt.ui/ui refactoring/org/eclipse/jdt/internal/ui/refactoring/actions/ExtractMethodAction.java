/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.ExtractMethodRefactoring;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.code.ExtractMethodWizard;

/**
 * Extracts a new method from the text editor's text selection by using the
 * extract method refactoing.
 */
public class ExtractMethodAction extends TextSelectionBasedRefactoringAction {

	/**
	 * Creates a new extract method action when used as an action delegate.
	 */
	public ExtractMethodAction() {
		super(RefactoringMessages.getString("ExtractMethodAction.extract_method")); //$NON-NLS-1$
	}
	
	/**
	 * Creates a new extract method action for the given text editor. The text
	 * editor's selection marks the set of statements to be extracted into a new
	 * method.
	 * @param editor the text editor.
	 */
	public ExtractMethodAction(JavaEditor editor) {
		this();
		setEditor(editor);
	}

	/*
	 * @see TextSelectionBasedRefactoringAction#createRefactoring
	 */	
	Refactoring createRefactoring(ICompilationUnit cunit, ITextSelection selection) {
		return new ExtractMethodRefactoring(
			cunit, 
			selection.getOffset(), selection.getLength(),
			CodeFormatterPreferencePage.isCompactingAssignment(),
			CodeFormatterPreferencePage.getTabSize(),
			JavaPreferencesSettings.getCodeGenerationSettings());
	}

	/*
	 * @see TextSelectionBasedRefactoringAction#getDialogTitle()
	 */
	protected String getDialogTitle() {
		return "Extract Method";
	}

	/*
	 * @see TextSelectionBasedRefactoringAction#createWizard(Refactoring)
	 */
	RefactoringWizard createWizard(Refactoring refactoring) {
		return new ExtractMethodWizard((ExtractMethodRefactoring)refactoring);
	}

}