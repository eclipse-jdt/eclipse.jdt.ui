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
 *     Red Hat Inc. - copied from IMultiFix and modified for use in jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;


import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.CleanUpContextCore;

import org.eclipse.jdt.internal.corext.fix.ICleanUpCore;

import org.eclipse.jdt.internal.ui.text.correction.IProblemLocationCore;

public interface IMultiFixCore extends ICleanUpCore {

	public static class MultiFixContext extends CleanUpContextCore {

		private final IProblemLocationCore[] fLocations;

		public MultiFixContext(ICompilationUnit unit, CompilationUnit ast, IProblemLocationCore[] locations) {
			super(unit, ast);
			fLocations= locations;
		}

		/**
		 * @return locations of problems to fix.
		 */
		public IProblemLocationCore[] getProblemLocations() {
			return fLocations;
		}
	}

	/**
	 * True if <code>problem</code> in <code>ICompilationUnit</code> can be
	 * fixed by this CleanUp.
	 * <p>
	 * <strong>This must be a fast operation, the result can be a guess.</strong>
	 * </p>
	 *
	 * @param compilationUnit
	 *            The compilation unit to fix not null
	 * @param problem
	 *            The location of the problem to fix
	 * @return True if problem can be fixed
	 */
	boolean canFix(ICompilationUnit compilationUnit, IProblemLocationCore problem);

	/**
	 * Maximal number of problems this clean up will fix in compilation unit.
	 * There may be less then the returned number but never more.
	 *
	 * @param compilationUnit
	 *            The compilation unit to fix, not null
	 * @return The maximal number of fixes or -1 if unknown.
	 */
	int computeNumberOfFixes(CompilationUnit compilationUnit);

}
