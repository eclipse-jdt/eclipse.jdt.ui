package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineTempRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;

public class InlineTempAction extends TextSelectionBasedRefactoringAction {

	public InlineTempAction() {
		super("Inline Local Variable");
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
		String pageTitle= "Inline Local Variable";
		RefactoringWizard result= new RefactoringWizard((InlineTempRefactoring)refactoring, pageTitle, helpId);
		result.setExpandFirstNode(true);
		return result;
	}
	
	/* (non-Javadoc)
	 * Method declated in TextSelectionBasedRefactoringAction
	 */	
	protected String getMessageDialogTitle() {
		return "Inline Local Variable";
	}
	
	/* (non-Javadoc)
	 * @see TextSelectionAction#canOperateOnCurrentSelection(ISelection)
	 */
	protected boolean canOperateOnCurrentSelection(ISelection selection) {
		if (!(selection instanceof ITextSelection))
			return false;
		return true;
	}
	
}

