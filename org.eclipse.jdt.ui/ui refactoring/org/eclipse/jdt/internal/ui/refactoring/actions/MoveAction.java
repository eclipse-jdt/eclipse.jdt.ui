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

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
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
		fMoveMembersAction= new MoveMembersAction(site);
		fJdtMoveAction= new JdtMoveAction(site);
	}
	
	public MoveAction(CompilationUnitEditor editor) {
		super(editor.getEditorSite());
		setText(RefactoringMessages.getString("MoveAction.text")); //$NON-NLS-1$
		fMoveMembersAction= new MoveMembersAction(editor);
	}	

	/*
	 * @see ISelectionChangedListener#selectionChanged(SelectionChangedEvent)
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		fMoveMembersAction.selectionChanged(event);
		if (fJdtMoveAction != null)
			fJdtMoveAction.selectionChanged(event);
		setEnabled(computeEnableState());	
	}

	/*
	 * @see IAction#run()
	 */
	public void run() {
		if (fJdtMoveAction != null && fJdtMoveAction.isEnabled())
			fJdtMoveAction.run();
		else if (fMoveMembersAction.isEnabled())	
			fMoveMembersAction.run();
	}
	
	/*
	 * @see IUpdate#update()
	 */
	public void update() {
		fMoveMembersAction.update();
		if (fJdtMoveAction != null)
			fJdtMoveAction.update();
		setEnabled(computeEnableState());
	}
	
	private boolean computeEnableState(){
		if (fJdtMoveAction == null)	
			return fMoveMembersAction.isEnabled();
		else
			return fMoveMembersAction.isEnabled() || fJdtMoveAction.isEnabled();
	}
}