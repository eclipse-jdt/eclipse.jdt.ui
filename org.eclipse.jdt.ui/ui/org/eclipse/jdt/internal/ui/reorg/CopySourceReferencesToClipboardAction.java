package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;

import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class CopySourceReferencesToClipboardAction extends SourceReferenceAction {

	public CopySourceReferencesToClipboardAction(ISelectionProvider provider) {
		super("&Copy", provider);
	}

	/*
	 * @see Action#run
	 */
	public void run() {
		new BusyIndicator().showWhile(JavaPlugin.getActiveWorkbenchShell().getDisplay(), new Runnable() {
			public void run() {
				try {
					perform(getSelectedElements());
				} catch (JavaModelException e) {
					ExceptionHandler.handle(e, "Copy", "Unexpected exception. See log for details.");
				}
			}
		});
	}
	
	static void perform(ISourceReference[] elements) throws JavaModelException {
		ISourceReference[] childrenRemoved= SourceReferenceUtil.removeAllWithParentsSelected(elements);
		SourceReferenceClipboard.setContent(childrenRemoved);
		copyToOSClipbard(convertToInputForOSClipboard(childrenRemoved));
	}
		
	private static String convertToInputForOSClipboard(ISourceReference[] elems)  throws JavaModelException {
		String lineDelim= System.getProperty("line.separator", "\n"); //$NON-NLS-1$
		StringBuffer buff= new StringBuffer();
		for (int i= 0; i < elems.length; i++) {
			ISourceReference elem= elems[i];
			buff.append(SourceReferenceSourceRangeComputer.computeSource(elem));
			if (i != elems.length)
				buff.append(lineDelim);
		}
		return buff.toString();
	}

	private static void copyToOSClipbard(String str) {
		Clipboard clipboard = new Clipboard(JavaPlugin.getActiveWorkbenchShell().getDisplay());
		clipboard.setContents(new String[] { str }, new Transfer[] { TextTransfer.getInstance()});
	}
}

