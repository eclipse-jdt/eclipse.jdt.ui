/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.jarpackager;

import java.lang.ClassNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

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
			if (ExceptionHandler.handle(ex, shell, "JAR Export Error", "Creation of JAR failed"))
				return;
		} catch (InterruptedException e) {
			// do nothing on cancel
			return;
		}
		IStatus status= op.getStatus();
		if (!status.isOK())
			ErrorDialog.openError(shell, "JAR Export Problems", null, status);
	}
}