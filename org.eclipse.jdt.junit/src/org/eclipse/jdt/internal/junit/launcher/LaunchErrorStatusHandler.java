/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Achim Demelt <a.demelt@exxcellent.de> - [junit] Separate UI from non-UI code - https://bugs.eclipse.org/bugs/show_bug.cgi?id=278844
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.launcher;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.debug.core.IStatusHandler;

import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;

public class LaunchErrorStatusHandler implements IStatusHandler {

	public Object handleStatus(final IStatus status, Object source) throws CoreException {
		final Boolean[] success= new Boolean[] { Boolean.FALSE };
		getDisplay().syncExec(
				new Runnable() {
					public void run() {
						Shell shell= JUnitPlugin.getActiveWorkbenchShell();
						if (shell == null)
							shell= getDisplay().getActiveShell();
						if (shell != null) {
							MessageDialog.openInformation(shell, JUnitMessages.JUnitLaunchConfigurationDelegate_dialog_title, status.getMessage());
							success[0]= Boolean.TRUE;
						}
					}
				}
		);
		if (success[0] == Boolean.TRUE) {
			return null;
		}
		throw new CoreException(status);
	}

	private Display getDisplay() {
		Display display;
		display= Display.getCurrent();
		if (display == null)
			display= Display.getDefault();
		return display;
	}

}
