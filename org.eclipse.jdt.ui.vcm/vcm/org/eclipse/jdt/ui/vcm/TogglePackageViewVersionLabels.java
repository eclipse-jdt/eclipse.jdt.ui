/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.vcm;
 
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;

/** 
 * This action decorates the packages view with
 * version labels.
 */
public class TogglePackageViewVersionLabels implements IViewActionDelegate {
	private IViewPart fPart;
	/**
	 * @see IViewActionDelegate#init(IViewPart)
	 */
	public void init(IViewPart part) {
		fPart= part;
	}
	/**
	 * @see IActionDelegate#run(IAction)
	 */ 
	public void run(IAction action) {
		if (fPart instanceof PackageExplorerPart) {
			final PackageExplorerPart packageExplorer= (PackageExplorerPart) fPart;
			if (action.isChecked()) {
				final Shell shell= fPart.getSite().getShell();
				BusyIndicator.showWhile(shell.getDisplay(), new Runnable() {
					public void run() {
						packageExplorer.setLabelDecorator(new JavaVCMLabelDecorator(shell));
					}
				});
			} else {
				packageExplorer.setLabelDecorator(null);
			}
		}
	}
	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}
}