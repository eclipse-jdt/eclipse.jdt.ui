/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Apr 11, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.jdt.internal.junit.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;


class LaunchConfigTypeChange extends Change {

	private IType fType;
	private ILaunchConfiguration fConfig;
	private String fNewName;

	public LaunchConfigTypeChange(IType type, ILaunchConfiguration config, String newName) {
		fType= type;
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
		RefactoringStatus refactoringStatus= new RefactoringStatus();
		if (!fConfig.exists())
			refactoringStatus.addFatalError(JUnitMessages.getString("LaunchConfigTypeChange.configDeleted")); //$NON-NLS-1$
		return refactoringStatus;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IChange#perform(org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change perform(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 1); //$NON-NLS-1$
		String current= fConfig.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, (String)null);
		int index= current.lastIndexOf('.');
		String newTypeName;
		if (index == -1) {
			newTypeName= fNewName;
		} else {
			newTypeName= current.substring(0, index + 1) + fNewName;
		}
		ILaunchConfigurationWorkingCopy copy= fConfig.getWorkingCopy();
		copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, newTypeName);
		
		// generate the new configuration name
		String launchConfigurationName= fConfig.getName();
		
		if (launchConfigurationName.equals(current)) {
			if (!DebugPlugin.getDefault().getLaunchManager().isExistingLaunchConfigurationName(fNewName)) 
				copy.rename(fNewName);
		}
		copy.doSave();
		pm.worked(1);
		return new LaunchConfigTypeChange(fType, fConfig, (index == -1) ? current : current.substring(index + 1));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IChange#getModifiedLanguageElement()
	 */
	public Object getModifiedElement() {
		return fConfig;
	}
}