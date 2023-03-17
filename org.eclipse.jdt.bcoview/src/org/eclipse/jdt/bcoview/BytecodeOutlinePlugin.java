/*******************************************************************************
 * Copyright (c) 2023 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview;

import org.osgi.framework.BundleContext;

import org.eclipse.jdt.bcoview.internal.Messages;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class BytecodeOutlinePlugin extends AbstractUIPlugin {
	private static BytecodeOutlinePlugin plugin;

	public static boolean DEBUG;

	public BytecodeOutlinePlugin() {
		super();
		if (plugin != null) {
			throw new IllegalStateException("Bytecode outline plugin is a singleton!"); //$NON-NLS-1$
		}
		plugin = this;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		DEBUG = isDebugging();
	}

	public static BytecodeOutlinePlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the workspace instance.
	 *
	 * @return shell object
	 */
	public static Shell getShell() {
		return PlatformUI.getWorkbench().getDisplay().getActiveShell();
	}

	public static void error(String message, Throwable error) {
		Shell shell = getShell();
		if (message == null) {
			message = Messages.BytecodeOutline_Error;
		}
		if (error != null) {
			message += " " + error.getMessage(); //$NON-NLS-1$
		}
		MessageDialog.openError(shell, Messages.BytecodeOutline_Title, message);
		getDefault().getLog().log(new Status(IStatus.ERROR, "org.eclipse.jdt.bcoview", 0, message, error)); //$NON-NLS-1$
	}

	public static void log(Throwable error, int severity) {
		String message = error.getMessage();
		if (message == null) {
			message = error.toString();
		}
		getDefault().getLog().log(new Status(severity, "org.eclipse.jdt.bcoview", 0, message, error)); //$NON-NLS-1$
	}


}
