package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

class DualReorgAction extends SelectionDispatchAction {

	SelectionDispatchAction fResourceAction;
	SelectionDispatchAction fSourceReferenceAction;
	
	protected DualReorgAction(IWorkbenchSite site, String text, String description, SelectionDispatchAction resourceAction, SelectionDispatchAction sourceReferenceAction, String helpContextID) {
		super(site);
		setText(text);
		setDescription(description);
		fResourceAction= resourceAction;
		fSourceReferenceAction= sourceReferenceAction;
		update(getSelection());
		WorkbenchHelp.setHelp(this, helpContextID);
	}

	/*
	 * @see IAction#run()
	 */
	public void run() {
		if (fResourceAction.isEnabled())
			fResourceAction.run();
		else if (fSourceReferenceAction.isEnabled())
			fSourceReferenceAction.run();	
	}

	/* (non-Javadoc)
	 * Method declared on ISelectionChangedListener.
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		fResourceAction.selectionChanged(event);
		fSourceReferenceAction.selectionChanged(event);		
		setEnabled(computeEnabledState());
	}

	/*
	 * @see IUpdate#update()
	 */
	public void update(ISelection selection) {
		fResourceAction.update(selection);
		fSourceReferenceAction.update(selection);
		setEnabled(computeEnabledState());
	}
	
	private boolean computeEnabledState(){
		if (! (fResourceAction.isEnabled() || fSourceReferenceAction.isEnabled()))
			return false;

		if (fResourceAction.isEnabled() && fSourceReferenceAction.isEnabled())	
			return false;
		
		return true;	
	}
}
