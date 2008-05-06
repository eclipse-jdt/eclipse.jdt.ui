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
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.internal.junit.BasicElementLabels;
import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;

public abstract class LaunchConfigChange extends Change {

	protected LaunchConfigurationContainer fConfig;

	private final boolean fShouldFlagWarning;

	public LaunchConfigChange(LaunchConfigurationContainer config, boolean shouldFlagWarning) {
		fConfig= config;
		fShouldFlagWarning= shouldFlagWarning;
	}

	/**
	 * {@inheritDoc}
	 */
	public Object getModifiedElement() {
		return fConfig;
	}

	/**
	 * {@inheritDoc}
	 */
	public void initializeValidationData(IProgressMonitor pm) {
		// must be implemented to decide correct value of isValid
	}

	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		RefactoringStatus refactoringStatus= new RefactoringStatus();
		if (!fConfig.getConfiguration().exists() && fShouldFlagWarning)
			refactoringStatus.addError(Messages.format(JUnitMessages.LaunchConfigChange_configDeleted,  BasicElementLabels.getJavaElementName(fConfig.getName())));
		return refactoringStatus;
	}

	public Change perform(IProgressMonitor pm) throws CoreException {
		if (!fConfig.getConfiguration().exists())
			return new NullChange();

		pm.beginTask("", 1); //$NON-NLS-1$
		String oldValue= getOldValue(fConfig.getConfiguration());

		ILaunchConfigurationWorkingCopy copy= fConfig.getConfiguration().getWorkingCopy();
		alterLaunchConfiguration(copy);
		fConfig.setConfiguration(copy.doSave());

		Change undo= getUndo(oldValue);

		pm.worked(1);
		return undo;
	}

	public boolean shouldFlagWarning() {
		return fShouldFlagWarning;
	}

	protected abstract void alterLaunchConfiguration(ILaunchConfigurationWorkingCopy copy) throws CoreException;

	protected abstract String getOldValue(ILaunchConfiguration config) throws CoreException;

	protected abstract Change getUndo(String oldValue) throws CoreException;
}
