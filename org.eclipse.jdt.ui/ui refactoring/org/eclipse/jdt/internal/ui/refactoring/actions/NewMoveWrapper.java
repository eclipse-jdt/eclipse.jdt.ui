package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jdt.ui.actions.UnifiedSite;

import org.eclipse.jdt.internal.ui.reorg.NewJdtMoveAction;

public class NewMoveWrapper extends SelectionDispatchAction{
	
	private SelectionDispatchAction fMoveMembersAction;
	private SelectionDispatchAction fJdtMoveAction;
	
	public NewMoveWrapper(UnifiedSite site) {
		super(site);
		setText("Mo&ve...");
		fMoveMembersAction= RefactoringGroup.createMoveMembersAction(site);
		fJdtMoveAction= new NewJdtMoveAction(site);
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

}