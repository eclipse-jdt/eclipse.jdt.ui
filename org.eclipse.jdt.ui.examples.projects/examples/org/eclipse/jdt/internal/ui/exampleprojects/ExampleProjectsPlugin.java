/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.exampleprojects;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

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
	public ExampleProjectsPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		fgPlugin= this;
	}


	/**
	 * Returns the shared instance.
	 */
	public static ExampleProjectsPlugin getDefault() {
		return fgPlugin;
	}
	
	/**
	 * Returns the workspace instance.
	 */
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	public ImageDescriptor getImageDescriptor(String name) {
		try {
			URL url= new URL(getDescriptor().getInstallURL(), name);
			return ImageDescriptor.createFromURL(url);
		} catch (MalformedURLException e) {
			return ImageDescriptor.getMissingImageDescriptor();
		}
	}	
	
	public static String getPluginId() {
		return getDefault().getDescriptor().getUniqueIdentifier();
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