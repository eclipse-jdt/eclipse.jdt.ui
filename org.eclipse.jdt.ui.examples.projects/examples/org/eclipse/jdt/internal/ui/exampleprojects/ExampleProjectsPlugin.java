/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.exampleprojects;

import java.net.MalformedURLException;import java.net.URL;import java.text.MessageFormat;import java.util.MissingResourceException;import java.util.ResourceBundle;import org.eclipse.swt.widgets.Display;import org.eclipse.core.resources.IWorkspace;import org.eclipse.core.resources.ResourcesPlugin;import org.eclipse.core.runtime.IPluginDescriptor;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Status;import org.eclipse.jface.resource.ImageDescriptor;import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * The main plugin class to be used in the desktop.
 */
public class ExampleProjectsPlugin extends AbstractUIPlugin {

	// The shared instance.
	private static ExampleProjectsPlugin fgPlugin;	// Resource bundle.
	private static ResourceBundle fgResourceBundle;

	/**
	 * The constructor.
	 */
	public ExampleProjectsPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		fgPlugin= this;
		try {
			fgResourceBundle= getDescriptor().getResourceBundle();
		} catch (MissingResourceException x) {
			log("ProjectTemplatesPlugin resourcebundle not found");
			fgResourceBundle= null;
		}
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

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle= ExampleProjectsPlugin.getResourceBundle();
		try {
			return bundle.getString(key);
		} catch (MissingResourceException e) {
			return key;
		}
	}

	/**
	 * Gets a string from the resource bundle and formats it with arguments
	 */	
	public static String getFormattedString(String key, String args) {
		return MessageFormat.format(getResourceString(key), new String[] { args });
	}
	/**
	 * Returns the plugin's resource bundle,
	 */
	public static ResourceBundle getResourceBundle() {
		return fgResourceBundle;
	}
	
	public ImageDescriptor getImageDescriptor(String name) {
		try {
			URL url;
			Display d= Display.getCurrent();
			if (d != null && d.getIconDepth() <= 4) {
				url= new URL(getDescriptor().getInstallURL(), "icons/basic/" + name);
			} else {
				url= new URL(getDescriptor().getInstallURL(), "icons/full/" + name);
			}
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
		log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, "Internal Error", e));
	}

}