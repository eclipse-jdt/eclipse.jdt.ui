package org.eclipse.jdt.internal.ui.reorg;

import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.dnd.Clipboard;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

public class CutSourceReferencesToClipboardAction extends SourceReferenceAction {

	CopySourceReferencesToClipboardAction fCopy;
	DeleteSourceReferencesAction fDelete;
	
	protected CutSourceReferencesToClipboardAction(IWorkbenchSite site, Clipboard clipboard, SelectionDispatchAction pasteAction, String helpContextID) {
		super(site);
		setText(ReorgMessages.getString("CutSourceReferencesToClipboardAction.cut")); //$NON-NLS-1$
		fCopy= new CopySourceReferencesToClipboardAction(site, clipboard, pasteAction);
		fDelete= new DeleteSourceReferencesAction(site);
		update(getSelection());
		WorkbenchHelp.setHelp(this, helpContextID);
	}
	
	protected void perform(IStructuredSelection selection) throws CoreException {
		fCopy.perform(selection);
		fDelete.perform(selection);
	}

	protected void selectionChanged(IStructuredSelection selection) {
		/*
		 * cannot cut top-level types. this deltes the cu and then you cannot paste because the cu is gone. 
		 */
		if (containsTopLevelTypes(selection)){
			setEnabled(false);
			return;
		}	
		fCopy.selectionChanged(selection);
		fDelete.selectionChanged(selection);
		setEnabled(fCopy.isEnabled() && fDelete.isEnabled());
	}

	private static boolean containsTopLevelTypes(IStructuredSelection selection) {
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			Object each= iter.next();
			if ((each instanceof IType) && ((IType)each).getDeclaringType() == null)
				return true;
		}
		return false;
	}
}
