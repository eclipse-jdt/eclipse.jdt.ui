package org.eclipse.jdt.internal.ui.refactoring.actions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.UnifiedSite;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoring;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.PullUpWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;

public class PullUpAction extends OpenRefactoringWizardAction {

	private static final String TITLE= RefactoringMessages.getString("RefactoringGroup.pull_Up_label"); //$NON-NLS-1$
	public PullUpAction(CompilationUnitEditor editor) {
		super(TITLE, editor, IMember.class);
	}

	public PullUpAction(UnifiedSite site) {
		super(TITLE, site, IMember.class);
	}

	protected Refactoring createNewRefactoringInstance(Object obj){
		Set memberSet= new HashSet();
		memberSet.addAll(Arrays.asList((Object[])obj));
		IMember[] members= (IMember[]) memberSet.toArray(new IMember[memberSet.size()]);
		return new PullUpRefactoring(members, JavaPreferencesSettings.getCodeGenerationSettings());
	}
	protected boolean canActivateRefactoring(Refactoring refactoring)  throws JavaModelException{
		return ((PullUpRefactoring)refactoring).checkPreactivation().isOK();
	}
	protected boolean canOperateOnMultiSelection(){
		return true;
	}	
	protected RefactoringWizard createWizard(Refactoring ref){
		String title= RefactoringMessages.getString("RefactoringGroup.pull_up"); //$NON-NLS-1$
		//FIX ME: wrong
		String helpId= "HELPID"; //$NON-NLS-1$
		return new PullUpWizard((PullUpRefactoring)ref, title, helpId);
	}
}
