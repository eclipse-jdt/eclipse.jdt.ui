package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineTempRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;

public class InlineTempAction extends TextSelectionRefactoringAction {

	public InlineTempAction(CompilationUnitEditor editor) {
		super(editor, RefactoringMessages.getString("InlineTempAction.inline_temp")); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * Method declated in TextSelectionBasedRefactoringAction
	 */	
	protected Refactoring createRefactoring(ICompilationUnit cunit, ITextSelection selection) {
		return new InlineTempRefactoring(cunit, selection.getOffset(), selection.getLength());
	}

	/* (non-Javadoc)
	 * Method declated in TextSelectionBasedRefactoringAction
	 */	
	protected RefactoringWizard createWizard(Refactoring refactoring) {
		//XXX wrong help
		String helpId= IJavaHelpContextIds.RENAME_TEMP_ERROR_WIZARD_PAGE;
		String pageTitle= RefactoringMessages.getString("InlineTempAction.inline_temp"); //$NON-NLS-1$
		RefactoringWizard result= new RefactoringWizard((InlineTempRefactoring)refactoring, pageTitle, helpId);
		result.setExpandFirstNode(true);
		return result;
	}
	
	protected void selectionChanged(ITextSelection selection) {
		setEnabled(getCompilationUnit() != null);
	}
	
	private ICompilationUnit getCompilationUnit(){
		return SelectionConverter.getInputAsCompilationUnit(getEditor());
	}	
}
