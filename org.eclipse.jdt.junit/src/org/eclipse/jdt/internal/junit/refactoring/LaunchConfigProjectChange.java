/*
 * Created on Apr 11, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.jdt.internal.junit.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;


class LaunchConfigProjectChange extends Change { 

	private ILaunchConfiguration fConfig;
	private String fNewName;

	public LaunchConfigProjectChange(ILaunchConfiguration config, String newName) {
		fConfig= config;
		fNewName= newName;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return fConfig.getName();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void initializeValidationData(IProgressMonitor pm) {
		// must be implemented to decide correct value of isValid
	}

	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		return new RefactoringStatus();
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IChange#perform(org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change perform(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		String oldProjectName= fConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String)null);

		ILaunchConfigurationWorkingCopy copy = fConfig.getWorkingCopy();
		copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, fNewName);
		copy.doSave();
		pm.worked(1);
		return new LaunchConfigProjectChange(fConfig, oldProjectName);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IChange#getModifiedLanguageElement()
	 */
	public Object getModifiedElement() {
		return fConfig;
	}
}