package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractInterfaceRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

public class ExtractInterfaceWizard extends RefactoringWizard {
	
	public ExtractInterfaceWizard(ExtractInterfaceRefactoring ref) {
		super(ref, "Extract Interface", IJavaHelpContextIds.EXTRACT_INTERFACE_ERROR_WIZARD_PAGE);
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new ExtractInterfaceInputPage());
	}
}
