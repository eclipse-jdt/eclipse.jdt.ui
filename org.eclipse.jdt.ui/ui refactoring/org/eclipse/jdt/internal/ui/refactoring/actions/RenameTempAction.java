package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTempRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RenameRefactoringWizard;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

class RenameTempAction extends TextSelectionRefactoringAction {

	public RenameTempAction(CompilationUnitEditor editor) {
		super(editor, RefactoringMessages.getString("RenameTempAction.rename_Local_Variable"));//$NON-NLS-1$
		setText(RefactoringMessages.getString("RenameTempAction.rename_Local_Variable"));//$NON-NLS-1$
	}
	
	protected Refactoring createRefactoring(ICompilationUnit cunit, ITextSelection selection) {
		return new RenameTempRefactoring(cunit, selection.getOffset(), selection.getLength());
	}
	
	protected RefactoringWizard createWizard(Refactoring refactoring) {
		String message= RefactoringMessages.getString("RenameTempAction.choose_new_name"); //$NON-NLS-1$
		String wizardPageHelp= IJavaHelpContextIds.RENAME_TEMP_WIZARD_PAGE; 
		String errorPageHelp= IJavaHelpContextIds.RENAME_TEMP_ERROR_WIZARD_PAGE;
		return new RenameRefactoringWizard((RenameTempRefactoring)refactoring, getText(), message, wizardPageHelp, errorPageHelp);
	}
	
	protected void selectionChanged(ITextSelection selection) {
		setEnabled(getCompilationUnit() != null);
	}
	
	protected boolean canRun(ITextSelection selection) {
		selectionChanged(selection);
		if (! isEnabled())
			return false;

		Refactoring renameTempRefactoring= createRefactoring(getCompilationUnit(), (ITextSelection)selection);
		try {
			return (renameTempRefactoring.checkActivation(new NullProgressMonitor()).isOK());
		} catch(JavaModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.getString("RenameTempAction.rename_Local_Variable"), RefactoringMessages.getString("RenameTempAction.exception"));//$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}
	}
		
	private ICompilationUnit getCompilationUnit(){
		return SelectionConverter.getInputAsCompilationUnit(getEditor());
	}	
}
