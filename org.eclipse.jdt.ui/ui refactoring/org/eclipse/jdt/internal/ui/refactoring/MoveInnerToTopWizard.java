package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveInnerToTopRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.JdtFlags;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;

public class MoveInnerToTopWizard extends RefactoringWizard {

	public MoveInnerToTopWizard(Refactoring ref) {
		super(ref, "Move Inner Type to Top Level", IJavaHelpContextIds.MOVE_INNER_TO_TOP_ERROR_WIZARD_PAGE);
//		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_PULL_UP);
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		try{			
			//no input page if the type is static
			if (! JdtFlags.isStatic(getMoveRefactoring().getInputType()))
				addPage(new MoveInnerToToplnputPage());
			else
				setChangeCreationCancelable(false);
		} catch (JavaModelException e){
			//log and try anyway
			JavaPlugin.log(e);
			addPage(new MoveInnerToToplnputPage()); 
		}		
	}

	private MoveInnerToTopRefactoring getMoveRefactoring() {
		return (MoveInnerToTopRefactoring)getRefactoring();
	}
}
