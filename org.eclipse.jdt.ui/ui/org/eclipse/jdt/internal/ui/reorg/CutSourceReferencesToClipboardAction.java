package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.custom.BusyIndicator;

import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jdt.core.ISourceReference;

import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class CutSourceReferencesToClipboardAction extends SourceReferenceAction {

	public CutSourceReferencesToClipboardAction(ISelectionProvider provider) {
		super("Cu&t", provider);
	}

	/*
	 * @see Action#run
	 */
	public void run() {
		if (! canOperateOn(getStructuredSelection()))
			return;
		
		new BusyIndicator().showWhile(JavaPlugin.getActiveWorkbenchShell().getDisplay(), new Runnable() {
			public void run() {
				try {
					perform(getSelectedElements());
				} catch (CoreException e) {
					ExceptionHandler.handle(e, "Cut", "Unexpected exception. See log for details.");
				}
			}
		});
	}
	
	static void perform(ISourceReference[] elements) throws CoreException {
		ISourceReference[] childrenRemoved= SourceReferenceUtil.removeAllWithParentsSelected(elements);
		CopySourceReferencesToClipboardAction.perform(childrenRemoved);
		DeleteSourceReferencesAction.perform(childrenRemoved);
	}	
}