package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTempRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.refactoring.RenameRefactoringWizard;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class RenameTempAction extends SelectionDispatchAction {

	private final CompilationUnitEditor fEditor;
	private final String fDialogMessageTitle;
	
	public RenameTempAction(CompilationUnitEditor editor) {
		super(editor.getEditorSite());
		setText(RefactoringMessages.getString("RenameTempAction.rename_Local_Variable"));//$NON-NLS-1$
		fEditor= editor;
		fDialogMessageTitle= RefactoringMessages.getString("RenameTempAction.rename_Local_Variable"); //$NON-NLS-1$
		setEnabled(fEditor != null && getCompilationUnit() != null);
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
	}
	
	public boolean canRun(ITextSelection selection) {
		selectionChanged(selection);
		if (! isEnabled())
			return false;
		
		if (getCompilationUnit() == null)
			return false;	
		Refactoring renameTempRefactoring= createRefactoring(getCompilationUnit(), (ITextSelection)selection);
		try {
			return (renameTempRefactoring.checkActivation(new NullProgressMonitor()).isOK());
		} catch(JavaModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.getString("RenameTempAction.rename_Local_Variable"), RefactoringMessages.getString("RenameTempAction.exception"));//$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}
	}
	
	protected void run(ITextSelection selection) {
		try{
			ICompilationUnit input= SelectionConverter.getInputAsCompilationUnit(fEditor);
			if (!ActionUtil.isProcessable(getShell(), input))
				return;
			Refactoring refactoring= createRefactoring(input, selection);
			new RefactoringStarter().activate(refactoring, createWizard(refactoring), getShell(), fDialogMessageTitle, false);
		} catch (JavaModelException e){
			ExceptionHandler.handle(e, fDialogMessageTitle, RefactoringMessages.getString("NewTextRefactoringAction.exception")); //$NON-NLS-1$
		}	
	}
		
	private ICompilationUnit getCompilationUnit(){
		return SelectionConverter.getInputAsCompilationUnit(fEditor);
	}	
}
