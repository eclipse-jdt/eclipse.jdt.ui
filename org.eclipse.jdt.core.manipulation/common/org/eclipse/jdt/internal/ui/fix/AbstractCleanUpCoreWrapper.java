/*******************************************************************************
 * Copyright (c) 2022 Red Hat and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

/**
 * Base class for cleanups that forward their implementation to to a cleanup class
 * in the jdt.core.manipulation project.
 *
 * @param <T> The type of the cleanup this class forwards to.
 */
public class AbstractCleanUpCoreWrapper<T extends AbstractCleanUpCore> extends AbstractCleanUp {
	protected final T cleanUpCore;

	protected AbstractCleanUpCoreWrapper(Map<String, String> settings, T wrapped) {
		cleanUpCore= wrapped;
		setOptions(new MapCleanUpOptions(settings));
	}

	@Override
	public RefactoringStatus checkPreConditions(IJavaProject project, ICompilationUnit[] compilationUnits, IProgressMonitor monitor) throws CoreException {
		return cleanUpCore.checkPreConditions(project, compilationUnits, monitor);
	}

	@Override
	public final ICleanUpFix createFix(CleanUpContext context) throws CoreException {
		ICleanUpFixCore fix= cleanUpCore.createFixCore(context);
		return fix != null ? new CleanUpFixWrapper(fix) : null;
	}

	@Override
	public RefactoringStatus checkPostConditions(IProgressMonitor monitor) throws CoreException {
		return cleanUpCore.checkPostConditions(monitor);
	}

	@Override
	public void setOptions(CleanUpOptions options) {
		cleanUpCore.setOptions(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(cleanUpCore.getRequirementsCore());
	}

	@Override
	public String[] getStepDescriptions() {
		return cleanUpCore.getStepDescriptions();
	}

	@Override
	public String getPreview() {
		return cleanUpCore.getPreview();
	}
}
