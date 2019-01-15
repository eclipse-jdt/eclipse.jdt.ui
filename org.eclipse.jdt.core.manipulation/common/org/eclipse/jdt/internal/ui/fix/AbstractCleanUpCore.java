/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
 *     Red Hat Inc. - copied and modified from AbstractCleanUp
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.manipulation.CleanUpContextCore;
import org.eclipse.jdt.core.manipulation.CleanUpOptionsCore;
import org.eclipse.jdt.core.manipulation.CleanUpRequirementsCore;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.fix.ICleanUpCore;

public abstract class AbstractCleanUpCore implements ICleanUpCore {

	private CleanUpOptionsCore fOptions;

	public AbstractCleanUpCore() {
	}

	public AbstractCleanUpCore(Map<String, String> settings) {
		setOptions(new MapCleanUpOptionsCore(settings));
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.fix.ICleanUpCore#setOptions(org.eclipse.jdt.ui.cleanup.CleanUpOptions)
	 */
	@Override
	public void setOptions(CleanUpOptionsCore options) {
		Assert.isLegal(options != null);
		fOptions= options;
	}


	/*
	 * @see org.eclipse.jdt.internal.corext.fix.ICleanUpCore#getStepDescriptions()
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
	 * @see org.eclipse.jdt.internal.corext.fix.ICleanUpCore#getRequirementsCore()
	 */
	@Override
	public CleanUpRequirementsCore getRequirementsCore() {
		return new CleanUpRequirementsCore(false, false, false, null);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.fix.ICleanUpCore#checkPreConditions(org.eclipse.jdt.core.IJavaProject, org.eclipse.jdt.core.ICompilationUnit[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public RefactoringStatus checkPreConditions(IJavaProject project, ICompilationUnit[] compilationUnits, IProgressMonitor monitor) throws CoreException {
		return new RefactoringStatus();
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.fix.ICleanUpCore#createFixCore(org.eclipse.jdt.ui.cleanup.CleanUpContext)
	 */
	@Override
	public ICleanUpFixCore createFixCore(CleanUpContextCore context) throws CoreException {
		return null;
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.fix.ICleanUpCore#checkPostConditions(org.eclipse.core.runtime.IProgressMonitor)
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
