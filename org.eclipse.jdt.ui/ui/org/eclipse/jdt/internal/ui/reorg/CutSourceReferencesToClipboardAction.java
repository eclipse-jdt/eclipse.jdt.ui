package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.dnd.Clipboard;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.internal.corext.refactoring.Assert;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

public class CutSourceReferencesToClipboardAction extends SourceReferenceAction {

	CopySourceReferencesToClipboardAction fCopy;
	DeleteSourceReferencesAction fDelete;
	
	protected CutSourceReferencesToClipboardAction(IWorkbenchSite site, Clipboard clipboard) {
		super(site);
		setText(ReorgMessages.getString("CutSourceReferencesToClipboardAction.cut")); //$NON-NLS-1$
		fCopy= new CopySourceReferencesToClipboardAction(site, clipboard);
		fDelete= new DeleteSourceReferencesAction(site);
		update(getSelection());
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
