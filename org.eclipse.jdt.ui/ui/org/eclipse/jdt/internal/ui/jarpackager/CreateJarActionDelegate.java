/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.jarpackager;

import java.lang.reflect.InvocationTargetException;import org.eclipse.core.runtime.IStatus;import org.eclipse.swt.widgets.Shell;import org.eclipse.jface.action.IAction;import org.eclipse.jface.dialogs.ProgressMonitorDialog;import org.eclipse.jdt.internal.ui.dialogs.ProblemDialog;import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class CreateJarActionDelegate extends JarPackageActionDelegate {

	/*
	 * @see IActionDelegate
	 */
	public void run(IAction action) {
		Shell shell= getShell();
		JarFileExportOperation op= new JarFileExportOperation(getDescriptionFiles(getSelection()), shell);
		try {
			ProgressMonitorDialog dialog= new ProgressMonitorDialog(shell);
			dialog.run(true, true, op);
		} catch (InvocationTargetException ex) {
			if (ExceptionHandler.handle(ex, shell, JarPackagerMessages.getString("CreateJarActionDelegate.jarExportError.title"), JarPackagerMessages.getString("CreateJarActionDelegate.jarExportError.message"))) //$NON-NLS-2$ //$NON-NLS-1$
				return;
		} catch (InterruptedException e) {
			// do nothing on cancel
			return;
		}
		IStatus status= op.getStatus();
		if (!status.isOK())
			ProblemDialog.open(shell, JarPackagerMessages.getString("CreateJarActionDelegate.jarExportProblems"), null, status); //$NON-NLS-1$
	}
}