/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
 *     Microsoft Corporation - based this file on FormatterProfileManager
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.eclipse.core.resources.ProjectScope;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.manipulation.JavaManipulation;

public class FormatterProfileManagerCore {
	public final static String FORMATTER_SETTINGS_VERSION= "formatter_settings_version"; //$NON-NLS-1$

	public static Map<String, String> getProjectSettings(IJavaProject javaProject) {
		Map<String, String> options= new HashMap<>(javaProject.getOptions(true));
		IEclipsePreferences prefs= new ProjectScope(javaProject.getProject()).getNode(JavaManipulation.getPreferenceNodeId());
		if (prefs == null)
			return options;
		int profileVersion= prefs.getInt(FORMATTER_SETTINGS_VERSION, ProfileVersionerCore.getCurrentVersion());
		if (profileVersion == ProfileVersionerCore.getCurrentVersion())
			return options;
		return ProfileVersionerCore.updateAndComplete(options, profileVersion);
	}
}
