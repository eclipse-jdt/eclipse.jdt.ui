package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.viewers.ISelectionProvider;

class CutSourceReferencesToClipboardAction extends SourceReferenceAction {

	public CutSourceReferencesToClipboardAction(ISelectionProvider provider) {
		super(ReorgMessages.getString("CutSourceReferencesToClipboardAction.cut"), provider); //$NON-NLS-1$
	}
	
	protected void perform() throws CoreException {
		CopySourceReferencesToClipboardAction copyAction= new CopySourceReferencesToClipboardAction(getSelectionProvider());
		copyAction.update();
		copyAction.perform();
		
		DeleteSourceReferencesAction deleteAction= new DeleteSourceReferencesAction(getSelectionProvider());
		deleteAction.update();
		deleteAction.perform();
	}	
}