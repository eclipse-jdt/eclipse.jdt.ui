/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import java.net.URL;

import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.help.IHelp;

public class OpenBrowserUtil {
	
	public static void open(final URL url, Shell shell, String dialogTitle) {
		if (shell == null) {
			shell= JavaPlugin.getActiveWorkbenchShell();
			if (shell == null) {
				return;
			}
		}
		
		IHelp help= WorkbenchHelp.getHelpSupport();
		if (help != null) {
			BusyIndicator.showWhile(shell.getDisplay(), new Runnable() {
				public void run() {
					WorkbenchHelp.getHelpSupport().displayHelpResource(url.toExternalForm());
				}
			});			
		} else {
			showMessage(shell, dialogTitle, ActionMessages.getString("OpenBrowserUtil.help_not_available"), false); //$NON-NLS-1$
		}
	}
	
	private static void showMessage(final Shell shell, final String title, final String message, final boolean isError) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				if (isError) {
					MessageDialog.openError(shell, title, message);
				} else {
					MessageDialog.openInformation(shell, title, message);
				}
			}
		});
	}	
}
