package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jdt.ui.actions.UnifiedSite;

public class CutSourceReferencesToClipboardAction extends SourceReferenceAction {

	CopySourceReferencesToClipboardAction fCopy;
	DeleteSourceReferencesAction fDelete;
	
	protected CutSourceReferencesToClipboardAction(UnifiedSite site) {
		super(site);
		setText(ReorgMessages.getString("CutSourceReferencesToClipboardAction.cut")); //$NON-NLS-1$
		fCopy= new CopySourceReferencesToClipboardAction(site);
		fDelete= new DeleteSourceReferencesAction(site);
		update();
	}
	
	protected void perform(IStructuredSelection selection) throws CoreException {
		fCopy.perform(selection);
		fDelete.perform(selection);
	}

	protected void selectionChanged(IStructuredSelection selection) {
		fCopy.selectionChanged(selection);
		fDelete.selectionChanged(selection);
		setEnabled(fCopy.isEnabled() && fDelete.isEnabled());
	}
}
