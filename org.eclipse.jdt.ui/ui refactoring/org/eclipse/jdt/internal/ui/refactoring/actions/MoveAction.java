package org.eclipse.jdt.internal.ui.refactoring.actions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.structure.MoveMembersRefactoring;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.MoveMembersWizard;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;
import org.eclipse.jdt.internal.ui.reorg.JdtMoveAction;

public class MoveAction extends SelectionDispatchAction{
	
	private SelectionDispatchAction fMoveMembersAction;
	private SelectionDispatchAction fJdtMoveAction;
	
	public MoveAction(IWorkbenchSite site) {
		super(site);
		setText(RefactoringMessages.getString("MoveAction.text")); //$NON-NLS-1$
		fMoveMembersAction= MoveAction.createMoveMembersAction(site);
		fJdtMoveAction= new JdtMoveAction(site);
	}

	/*
	 * @see ISelectionChangedListener#selectionChanged(SelectionChangedEvent)
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		fMoveMembersAction.selectionChanged(event);
		fJdtMoveAction.selectionChanged(event);
		setEnabled(fMoveMembersAction.isEnabled() || fJdtMoveAction.isEnabled());
	}

	/*
	 * @see IAction#run()
	 */
	public void run() {
		if (fJdtMoveAction.isEnabled())
			fJdtMoveAction.run();
		else if (fMoveMembersAction.isEnabled())	
			fMoveMembersAction.run();
	}
	
	/*
	 * @see IUpdate#update()
	 */
	public void update() {
		fMoveMembersAction.update();
		fJdtMoveAction.update();
		setEnabled(fMoveMembersAction.isEnabled() || fJdtMoveAction.isEnabled());
	}

	private static OpenRefactoringWizardAction createMoveMembersAction(IWorkbenchSite site) {
		String label= RefactoringMessages.getString("RefactoringGroup.move_label"); //$NON-NLS-1$
		return new OpenRefactoringWizardAction(label, site, IMember.class) {
			protected Refactoring createNewRefactoringInstance(Object obj){
				Set memberSet= new HashSet();
				memberSet.addAll(Arrays.asList((Object[])obj));
				IMember[] methods= (IMember[]) memberSet.toArray(new IMember[memberSet.size()]);
				return new MoveMembersRefactoring(methods, JavaPreferencesSettings.getCodeGenerationSettings());
			}
			protected boolean canActivateRefactoring(Refactoring refactoring)  throws JavaModelException{
				return ((MoveMembersRefactoring)refactoring).checkActivation(new NullProgressMonitor()).isOK();
			}
			protected boolean canOperateOnMultiSelection(){
				return true;
			}	
			protected RefactoringWizard createWizard(Refactoring ref){
				String title= RefactoringMessages.getString("RefactoringGroup.move_Members"); //$NON-NLS-1$
				//FIX ME: wrong
				String helpId= "HELPID"; //$NON-NLS-1$
				return new MoveMembersWizard((MoveMembersRefactoring)ref, title, helpId);
			}
		};
	}	

}