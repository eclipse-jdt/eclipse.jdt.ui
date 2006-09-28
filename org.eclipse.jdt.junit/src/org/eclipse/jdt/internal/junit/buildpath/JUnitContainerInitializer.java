/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.buildpath;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.ui.JUnitMessages;

public class JUnitContainerInitializer extends ClasspathContainerInitializer {
	
	public static final String JUNIT_CONTAINER_ID= "org.eclipse.jdt.junit.JUNIT_CONTAINER"; //$NON-NLS-1$
	
	/**
	 * @deprecated just for compatibility
	 */
	private final static String JUNIT3_8_1= "3.8.1"; //$NON-NLS-1$
	private final static String JUNIT3= "3"; //$NON-NLS-1$
	private final static String JUNIT4= "4"; //$NON-NLS-1$
	
	public final static IPath JUNIT3_PATH= new Path(JUNIT_CONTAINER_ID).append(JUNIT3);
	public final static IPath JUNIT4_PATH= new Path(JUNIT_CONTAINER_ID).append(JUNIT4);
	
	private static class JUnitContainer implements IClasspathContainer {
		
		private final IClasspathEntry[] fEntries;
		private final IPath fPath;

		public JUnitContainer(IPath path, IClasspathEntry[] entries) {
			fPath= path;
			fEntries= entries;
		}

		public IClasspathEntry[] getClasspathEntries() {
			return fEntries;
		}

		public String getDescription() {
			if (JUNIT4_PATH.equals(fPath)) {
				return JUnitMessages.JUnitContainerInitializer_description_junit4;
			}
			return JUnitMessages.JUnitContainerInitializer_description_junit3;
		}

		public int getKind() {
			return IClasspathContainer.K_APPLICATION;
		}

		public IPath getPath() {
			return fPath;
		}
		
	}
	
	
	public JUnitContainerInitializer() {
	}

	public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
		if (isValidJUnitContainerPath(containerPath)) {

			IClasspathEntry entry= null;
			String version= containerPath.segment(1);
			if (JUNIT3_8_1.equals(version) || JUNIT3.equals(version)) {
				entry= BuildPathSupport.getJUnit3LibraryEntry();
			} else if (JUNIT4.equals(version)) {
				entry= BuildPathSupport.getJUnit4LibraryEntry();
			}
			
			IClasspathEntry[] entries;
			if (entry != null) {
				entries= new IClasspathEntry[] { entry };
			} else {
				entries= new IClasspathEntry[] { };
			}
			
			JUnitContainer container= new JUnitContainer(containerPath, entries);
			JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { project }, 	new IClasspathContainer[] { container }, null);
		}

	}
	
	private static boolean isValidJUnitContainerPath(IPath path) {
		return path != null && path.segmentCount() == 2 && JUNIT_CONTAINER_ID.equals(path.segment(0));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.ClasspathContainerInitializer#canUpdateClasspathContainer(org.eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject)
	 */
	public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.ClasspathContainerInitializer#requestClasspathContainerUpdate(org.eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject, org.eclipse.jdt.core.IClasspathContainer)
	 */
	public void requestClasspathContainerUpdate(IPath containerPath, IJavaProject project, IClasspathContainer containerSuggestion) throws CoreException {
		JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { project }, 	new IClasspathContainer[] { containerSuggestion }, null);
	}

	/**
	 * @see org.eclipse.jdt.core.ClasspathContainerInitializer#getDescription(org.eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject)
	 */
	public String getDescription(IPath containerPath, IJavaProject project) {
		if (isValidJUnitContainerPath(containerPath)) {
			String version= containerPath.segment(1);
			if (JUNIT3_8_1.equals(version) || JUNIT3.equals(version)) {
				return JUnitMessages.JUnitContainerInitializer_description_initializer_junit3;
			} else if (JUNIT4.equals(version)) {
				return JUnitMessages.JUnitContainerInitializer_description_initializer_junit4;
			}
		}
		return JUnitMessages.JUnitContainerInitializer_description_initializer_unresolved;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.ClasspathContainerInitializer#getComparisonID(org.eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject)
	 */
	public Object getComparisonID(IPath containerPath, IJavaProject project) {
		return containerPath;
	}
	
}
