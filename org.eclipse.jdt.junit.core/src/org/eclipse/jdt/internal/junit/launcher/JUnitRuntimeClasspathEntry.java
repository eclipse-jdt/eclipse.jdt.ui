/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.launcher;

public class JUnitRuntimeClasspathEntry {
	private final String fPluginId;

	private final String fPluginRelativePath;

	public JUnitRuntimeClasspathEntry(String pluginId, String jarFile) {
		fPluginId = pluginId;
		fPluginRelativePath = jarFile;
	}

	public String getPluginId() {
		return fPluginId;
	}

	public String getPluginRelativePath() {
		return fPluginRelativePath;
	}

	JUnitRuntimeClasspathEntry developmentModeEntry() {
		return new JUnitRuntimeClasspathEntry(getPluginId(), "bin"); //$NON-NLS-1$
	}

	public String toString() {
		return "ClasspathEntry(" + fPluginId + "/" + fPluginRelativePath + ")"; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof JUnitRuntimeClasspathEntry))
			return false;
		JUnitRuntimeClasspathEntry other = (JUnitRuntimeClasspathEntry) obj;
		return fPluginId.equals(other.getPluginId())
				&& ( (fPluginRelativePath == null && other.getPluginRelativePath() == null)
						|| fPluginRelativePath.equals(other.getPluginRelativePath()) );
	}
	
	public int hashCode() {
		// TODO Auto-generated method stub
		return super.hashCode();
	}
}
