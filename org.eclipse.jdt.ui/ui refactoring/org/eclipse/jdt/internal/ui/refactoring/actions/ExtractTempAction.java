package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.ExtractTempRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.ExtractTempWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;

public class ExtractTempAction extends TextSelectionRefactoringAction {

	public ExtractTempAction(CompilationUnitEditor editor) {
		super(editor, RefactoringMessages.getString("ExtractTempAction.extract_temp")); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * Method declated in TextSelectionBasedRefactoringAction
	 */	
	protected Refactoring createRefactoring(ICompilationUnit cunit, ITextSelection selection) {
		return new ExtractTempRefactoring(cunit, selection.getOffset(), selection.getLength(), 
																 JavaPreferencesSettings.getCodeGenerationSettings(),
																 CodeFormatterPreferencePage.getTabSize(),
																 CodeFormatterPreferencePage.isCompactingAssignment());
	}

	/* (non-Javadoc)
	 * Method declated in TextSelectionBasedRefactoringAction
	 */	
	protected RefactoringWizard createWizard(Refactoring refactoring) {
		//XXX wrong help
		String helpId= IJavaHelpContextIds.RENAME_TEMP_ERROR_WIZARD_PAGE;
		String pageTitle= RefactoringMessages.getString("ExtractTempAction.extract_temp"); //$NON-NLS-1$
		return new ExtractTempWizard((ExtractTempRefactoring)refactoring, pageTitle, helpId);
	}
}
