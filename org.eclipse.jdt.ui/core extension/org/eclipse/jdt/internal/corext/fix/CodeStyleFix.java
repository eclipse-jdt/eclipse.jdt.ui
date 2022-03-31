/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.fix.CleanUpFixWrapper;

/**
 * A fix which fixes code style issues.
 */
public class CodeStyleFix {

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit,
			boolean addThisQualifier,
			boolean changeNonStaticAccessToStatic,
			boolean qualifyStaticFieldAccess,
			boolean changeIndirectStaticAccessToDirect,
			boolean qualifyMethodAccess,
			boolean qualifyStaticMethodAccess,
			boolean removeFieldQualifier,
			boolean removeMethodQualifier) {

		return CleanUpFixWrapper.create(CodeStyleFixCore.createCleanUp(compilationUnit, addThisQualifier, changeNonStaticAccessToStatic, qualifyStaticFieldAccess,
				changeIndirectStaticAccessToDirect, qualifyMethodAccess, qualifyStaticMethodAccess, removeFieldQualifier, removeMethodQualifier));
	}

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit, IProblemLocation[] problems,
			boolean addThisQualifier,
			boolean changeNonStaticAccessToStatic,
			boolean changeIndirectStaticAccessToDirect) {

		return CleanUpFixWrapper.create(problems, problemLocations -> {
			return CodeStyleFixCore.createCleanUp(compilationUnit, problemLocations, addThisQualifier, changeNonStaticAccessToStatic, changeIndirectStaticAccessToDirect);
		});
	}

}
