/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.actions;
import java.util.Iterator;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class CopyNamesToClipboardAction implements IViewActionDelegate {
	
	private ISelection fLastSelection;
	private ILabelProvider fLabelProvider;
	
	public CopyNamesToClipboardAction() {
		fLastSelection= null;
		fLabelProvider= new JavaElementLabelProvider(
			JavaElementLabelProvider.SHOW_VARIABLE
			+ JavaElementLabelProvider.SHOW_PARAMETERS
			+ JavaElementLabelProvider.SHOW_TYPE
		);
	
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */	
	public void run(IAction action) {
		if (fLastSelection == null || fLastSelection.isEmpty()) {
			return;
		}
		Shell shell= JavaPlugin.getActiveWorkbenchShell();
		if (shell == null) {
			return;
		}
		
		if (fLastSelection instanceof IStructuredSelection) {
			String lineDelim= System.getProperty("line.separator"); //$NON-NLS-1$
			StringBuffer buf= new StringBuffer();
			Iterator iter= ((IStructuredSelection)fLastSelection).iterator();
			while (iter.hasNext()) {
				if (buf.length() > 0) {
					buf.append(lineDelim);
				}
				buf.append(fLabelProvider.getText(iter.next()));
			}
			
			if (buf.length() > 0) {
				copyToClipbard(shell.getDisplay(), buf.toString());
			}
		} else {
			copyToClipbard(shell.getDisplay(), fLastSelection.toString());
		}
	}
		
	private void copyToClipbard(Display display, String str) {
		Clipboard clipboard = new Clipboard(display);
		clipboard.setContents(new String[] { str },	new Transfer[] { TextTransfer.getInstance()});			
	}
	
	/**
	 * @see IViewActionDelegate#init(IViewPart)
	 */
	public void init(IViewPart view) {
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		fLastSelection= selection;
	}

}