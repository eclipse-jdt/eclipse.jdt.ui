/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.buildpath;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.ui.JUnitPreferencesConstants;

public class JUnitContainerInitializer extends ClasspathContainerInitializer {

	public static final String JUNIT_CONTAINER_ID= "org.eclipse.jdt.junit.JUNIT_CONTAINER"; //$NON-NLS-1$

	private static final IStatus NOT_SUPPORTED= new Status(IStatus.ERROR, JUnitPlugin.PLUGIN_ID, ClasspathContainerInitializer.ATTRIBUTE_NOT_SUPPORTED, new String(), null);
	private static final IStatus READ_ONLY= new Status(IStatus.ERROR, JUnitPlugin.PLUGIN_ID, ClasspathContainerInitializer.ATTRIBUTE_READ_ONLY, new String(), null);

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
			JUnitContainer container= getNewContainer(containerPath);
			JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { project }, 	new IClasspathContainer[] { container }, null);
		}

	}

	private static JUnitContainer getNewContainer(IPath containerPath) {
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
		return new JUnitContainer(containerPath, entries);
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
	 * @see org.eclipse.jdt.core.ClasspathContainerInitializer#getAccessRulesStatus(org.eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject)
	 */
	public IStatus getAccessRulesStatus(IPath containerPath, IJavaProject project) {
		return NOT_SUPPORTED;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.ClasspathContainerInitializer#getSourceAttachmentStatus(org.eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject)
	 */
	public IStatus getSourceAttachmentStatus(IPath containerPath, IJavaProject project) {
		return READ_ONLY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.ClasspathContainerInitializer#getAttributeStatus(org.eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject, java.lang.String)
	 */
	public IStatus getAttributeStatus(IPath containerPath, IJavaProject project, String attributeKey) {
		if (attributeKey.equals(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME)) {
			return Status.OK_STATUS;
		}
		return NOT_SUPPORTED;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.ClasspathContainerInitializer#requestClasspathContainerUpdate(org.eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject, org.eclipse.jdt.core.IClasspathContainer)
	 */
	public void requestClasspathContainerUpdate(IPath containerPath, IJavaProject project, IClasspathContainer containerSuggestion) throws CoreException {
		IClasspathEntry[] entries= containerSuggestion.getClasspathEntries();
		if (entries.length == 1 && isValidJUnitContainerPath(containerPath)) {
			String version= containerPath.segment(1);

			// only modifiable entry in Javadoc location
			IClasspathAttribute[] extraAttributes= entries[0].getExtraAttributes();
			for (int i= 0; i < extraAttributes.length; i++) {
				IClasspathAttribute attrib= extraAttributes[i];
				if (attrib.getName().equals(IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME)) {

					IPreferenceStore preferenceStore= JUnitPlugin.getDefault().getPreferenceStore();
					if (JUNIT3.equals(version)) {
						preferenceStore.setValue(JUnitPreferencesConstants.JUNIT3_JAVADOC, attrib.getValue());
					} else if (JUNIT4.equals(version)) {
						preferenceStore.setValue(JUnitPreferencesConstants.JUNIT4_JAVADOC, attrib.getValue());
					}
					break;
				}
			}
			rebindClasspathEntries(project.getJavaModel(), containerPath);
		}
	}

	private static void rebindClasspathEntries(IJavaModel model, IPath containerPath) throws JavaModelException {
		ArrayList affectedProjects= new ArrayList();

		IJavaProject[] projects= model.getJavaProjects();
		for (int i= 0; i < projects.length; i++) {
			IJavaProject project= projects[i];
			IClasspathEntry[] entries= project.getRawClasspath();
			for (int k= 0; k < entries.length; k++) {
				IClasspathEntry curr= entries[k];
				if (curr.getEntryKind() == IClasspathEntry.CPE_CONTAINER && containerPath.equals(curr.getPath())) {
					affectedProjects.add(project);
				}
			}
		}
		if (!affectedProjects.isEmpty()) {
			IJavaProject[] affected= (IJavaProject[]) affectedProjects.toArray(new IJavaProject[affectedProjects.size()]);
			IClasspathContainer[] containers= new IClasspathContainer[affected.length];
			for (int i= 0; i < containers.length; i++) {
				containers[i]= getNewContainer(containerPath);
			}
			JavaCore.setClasspathContainer(containerPath, affected, containers, null);
		}
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
