/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
package org.eclipse.jdt.ui.cleanup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

/**
 * A clean up fix calculates a {@link CompilationUnitChange} which can be applied on a document to
 * fix one or more problems in a compilation unit.
 * Originally from org.eclipse.jdt.ui 3.5
 *
 * @since 1.21
 */
@SuppressWarnings({ "removal" })
public interface ICleanUpFix extends ICleanUpFixCore {

	/**
	 * Calculates and returns a {@link CompilationUnitChange} which can be applied on a document to
	 * fix one or more problems in a compilation unit.
	 *
	 * @param progressMonitor the progress monitor or <code>null</code> if none
	 * @return a compilation unit change change which should not be empty
	 * @throws CoreException if something went wrong while calculating the change
	 */
	@Override
	CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException;

}
