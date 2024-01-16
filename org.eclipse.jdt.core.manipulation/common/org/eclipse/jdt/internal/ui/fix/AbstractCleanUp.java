/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.fix;

import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public abstract class AbstractCleanUp implements ICleanUp {

	private CleanUpOptions fOptions;

	protected AbstractCleanUp() {
	}

	protected AbstractCleanUp(Map<String, String> settings) {
		setOptions(new MapCleanUpOptions(settings));
	}

	/**
	 * Sets the options that will be used.
	 *
	 * @param settings Map of setting keys to values
	 * @since 3.19
	 */
	public void setOptions(Map<String, String> settings) {
		setOptions(new MapCleanUpOptions(settings));
	}

	/*
	 * @see org.eclipse.jdt.ui.cleanup.ICleanUp#setOptions(org.eclipse.jdt.ui.cleanup.CleanUpOptions)
	 * @since 3.5
	 */
	@Override
	public void setOptions(CleanUpOptions options) {
		Assert.isLegal(options != null);
		fOptions= options;
	}


	/*
	 * @see org.eclipse.jdt.ui.cleanup.ICleanUp#getStepDescriptions()
	 * @since 3.5
	 */
	@Override
	public String[] getStepDescriptions() {
		return new String[0];
	}

	/**
	 * @return code snippet complying to current options
	 */
	public String getPreview() {
		return ""; //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.jdt.ui.cleanup.ICleanUp#getRequirements()
	 * @since 3.5
	 */
	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(false, false, false, null);
	}

	/*
	 * @see org.eclipse.jdt.ui.cleanup.ICleanUp#checkPreConditions(org.eclipse.jdt.core.IJavaProject, org.eclipse.jdt.core.ICompilationUnit[], org.eclipse.core.runtime.IProgressMonitor)
	 * @since 3.5
	 */
	@Override
	public RefactoringStatus checkPreConditions(IJavaProject project, ICompilationUnit[] compilationUnits, IProgressMonitor monitor) throws CoreException {
		return new RefactoringStatus();
	}

	/*
	 * @see org.eclipse.jdt.ui.cleanup.ICleanUp#createFix(org.eclipse.jdt.ui.cleanup.CleanUpContext)
	 * @since 3.5
	 */
	@Override
	public ICleanUpFix createFix(CleanUpContext context) throws CoreException {
		return null;
	}

	/*
	 * @see org.eclipse.jdt.ui.cleanup.ICleanUp#checkPostConditions(org.eclipse.core.runtime.IProgressMonitor)
	 * @since 3.5
	 */
	@Override
	public RefactoringStatus checkPostConditions(IProgressMonitor monitor) throws CoreException {
		return new RefactoringStatus();
	}

	/**
	 * @param key the name of the option
	 * @return <code>true</code> if option with <code>key</code> is enabled
	 */
	protected boolean isEnabled(String key) {
		Assert.isNotNull(fOptions);
		Assert.isLegal(key != null);
		return fOptions.isEnabled(key);
	}

}
