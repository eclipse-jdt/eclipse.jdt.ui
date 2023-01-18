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

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.osgi.framework.BundleContext;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class BytecodeOutlinePlugin extends AbstractUIPlugin {
	private static BytecodeOutlinePlugin plugin;

	private ResourceBundle resourceBundle;

	public static boolean DEBUG;

	public BytecodeOutlinePlugin() {
		super();
		if (plugin != null) {
			throw new IllegalStateException("Bytecode outline plugin is a singleton!"); //$NON-NLS-1$
		}
		plugin = this;
		try {
			resourceBundle = ResourceBundle.getBundle("org.eclipse.jdt.bcoview.BytecodeOutlinePluginResources"); //$NON-NLS-1$
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
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
	 * Returns the string from the plugin's resource bundle, or 'key' if not found.
	 *
	 * @param key preference key
	 * @return translation
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle = BytecodeOutlinePlugin.getDefault().getResourceBundle();
		try {
			return bundle != null ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}

	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}

	/**
	 * Returns the workspace instance.
	 *
	 * @return shell object
	 */
	public static Shell getShell() {
		return PlatformUI.getWorkbench().getDisplay().getActiveShell();
	}

	public static void error(String messageID, Throwable error) {
		Shell shell = getShell();
		String message = getResourceString("BytecodeOutline.Error"); //$NON-NLS-1$
		if (messageID != null) {
			message = getResourceString(messageID);
		}
		if (error != null) {
			message += " " + error.getMessage(); //$NON-NLS-1$
		}
		MessageDialog.openError(shell, getResourceString("BytecodeOutline.Title"), message); //$NON-NLS-1$
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
