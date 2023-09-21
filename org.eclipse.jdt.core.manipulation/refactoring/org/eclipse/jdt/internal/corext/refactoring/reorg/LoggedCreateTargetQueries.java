/*******************************************************************************
 * Copyright (c) 2006, 2023 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.ui.util.JavaProjectUtilities;
import org.eclipse.jdt.internal.ui.util.ResourcesUtility;

/**
 * Logged implementation of new create target queries.
 *
 * @since 3.3
 */
public final class LoggedCreateTargetQueries implements ICreateTargetQueries {

	/** Default implementation of create target query */
	private final class CreateTargetQuery implements ICreateTargetQuery {

		private void createJavaProject(IProject project) throws CoreException {
			JavaProjectUtilities.createJavaProject(project);
		}

		private void createPackageFragmentRoot(IPackageFragmentRoot root) throws CoreException {
			JavaProjectUtilities.createPackageFragmentRoot(root);
		}

		@Override
		public Object getCreatedTarget(final Object selection) {
			final Object target= fLog.getCreatedElement(selection);
			if (target instanceof IPackageFragment) {
				final IPackageFragment fragment= (IPackageFragment) target;
				final IJavaElement parent= fragment.getParent();
				if (parent instanceof IPackageFragmentRoot) {
					try {
						final IPackageFragmentRoot root= (IPackageFragmentRoot) parent;
						if (!root.exists())
							createPackageFragmentRoot(root);
						if (!fragment.exists())
							root.createPackageFragment(fragment.getElementName(), true, new NullProgressMonitor());
					} catch (CoreException exception) {
						JavaManipulationPlugin.log(exception);
						return null;
					}
				}
			} else if (target instanceof IFolder) {
				try {
					final IFolder folder= (IFolder) target;
					final IProject project= folder.getProject();
					if (!project.exists())
						createJavaProject(project);
					if (!folder.exists())
						ResourcesUtility.createFolder(folder, true, true, new NullProgressMonitor());
				} catch (CoreException exception) {
					JavaManipulationPlugin.log(exception);
					return null;
				}
			}
			return target;
		}

		@Override
		public String getNewButtonLabel() {
			return "unused"; //$NON-NLS-1$
		}
	}

	/** The create target execution log */
	private final CreateTargetExecutionLog fLog;

	/**
	 * Creates a new logged create target queries.
	 *
	 * @param log
	 *            the create target execution log
	 */
	public LoggedCreateTargetQueries(final CreateTargetExecutionLog log) {
		Assert.isNotNull(log);
		fLog= log;
	}

	@Override
	public ICreateTargetQuery createNewPackageQuery() {
		return new CreateTargetQuery();
	}

	/**
	 * Returns the create target execution log.
	 *
	 * @return the create target execution log
	 */
	public CreateTargetExecutionLog getCreateTargetExecutionLog() {
		return fLog;
	}
}