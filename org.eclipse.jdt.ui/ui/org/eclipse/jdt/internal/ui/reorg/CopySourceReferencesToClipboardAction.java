package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;

import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jdt.core.IJavaElement;
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
		copyToOSClipbard(SourceReferenceUtil.removeAllWithParentsSelected(elements));
	}
		
	private static String convertToInputForOSClipboard(ISourceReference[] elems) throws JavaModelException {
		String lineDelim= System.getProperty("line.separator", "\n"); //$NON-NLS-1$
		StringBuffer buff= new StringBuffer();
		for (int i= 0; i < elems.length; i++) {
			buff.append(elems[i].getSource());
			if (i != elems.length)
				buff.append(lineDelim);
		}
		return buff.toString();
	}

	private static IJavaElement[] convertToJavaElementArray(ISourceReference[] refs){
		IJavaElement[] elems= new IJavaElement[refs.length];
		for (int i= 0; i < refs.length; i++) {
			elems[i]= (IJavaElement)refs[i];
		}
		return elems;
	}
	
	private static void copyToOSClipbard(ISourceReference[] refs)  throws JavaModelException {
		Clipboard clipboard = new Clipboard(JavaPlugin.getActiveWorkbenchShell().getDisplay());
		Object[] data= new Object[] { convertToInputForOSClipboard(refs), convertToJavaElementArray(refs)};
		Transfer[] transfers= new Transfer[] { TextTransfer.getInstance(), JavaElementTransfer.getInstance()};
		clipboard.setContents(data, transfers);
	}
}

