/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	public static class MyClasspathContainer implements IClasspathContainer {

		private final IPath fPath;
		private static final IPath MY_ARCHIVE= new Path("C:\\xy.jar");

		public MyClasspathContainer(IPath path) {
			fPath= path;
		}

		public IClasspathEntry[] getClasspathEntries() {
			return new IClasspathEntry[] { JavaCore.newLibraryEntry(MY_ARCHIVE, null, null) };
		}

		public String getDescription() { return "My example"; }

		public int getKind() { return IClasspathContainer.K_APPLICATION; }

		public IPath getPath() { return fPath; }

	}

	public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
		IClasspathContainer[] containers= { new MyClasspathContainer(containerPath) };
		JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { project }, containers, null);
	}

}
