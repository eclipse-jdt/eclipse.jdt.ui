package org.eclipse.jdt.internal.ui.refactoring.actions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveMembersRefactoring;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.MoveMembersWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;

public class MoveMembersAction extends OpenRefactoringWizardAction {
	private static final String LABEL= RefactoringMessages.getString("RefactoringGroup.move_label"); //$NON-NLS-1$
	private static final String UNAVAILABLE= "To activate this refactoring, please select the name of a static method or field";

	protected MoveMembersAction(IWorkbenchSite site) {
		super(LABEL, UNAVAILABLE, site, IMember.class, true);
	}

	protected MoveMembersAction(CompilationUnitEditor editor) {
		super(LABEL, UNAVAILABLE, editor, IMember.class, true);
	}

	protected Refactoring createNewRefactoringInstance(Object obj){
		Set memberSet= new HashSet();
		memberSet.addAll(Arrays.asList((Object[])obj));
		IMember[] methods= (IMember[]) memberSet.toArray(new IMember[memberSet.size()]);
		return new MoveMembersRefactoring(methods, JavaPreferencesSettings.getCodeGenerationSettings());
	}
	protected boolean canActivateRefactoring(Refactoring refactoring)  throws JavaModelException{
		return ((MoveMembersRefactoring)refactoring).checkPreactivation().isOK();
	}
	protected RefactoringWizard createWizard(Refactoring ref){
		String title= RefactoringMessages.getString("RefactoringGroup.move_Members"); //$NON-NLS-1$
		//FIX ME: wrong
		String helpId= "HELPID"; //$NON-NLS-1$
		return new MoveMembersWizard((MoveMembersRefactoring)ref, title, helpId);
	}

}
