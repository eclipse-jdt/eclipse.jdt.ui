/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.reorg;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.actions.CopyProjectAction;

import org.eclipse.jdt.internal.corext.refactoring.reorg.CopyRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgRefactoring;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;

public class CopyAction extends ReorgDestinationAction {
	
	public CopyAction(StructuredSelectionProvider provider) {
		super(ReorgMessages.getString("copyAction.label"), provider); //$NON-NLS-1$
		setDescription(ReorgMessages.getString("copyAction.description")); //$NON-NLS-1$
	}
	
	public CopyAction(String name, StructuredSelectionProvider provider) {
		super(name, provider);
	}
	
	ReorgRefactoring createRefactoring(List elements){
		return new CopyRefactoring(elements);
	}
	
	String getActionName() {
		return ReorgMessages.getString("copyAction.name"); //$NON-NLS-1$
	}
	
	String getDestinationDialogMessage() {
		return ReorgMessages.getString("copyAction.destination.label"); //$NON-NLS-1$
	}
	
	/* non java-doc
	 * @see IRefactoringAction#canOperateOn(IStructuredSelection)
	 */
	public boolean canOperateOn(IStructuredSelection selection) {
		if (hasOnlyProjects())
			return selection.size() == 1;
		else
			return super.canOperateOn(selection);
	}
	
	/*
	 * @see Action#run()
	 */
	public void run() {
		if (hasOnlyProjects()){
			copyProject();
		}	else {
			super.run();
		}
	}

	private void copyProject(){
		CopyProjectAction action= new CopyProjectAction(JavaPlugin.getActiveWorkbenchShell());
		action.selectionChanged(getStructuredSelection());
		action.run();
	}
}
