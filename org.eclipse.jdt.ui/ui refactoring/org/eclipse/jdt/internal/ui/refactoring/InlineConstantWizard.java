package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.internal.corext.refactoring.code.InlineConstantRefactoring;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jface.dialogs.IMessageProvider;

public class InlineConstantWizard extends RefactoringWizard {

	private static final String MESSAGE = "Specify where to inline references to the constant.";

	public InlineConstantWizard(InlineConstantRefactoring ref) {
		super(ref, "Inline Constant", IJavaHelpContextIds.INLINE_CONSTANT_ERROR_WIZARD_PAGE);
		setExpandFirstNode(true);
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */
	protected void addUserInputPages() {

		String message= null;
		int messageType= IMessageProvider.NONE;			
		if(!getInlineConstantRefactoring().isInitializerAllStaticFinal()) {
			message= "This constant's initializer refers to non-final or non-static fields";
			messageType= IMessageProvider.INFORMATION;
		} else {	
			message= MESSAGE;
			messageType= IMessageProvider.NONE;
		}
		
		addPage(new InlineConstantInputPage(message, messageType));
	}

	private InlineConstantRefactoring getInlineConstantRefactoring(){
		return (InlineConstantRefactoring)getRefactoring();
	}
	
}