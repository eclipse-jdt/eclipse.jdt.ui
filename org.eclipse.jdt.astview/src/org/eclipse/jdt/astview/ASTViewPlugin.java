/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.astview;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPluginDescriptor;

import org.eclipse.ui.plugin.AbstractUIPlugin;

public class ASTViewPlugin extends AbstractUIPlugin {

	private static ASTViewPlugin fDefault;

	public ASTViewPlugin(IPluginDescriptor desc) {
		fDefault = this;
	}
	
	public static String getPluginId() {
		return "org.eclipse.jdt.astview"; //$NON-NLS-1$
	}

	/**
	 * @return the shared instance
	 */
	public static ASTViewPlugin getDefault() {
		return fDefault;
	}

	/**
	 * @return the workspace instance
	 */
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}
	
	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}
	
	public static void logErrorMessage(String message) {
		log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, message, null));
	}
	
	public static void logErrorStatus(String message, IStatus status) {
		if (status == null) {
			logErrorMessage(message);
			return;
		}
		MultiStatus multi= new MultiStatus(getPluginId(), IStatus.ERROR, message, null);
		multi.add(status);
		log(multi);
	}
	
	public static void log(String message, Throwable e) {
		log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, message, e)); //$NON-NLS-1$
	}
	
}
