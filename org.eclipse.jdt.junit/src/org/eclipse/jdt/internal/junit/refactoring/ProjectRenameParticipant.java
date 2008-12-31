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
package org.eclipse.jdt.internal.junit.refactoring;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IProject;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

public class ProjectRenameParticipant extends JUnitRenameParticipant {

	private IJavaProject fProject;

	private IJavaProject getNewJavaProject() {
		IProject project= fProject.getProject().getWorkspace().getRoot().getProject(getNewName());
		return getJavaProject(project);
	}

	protected IJavaProject getJavaProject(IProject project) {
		return JavaCore.create(project);
	}

	protected boolean initialize(Object element) {
		fProject= (IJavaProject) element;
		return true;
	}

	public void createChangeForConfig(JUnitRenameParticipant.ChangeList changeList, LaunchConfigurationContainer config) throws CoreException {

		changeList.addAttributeChangeIfNeeded(config, IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, fProject.getElementName(), getNewName());

		IJavaProject newJavaProject= getNewJavaProject();

		String container= config.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER, (String) null);
		IJavaElement javaElement= getJavaElement(container);
		if (javaElement == null)
			return;
		IJavaElement potentialMatch= javaElement;

		// TODO: spike!
		while (!potentialMatch.getHandleIdentifier().equals(fProject.getHandleIdentifier())) {
			potentialMatch= potentialMatch.getParent();
			if (potentialMatch == null)
				return;
		}

		String newHandle;
		if (javaElement instanceof IPackageFragment) {
			IPackageFragment fragment= (IPackageFragment) javaElement;
			newHandle= newJavaProject.getPackageFragmentRoot(fragment.getParent().getElementName()).getPackageFragment(fragment.getElementName()).getHandleIdentifier();
		} else if (javaElement instanceof IPackageFragmentRoot) {
			IPackageFragmentRoot root= (IPackageFragmentRoot) javaElement;
			newHandle= newJavaProject.getPackageFragmentRoot(root.getElementName()).getHandleIdentifier();
		} else if (javaElement instanceof IJavaProject) {
			newHandle= newJavaProject.getHandleIdentifier();
		} else {
			// shouldn't happen, but if it does, we silently fail.
			return;
		}
		changeList.addAttributeChange(config, JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER, newHandle);

		// rename change must come at the end
		changeList.addRenameChangeIfNeeded(config, fProject.getElementName());
	}

	protected IJavaElement getJavaElement(String handle) {
		return JavaCore.create(handle);
	}
}
