package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.ExtractTempRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.ExtractTempWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;

public class ExtractTempAction extends TextSelectionBasedRefactoringAction {

	public ExtractTempAction() {
		super("Extract Local Variable", "Extract Local Variable", "This action is unavailable on the current text selection. Select an expression.");
	}
	
	public ExtractTempAction(JavaEditor editor) {
		this();
		setEditor(editor);
	}

	protected Refactoring createRefactoring(ICompilationUnit cunit, ITextSelection selection) {
		return new ExtractTempRefactoring(cunit, selection.getOffset(), selection.getLength(), 
																 JavaPreferencesSettings.getCodeGenerationSettings(),
																 CodeFormatterPreferencePage.getTabSize(),
																 CodeFormatterPreferencePage.isCompactingAssignment());
	}

	protected RefactoringWizard createWizard(Refactoring refactoring) {
		//XXX wrong help
		String helpId= IJavaHelpContextIds.RENAME_TEMP_ERROR_WIZARD_PAGE;
		String pageTitle= "Extract Local Variable";
		return new ExtractTempWizard((ExtractTempRefactoring)refactoring, pageTitle, helpId);
	}

}
