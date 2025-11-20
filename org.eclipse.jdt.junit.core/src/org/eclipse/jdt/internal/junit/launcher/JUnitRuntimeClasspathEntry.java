/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
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
 *     David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.launcher;

import java.util.Objects;

public class JUnitRuntimeClasspathEntry {
	private final String fPluginId;

	private final String fPluginRelativePath;

	private final String fVersion;

	public JUnitRuntimeClasspathEntry(String pluginId, String jarFile) {
		this(pluginId, jarFile, null);
	}

	public JUnitRuntimeClasspathEntry(String pluginId, String jarFile, String version) {
		fPluginId = pluginId;
		fPluginRelativePath = jarFile;
		fVersion = version;
	}

	public String getPluginId() {
		return fPluginId;
	}

	public String getPluginRelativePath() {
		return fPluginRelativePath;
	}

	public String getVersion() {
		return fVersion;
	}

	public JUnitRuntimeClasspathEntry developmentModeEntry() {
		return new JUnitRuntimeClasspathEntry(getPluginId(), "bin", getVersion()); //$NON-NLS-1$
	}

	@Override
	public String toString() {
		return "ClasspathEntry(" + fPluginId + "/" + fPluginRelativePath + "[" + fVersion + "])"; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof JUnitRuntimeClasspathEntry))
			return false;
		JUnitRuntimeClasspathEntry other = (JUnitRuntimeClasspathEntry) obj;
		if (!fPluginId.equals(other.getPluginId()))
			return false;
		if (!Objects.equals(fPluginRelativePath,other.getPluginRelativePath())) {
			return false;
		}
		if (!Objects.equals(fVersion,other.getVersion())) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return Objects.hash(fPluginId,fPluginRelativePath,fVersion);
	}
}
