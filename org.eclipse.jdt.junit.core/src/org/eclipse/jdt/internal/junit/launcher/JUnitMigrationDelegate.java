package org.eclipse.jdt.internal.junit.launcher;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationMigrationDelegate;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

public class JUnitMigrationDelegate implements ILaunchConfigurationMigrationDelegate {

	protected static final String EMPTY_STRING= ""; //$NON-NLS-1$

	public JUnitMigrationDelegate() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.core.ILaunchConfigurationMigrationDelegate#isCandidate()
	 */
	public boolean isCandidate(ILaunchConfiguration candidate) throws CoreException {
		if (candidate.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null) == null) {
			return false;
		}
		IResource[] mappedResources= candidate.getMappedResources();
		if (mappedResources != null && mappedResources.length > 0) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.core.ILaunchConfigurationMigrationDelegate#migrate(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void migrate(ILaunchConfiguration candidate) throws CoreException {
		String projectName= candidate.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, EMPTY_STRING);
		IProject project= ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		ILaunchConfigurationWorkingCopy wc= candidate.getWorkingCopy();
		wc.setMappedResources(new IProject[] { project });
		wc.doSave();
	}

}
