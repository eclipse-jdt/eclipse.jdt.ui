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

public class OpenBrowserUtil {
	
	public static void open(final URL url, Display display, final String dialogTitle) {
		display.syncExec(new Runnable() {
			public void run() {
				internalOpen(url, dialogTitle);
			}
		});
	}
	
	private static void internalOpen(final URL url, String title) {
		if (WorkbenchHelp.getHelpSupport() != null) {	// filed bug 57435 to avoid deprecation
			BusyIndicator.showWhile(null, new Runnable() {
				public void run() {
					WorkbenchHelp.displayHelpResource(url.toExternalForm() + "?noframes=true"); //$NON-NLS-1$
				}
			});			
		} else {
			Shell shell= JavaPlugin.getActiveWorkbenchShell();
			String message= ActionMessages.getString("OpenBrowserUtil.help_not_available"); //$NON-NLS-1$
			MessageDialog.openInformation(shell, title, message);
		}
	}
}
