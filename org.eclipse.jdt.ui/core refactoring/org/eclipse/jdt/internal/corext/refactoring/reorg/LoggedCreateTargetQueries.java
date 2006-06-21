/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import org.eclipse.core.runtime.Assert;

/**
 * Logged implementation of new create target queries.
 * 
 * @since 3.3
 */
public final class LoggedCreateTargetQueries implements ICreateTargetQueries {

	/** Default implementation of create target query */
	private final class CreateTargetQuery implements ICreateTargetQuery {

		/**
		 * {@inheritDoc}
		 */
		public Object getCreatedTarget(final Object selection) {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		public String getNewButtonLabel() {
			return "unused"; //$NON-NLS-1$
		}
	}

	/** The java refactoring descriptor input handle to create */
	private final String fHandle;

	/** The project, or <code>null</code> for the workspace */
	private final String fProject;

	/**
	 * Creates a new logged create target queries.
	 * <p>
	 * The handle may be relative to the project or absolute.
	 * </p>
	 * 
	 * @param handle
	 *            the java refactoring descriptor input handle
	 */
	public LoggedCreateTargetQueries(final String project, final String handle) {
		Assert.isNotNull(project);
		Assert.isNotNull(handle);
		fHandle= handle;
		fProject= project;
	}

	/**
	 * {@inheritDoc}
	 */
	public ICreateTargetQuery createNewPackageQuery() {
		return new CreateTargetQuery();
	}
}