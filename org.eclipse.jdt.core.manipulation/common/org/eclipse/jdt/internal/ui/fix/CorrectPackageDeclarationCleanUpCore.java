/*******************************************************************************
 * Copyright (c) 2025 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.CorrectPackageDeclarationFixCore;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

public class CorrectPackageDeclarationCleanUpCore extends AbstractMultiFix {

	@Override
	public boolean canFix(ICompilationUnit compilationUnit, IProblemLocation problem) {
		if (problem.getProblemId() == IProblem.PackageIsNotExpectedPackage) {
			return true;
		}
		return false;
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		IProblem[] problems= unit.getProblems();
		for (IProblem problem : problems) {
			ProblemLocation location= new ProblemLocation(problem);
			if (location.getProblemId() == IProblem.PackageIsNotExpectedPackage) {
				return createFix(unit, new IProblemLocation[] { location });
			}
		}
		return null;
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit, IProblemLocation[] problems) throws CoreException {
		for (IProblemLocation problem : problems) {
			if (problem.getProblemId() == IProblem.PackageIsNotExpectedPackage) {
				return CorrectPackageDeclarationFixCore.create(unit, problem);
			}
		}
		return null;
	}

	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(true, false, false, null);
	}
}
