package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.ui.refactoring.actions.IRefactoringAction;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringAction;

class DualReorgAction extends RefactoringAction {
	
	private IRefactoringAction fResourceAction;
	private IRefactoringAction fSourceReferenceAction;
	
	public DualReorgAction(ISelectionProvider provider, String text, String description, IRefactoringAction resourceAction, IRefactoringAction sourceReferenceAction) {
		super(text, provider);
		Assert.isNotNull(text);
		Assert.isNotNull(description);
		Assert.isNotNull(resourceAction);
		Assert.isNotNull(sourceReferenceAction);
		setDescription(description);
		fResourceAction= resourceAction;
		fSourceReferenceAction= sourceReferenceAction;
	}

	/*
	 * @see RefactoringAction#canOperateOn(IStructuredSelection)
	 */
	public boolean canOperateOn(IStructuredSelection selection) {
		boolean canReorgResources= fResourceAction.isEnabled();
		boolean canReorgSourceReferences= fSourceReferenceAction.isEnabled();
		Assert.isTrue(! canReorgResources || ! canReorgSourceReferences);
		return (canReorgResources || canReorgSourceReferences);
	}
	
	public void update() {
		fResourceAction.update();
		fSourceReferenceAction.update();
		super.update();
	}
	
	public void run(){
		update();
		if (! isEnabled())
			return;
		Assert.isTrue(! fResourceAction.isEnabled() || ! fSourceReferenceAction.isEnabled());
		Assert.isTrue(fResourceAction.isEnabled() || fSourceReferenceAction.isEnabled());
		if (fResourceAction.isEnabled())
			fResourceAction.run();
		else if (fSourceReferenceAction.isEnabled())
			fSourceReferenceAction.run();	
	}
}