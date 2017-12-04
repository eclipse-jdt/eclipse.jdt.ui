/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.manipulation;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.core.resources.ProjectScope;

import org.eclipse.jdt.core.IJavaProject;

/**
 * Central access point for the Java Manipulation plug-in (id <code>"org.eclipse.jdt.core.manipulation"</code>).
 */
public class JavaManipulation {

	/**
	 * The id of the Java Manipulation plug-in (value <code>"org.eclipse.jdt.core.manipulation"</code>).
	 */
	public static final String ID_PLUGIN= "org.eclipse.jdt.core.manipulation"; //$NON-NLS-1$

	/**
	 * Some operations can be performed without UI, but their preferences are stored
	 * in the JDT UI preference node. If JDT UI is not present in runtime, there are
	 * sane defaults, but if it exists, the preference node should be checked.
	 */
	private static String JAVA_UI_ID_PLUGIN;

	/**
	 * @return The id of the Java plug-in (value <code>"org.eclipse.jdt.ui"</code>).
	 * @since 1.10
	 */
	public static final String getJavaUIPluginId () {
		return JAVA_UI_ID_PLUGIN;
	}

	/**
	 * Sets JAVA_UI_ID_PLUGIN to the JDT UI plug-in Id.
	 * @param id the Id to set for JAVA_UI_ID_PLUGIN
	 * @since 1.10
	 */
	public static final void setJavaUIPluginId (String id) {
		JAVA_UI_ID_PLUGIN = id;
	}

	/**
	 * Returns the value for the given key in the given context for the JDT UI plug-in.
	 * @param key The preference key
	 * @param project The current context or <code>null</code> if no context is available and the
	 * workspace setting should be taken. Note that passing <code>null</code> should
	 * be avoided.
	 * @return Returns the current value for the string.
	 * @since 1.10
	 */
	public static String getPreference(String key, IJavaProject project) {
		String val;
		if (project != null) {
			val= new ProjectScope(project.getProject()).getNode(JavaManipulation.JAVA_UI_ID_PLUGIN).get(key, null);
			if (val != null) {
				return val;
			}
		}
		val= InstanceScope.INSTANCE.getNode(JavaManipulation.JAVA_UI_ID_PLUGIN).get(key, null);
		if (val != null) {
			return val;
		}
		return DefaultScope.INSTANCE.getNode(JavaManipulation.JAVA_UI_ID_PLUGIN).get(key, null);
	}
}
