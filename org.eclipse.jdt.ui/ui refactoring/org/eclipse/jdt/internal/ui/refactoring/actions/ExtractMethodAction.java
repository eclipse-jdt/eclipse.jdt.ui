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
		super(RefactoringMessages.getString("ExtractMethodAction.extract_method"));  //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * Method declated in TextSelectionBasedRefactoringAction
	 */	
	protected Refactoring createRefactoring(ICompilationUnit cunit, ITextSelection selection) {
		return new ExtractMethodRefactoring(
			cunit, 
			selection.getOffset(), selection.getLength(),
			CodeFormatterPreferencePage.isCompactingAssignment(),
			CodeFormatterPreferencePage.getTabSize(),
			JavaPreferencesSettings.getCodeGenerationSettings());
	}

	/* (non-Javadoc)
	 * Method declated in TextSelectionBasedRefactoringAction
	 */	
	protected RefactoringWizard createWizard(Refactoring refactoring) {
		return new ExtractMethodWizard((ExtractMethodRefactoring)refactoring);
	}

	/* (non-Javadoc)
	 * Method declated in TextSelectionBasedRefactoringAction
	 */	
	protected String getMessageDialogTitle() {
		return RefactoringMessages.getString("ExtractMethodAction.extract_Method"); //$NON-NLS-1$
	}
}