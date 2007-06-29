/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * The main plug-in class to be used in the desktop.
 */
public class JdtTextTestPlugin extends AbstractUIPlugin {
	/**
	 * The plug-in id
	 * @since 3.1
	 */
	public static final String PLUGIN_ID= "org.eclipse.jdt.text.tests";
	//The shared instance.
	private static JdtTextTestPlugin plugin;
	//Resource bundle.
	private ResourceBundle resourceBundle;
	
	/**
	 * The constructor.
	 * 
	 * @param descriptor the plug-in descriptor
	 */
	public JdtTextTestPlugin() {
		plugin= this;
		try {
			resourceBundle= ResourceBundle.getBundle("org.eclipse.jdt.text.tests.JdtTextTestPluginResources"); //$NON-NLS-1$
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}

	/**
	 * Returns the shared instance.
	 * 
	 * @return the default plug-in instance 
	 */
	public static JdtTextTestPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the workspace instance.
	 * 
	 * @return the workspace 
	 */
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	/**
	 * Returns the string from the plugin's resource bundle.
	 * 
	 * @param key the resource key 
	 * @return the resource string or the given <code>key</code> if not found 
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle= JdtTextTestPlugin.getDefault().getResourceBundle();
		try {
			return (bundle!=null ? bundle.getString(key) : key);
		} catch (MissingResourceException e) {
			return key;
		}
	}

	/**
	 * Returns the plugin's resource bundle.
	 * 
	 * @return the resource bundle
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
}
