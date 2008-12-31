/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.exampleprojects;

import java.net.URL;

import org.osgi.framework.Bundle;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * The main plugin class to be used in the desktop.
 */
public class ExampleProjectsPlugin extends AbstractUIPlugin {

	// The shared instance.
	private static ExampleProjectsPlugin fgPlugin;

	/**
	 * The constructor.
	 */
	public ExampleProjectsPlugin() {
		fgPlugin= this;
	}

	/**
	 * @return Returns the shared instance.
	 */
	public static ExampleProjectsPlugin getDefault() {
		return fgPlugin;
	}

	/**
	 * @return Returns the workspace instance.
	 */
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	/*
	 * Since 3.1.1. Load from icon paths with $NL$
	 */
	public static ImageDescriptor createImageDescriptor(Bundle bundle, IPath path) {
		URL url= FileLocator.find(bundle, path, null);
		if (url != null) {
			return ImageDescriptor.createFromURL(url);
		}
		return ImageDescriptor.getMissingImageDescriptor();
	}

	public static String getPluginId() {
		return "org.eclipse.jdt.ui.examples.projects"; //$NON-NLS-1$
	}


	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}


	public static void log(String message) {
		log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, message, null));
	}


	public static void log(Throwable e) {
		log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, "Internal Error", e)); //$NON-NLS-1$
	}


}
