package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jface.dialogs.IMessageProvider;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.code.ExtractConstantRefactoring;
import org.eclipse.jdt.internal.ui.JavaPlugin;

public class ExtractConstantWizard extends RefactoringWizard {

	private static final String MESSAGE = RefactoringMessages.getString("ExtractConstantInputPage.enter_name"); //$NON-NLS-1$

	public ExtractConstantWizard(ExtractConstantRefactoring ref, String pageTitle, String errorPageContextHelpId) {
		super(ref, pageTitle, errorPageContextHelpId);
		setExpandFirstNode(true);
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */
	protected void addUserInputPages() {

		String message= null;
		int messageType= IMessageProvider.NONE;			
		if(!getExtractConstantRefactoring().selectionAllStaticFinal()) {
			message= RefactoringMessages.getString("ExtractConstantInputPage.selection_refers_to_nonfinal_fields");  //$NON-NLS-1$
			messageType= IMessageProvider.INFORMATION;
		} else {	
			message= MESSAGE;
			messageType= IMessageProvider.NONE;
		}
		
		addPage(new ExtractConstantInputPage(message, messageType, guessName()));
	}


	private String guessName() {
		try {
			return getExtractConstantRefactoring().guessConstantName();
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return "";//default value. no ui here, just log
		}
	}

	private ExtractConstantRefactoring getExtractConstantRefactoring(){
		return (ExtractConstantRefactoring)getRefactoring();
	}
	
}
