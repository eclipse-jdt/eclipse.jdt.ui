/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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



public interface ITestKind {
	static class NullTestKind extends TestKind {
		private NullTestKind() {
			super(null);
		}

		@Override
		public boolean isNull() {
			return true;
		}

		@Override
		protected String getAttribute(String arg0) {
			return null;
		}

		@Override
		public ITestFinder getFinder() {
			return ITestFinder.NULL;
		}

		@Override
		public JUnitRuntimeClasspathEntry[] getClasspathEntries() {
			return new JUnitRuntimeClasspathEntry[0];
		}

	}

	TestKind NULL = new NullTestKind();

	String ID = "id"; //$NON-NLS-1$
	String DISPLAY_NAME = "displayName"; //$NON-NLS-1$
	String FINDER_CLASS_NAME = "finderClass"; //$NON-NLS-1$
	String LOADER_PLUGIN_ID = "loaderPluginId"; //$NON-NLS-1$
	String LOADER_CLASS_NAME = "loaderClass"; //$NON-NLS-1$
	String PRECEDES = "precedesTestKind"; //$NON-NLS-1$

	String RUNTIME_CLASSPATH_ENTRY = "runtimeClasspathEntry"; //$NON-NLS-1$

	String CLASSPATH_PLUGIN_ID = "pluginId"; //$NON-NLS-1$
	String CLASSPATH_PATH_TO_JAR = "pathToJar"; //$NON-NLS-1$

	ITestFinder getFinder();

	String getId();
	String getDisplayName();
	String getFinderClassName();
	String getLoaderPluginId();
	String getLoaderClassName();
	String getPrecededKindId();


	boolean isNull();

	JUnitRuntimeClasspathEntry[] getClasspathEntries();

}