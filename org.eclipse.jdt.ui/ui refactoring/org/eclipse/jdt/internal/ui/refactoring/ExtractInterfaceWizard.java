package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractInterfaceRefactoring;

public class ExtractInterfaceWizard extends RefactoringWizard {
	
	private static final String EXTRACT_INTERFACE_ERROR_PAGE_HELP_ID= "errorPageContextHelpId"; //XXX
	public ExtractInterfaceWizard(ExtractInterfaceRefactoring ref) {
		super(ref, "Extract Interface", EXTRACT_INTERFACE_ERROR_PAGE_HELP_ID);
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new ExtractInterfaceInputPage());
	}
}
