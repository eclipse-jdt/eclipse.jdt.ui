package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringAction;

public class DeleteAction extends RefactoringAction {
	
	private JdtDeleteResourceAction fDeleteResource;
	private DeleteSourceReferencesAction fDeleteSourceReference;
	
	public DeleteAction(ISelectionProvider provider) {
		super("&Delete", provider);
		setDescription(ReorgMessages.getString("deleteAction.description")); //$NON-NLS-1$
		fDeleteResource= new JdtDeleteResourceAction(provider);
		fDeleteSourceReference= new DeleteSourceReferencesAction(provider);
	}

	/*
	 * @see RefactoringAction#canOperateOn(IStructuredSelection)
	 */
	public boolean canOperateOn(IStructuredSelection selection) {
		boolean canDeleteResource= fDeleteResource.canOperateOn(selection);
		boolean canDeleteSourceReference= fDeleteSourceReference.canOperateOn(selection);
		Assert.isTrue(! canDeleteResource || ! canDeleteSourceReference);
		if (canDeleteResource)
			return true;
		return canDeleteSourceReference;	
	}
	
	public void update() {
		fDeleteResource.update();
		fDeleteSourceReference.update();
		super.update();
	}
	
	public void run(){
		Assert.isTrue(! fDeleteResource.isEnabled() || ! fDeleteSourceReference.isEnabled());
		Assert.isTrue(fDeleteResource.isEnabled() || fDeleteSourceReference.isEnabled());
		if (fDeleteResource.isEnabled())
			fDeleteResource.run();
		else if (fDeleteSourceReference.isEnabled())
			fDeleteSourceReference.run();	
	}
}

