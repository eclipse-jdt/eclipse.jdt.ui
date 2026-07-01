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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.manipulation.JavaManipulation;

public class FormatterProfileManagerCore {
	private static final String EDITORCONFIG= ".editorconfig"; //$NON-NLS-1$

	public final static String FORMATTER_SETTINGS_VERSION= "formatter_settings_version"; //$NON-NLS-1$

	public static Map<String, String> getProjectSettings(IJavaProject javaProject) {
		Map<String, String> map= computeDefaults(javaProject);
		IPath editorConfig= findEditorConfig(javaProject.getProject());
		if (editorConfig != null) {
			try {
				List<String> lines= Files.readAllLines(editorConfig.toFile().toPath());
				String tabChar= getSetting(lines, "indent_style"); //$NON-NLS-1$
				if (tabChar != null) {
					System.out.println("Override tab char from editor config with value: " + tabChar); //$NON-NLS-1$
					map.put("org.eclipse.jdt.core.formatter.tabulation.char", tabChar); //$NON-NLS-1$
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return map;
	}

	private static String getSetting(List<String> lines, String property) {
		boolean found= false;
		for (String line : lines) {
			if (line.equals("[*]") || line.equals("[*.java]")) { //$NON-NLS-1$ //$NON-NLS-2$
				found= true;
			} else if (line.startsWith("[")) { //$NON-NLS-1$
				found= false;
			} else if (found) {
				if (line.startsWith(property)) {
					String[] split= line.split("="); //$NON-NLS-1$
					return split[1].trim();
				}
			}
		}
		return null;
	}

	private static IPath findEditorConfig(IProject project) {
		IFile projectFile= project.getFile(EDITORCONFIG);
		if (projectFile.exists()) {
			return projectFile.getLocation();
		}
		IPath location= project.getLocation();
		if (location == null) {
			return null;
		}
		File file= location.toFile();
		return findEditorConfig(file.getParentFile(), project.getWorkspace().getRoot().getLocation().toFile());
	}

	private static IPath findEditorConfig(File folder, File workspaceRoot) {
		if (folder == null || folder.equals(workspaceRoot)) {
			return null;
		}
		File file= new File(folder, EDITORCONFIG);
		if (file.exists()) {
			return IPath.fromFile(file);
		}
		if (new File(folder, ".git").isDirectory()) { //$NON-NLS-1$
			//do not search beyond boundaries of git workingcopy
			return null;
		}
		return findEditorConfig(folder.getParentFile(), workspaceRoot);
	}

	protected static Map<String, String> computeDefaults(IJavaProject javaProject) {
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
