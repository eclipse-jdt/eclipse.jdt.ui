package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.ModifyParametersRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.refactoring.ModifyParametersWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;

public class ModifyParametersAction extends OpenRefactoringWizardAction {
	
	private static final String TITLE= RefactoringMessages.getString("RefactoringGroup.modify_Parameters_label"); //$NON-NLS-1$
	private static final String UNAVAILABLE= RefactoringMessages.getString("ModifyParametersAction.unavailable"); //$NON-NLS-1$
	
	public ModifyParametersAction(CompilationUnitEditor editor) {
		super(TITLE, UNAVAILABLE, editor, IMethod.class, false);
	}

	public ModifyParametersAction(IWorkbenchSite site) {
		super(TITLE, UNAVAILABLE, site, IMethod.class, false);
	}

	protected Refactoring createNewRefactoringInstance(Object obj){
		return new ModifyParametersRefactoring((IMethod)obj);
	}
	protected boolean canActivateRefactoring(Refactoring refactoring)  throws JavaModelException{
		return ((ModifyParametersRefactoring)refactoring).checkPreactivation().isOK();
	}
	protected RefactoringWizard createWizard(Refactoring ref){
		String title= RefactoringMessages.getString("RefactoringGroup.modify_method_parameters"); //$NON-NLS-1$
		//FIX ME: wrong
		String helpId= IJavaHelpContextIds.RENAME_PARAMS_ERROR_WIZARD_PAGE;
		return new ModifyParametersWizard((ModifyParametersRefactoring)ref, title, helpId);
	}

}
