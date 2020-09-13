/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.jdt.ui.jarpackager;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.operation.IRunnableWithProgress;

/**
 * A runnable which executes a JAR export operation within the workspace.
 *
 * Clients may implement this interface.
 *
 * @see org.eclipse.jdt.ui.jarpackager.JarPackageData#createJarExportRunnable(org.eclipse.swt.widgets.Shell)
 * @see org.eclipse.core.resources.IWorkspaceRunnable
 * @since 2.0
 */
public interface IJarExportRunnable extends IRunnableWithProgress {

	/**
	 * Returns the current status of this operation.
	 * The result is a status object which may contain individual
	 * nested status objects.
	 * <p>
	 * Clients may call this method during the operation and add
	 * additional status information.
	 * </p>
	 *
	 * @return the status of this operation
	 */
	IStatus getStatus();
}
