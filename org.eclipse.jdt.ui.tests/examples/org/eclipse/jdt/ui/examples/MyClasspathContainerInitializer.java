/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
package org.eclipse.jdt.ui.examples;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

/**
 *
 */
public class MyClasspathContainerInitializer extends ClasspathContainerInitializer {

	public static ClasspathContainerInitializer initializerDelegate;

	public static final String CONTAINER_NAME= "org.eclipse.jdt.EXAMPLE_CONTAINER";

	public static void setInitializer(ClasspathContainerInitializer initializer) {
		initializerDelegate = initializer;
	}

	public static class MyClasspathContainer implements IClasspathContainer {

		private final IPath fPath;
		private static final IPath MY_ARCHIVE= new Path("C:\\xy.jar");

		public MyClasspathContainer(IPath path) {
			fPath= path;
		}

		@Override
		public IClasspathEntry[] getClasspathEntries() {
			return new IClasspathEntry[] { JavaCore.newLibraryEntry(MY_ARCHIVE, null, null) };
		}

		@Override
		public String getDescription() { return "My example"; }

		@Override
		public int getKind() { return IClasspathContainer.K_APPLICATION; }

		@Override
		public IPath getPath() { return fPath; }

	}

	@Override
	public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
		if (initializerDelegate != null) {
			initializerDelegate.initialize(containerPath, project);
		} else {
			IClasspathContainer[] containers= { new MyClasspathContainer(containerPath) };
			JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { project }, containers, null);
		}
	}
}
