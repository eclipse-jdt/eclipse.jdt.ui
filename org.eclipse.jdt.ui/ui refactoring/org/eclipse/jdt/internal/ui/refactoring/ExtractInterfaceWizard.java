package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractInterfaceRefactoring;

public class ExtractInterfaceWizard extends RefactoringWizard {

	public ExtractInterfaceWizard(ExtractInterfaceRefactoring ref) {
		//XXX
		super(ref, "Extract Interface", "errorPageContextHelpId");
		setExpandFirstNode(true);
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		addPage(new ExtractInterfaceInputPage());
	}
}
