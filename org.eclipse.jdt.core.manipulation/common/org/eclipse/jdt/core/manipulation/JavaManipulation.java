/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.manipulation;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.core.resources.ProjectScope;

import org.eclipse.text.templates.ContextTypeRegistry;
import org.eclipse.text.templates.TemplateStoreCore;

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
	private static String fgPreferenceNodeId;

	private static TemplateStoreCore fTemplateStore;

	private static ContextTypeRegistry fCodeTemplateContextTypeRegistry;

	/**
	 * @return The id of the preference node for some basic Java preferences.
	 * Generally this will be <code>"org.eclipse.jdt.ui"</code> but some
	 * environments may not have the 'org.eclipse.jdt.ui' bundle, so a
	 * different one can be set.
	 * @since 1.10
	 */
	public static final String getPreferenceNodeId () {
		return fgPreferenceNodeId;
	}

	/**
	 * Sets the preference node to be used for basic Java preferences.
	 * The client should set the value back to null when finished.
	 *
	 * @param id the Id to use for the preference node
	 * @since 1.10
	 */
	public static final void setPreferenceNodeId (String id) {
		Assert.isLegal(fgPreferenceNodeId == null || id == null, "Preference node already set"); //$NON-NLS-1$
		fgPreferenceNodeId= id;
	}

	/**
	 * @since 1.11
	 */
	public static final TemplateStoreCore getCodeTemplateStore () {
		return fTemplateStore;
	}

	/**
	 * @since 1.11
	 */
	public static final void setCodeTemplateStore (TemplateStoreCore in) {
		fTemplateStore= in;
	}

	/**
	 * @since 1.11
	 */
	public static final ContextTypeRegistry getCodeTemplateContextRegistry () {
		return fCodeTemplateContextTypeRegistry;
	}

	/**
	 * @since 1.11
	 */
	public static final void setCodeTemplateContextRegistry (ContextTypeRegistry in) {
		fCodeTemplateContextTypeRegistry= in;
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
			val= new ProjectScope(project.getProject()).getNode(JavaManipulation.fgPreferenceNodeId).get(key, null);
			if (val != null) {
				return val;
			}
		}
		val= InstanceScope.INSTANCE.getNode(JavaManipulation.fgPreferenceNodeId).get(key, null);
		if (val != null) {
			return val;
		}
		return DefaultScope.INSTANCE.getNode(JavaManipulation.fgPreferenceNodeId).get(key, null);
	}
}
