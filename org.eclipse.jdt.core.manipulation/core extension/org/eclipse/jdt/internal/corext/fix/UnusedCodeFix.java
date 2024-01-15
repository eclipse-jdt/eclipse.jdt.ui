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
 *     Red Hat Inc. - modified to use UnusedCodeFixCore
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.ui.fix.UnusedCodeCleanUp;
import org.eclipse.jdt.internal.ui.fix.UnusedCodeCleanUpCore;
import org.eclipse.jdt.internal.ui.text.correction.IProblemLocationCore;

/**
 * Fix which removes unused code.
 */
public class UnusedCodeFix implements IProposableFix {

	protected UnusedCodeFixCore cleanUpFixCore;

	public UnusedCodeFix(UnusedCodeFixCore cleanUpFixCore) {
		this.cleanUpFixCore = cleanUpFixCore;
	}

	public UnusedCodeCleanUp getCleanUp() {
		UnusedCodeCleanUpCore cleanUp= cleanUpFixCore.getCleanUp();

		return cleanUp == null ? null : new UnusedCodeCleanUp(cleanUp);
	}

	@Override
	public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {
		return cleanUpFixCore.createChange(progressMonitor);
	}

	@Override
	public String getDisplayString() {
		return cleanUpFixCore.getDisplayString();
	}

	@Override
	public String getAdditionalProposalInfo() {
		return cleanUpFixCore.getAdditionalProposalInfo();
	}

	@Override
	public IStatus getStatus() {
		return cleanUpFixCore.getStatus();
	}

	public static UnusedCodeFix createRemoveUnusedImportFix(CompilationUnit compilationUnit, IProblemLocationCore problem) {
		return wrap(UnusedCodeFixCore.createRemoveUnusedImportFix(compilationUnit, problem));
	}

	private static UnusedCodeFix wrap(UnusedCodeFixCore coreFix) {
		return coreFix == null ? null : new UnusedCodeFix(coreFix);
	}

	public static UnusedCodeFix createUnusedMemberFix(CompilationUnit compilationUnit, IProblemLocationCore problem, boolean removeAllAssignements) {
		return wrap(UnusedCodeFixCore.createUnusedMemberFix(compilationUnit, problem, removeAllAssignements));
	}

	public static UnusedCodeFix createUnusedParameterFix(CompilationUnit compilationUnit, IProblemLocationCore problem) {
		return wrap(UnusedCodeFixCore.createUnusedParameterFix(compilationUnit, problem));
	}

	public static UnusedCodeFix createUnusedTypeParameterFix(CompilationUnit compilationUnit, IProblemLocationCore problemLoc) {
		return wrap(UnusedCodeFixCore.createUnusedTypeParameterFix(compilationUnit, problemLoc));
	}

	public static UnusedCodeFix createRemoveUnusedCastFix(CompilationUnit compilationUnit, IProblemLocationCore problem) {
		return wrap(UnusedCodeFixCore.createRemoveUnusedCastFix(compilationUnit, problem));
	}

}
