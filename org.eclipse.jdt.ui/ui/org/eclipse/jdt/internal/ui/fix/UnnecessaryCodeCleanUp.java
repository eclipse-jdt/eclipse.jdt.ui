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
 *     Red Hat Inc. - modified to use UnnecessaryCodeCleanUpCore
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.IProblemLocationCore;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

public class UnnecessaryCodeCleanUp extends AbstractMultiFix {

	private UnnecessaryCodeCleanUpCore cleanUpCore= new UnnecessaryCodeCleanUpCore();

	public UnnecessaryCodeCleanUp(Map<String, String> options) {
		super();
		setOptions(options);
	}

	public UnnecessaryCodeCleanUp() {
		super();
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
	protected ICleanUpFix createFix(CompilationUnit compilationUnit) throws CoreException {
		ICleanUpFixCore fix= cleanUpCore.createFix(compilationUnit);
		return fix == null ? null : new CleanUpFixWrapper(fix);
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
        IProblemLocationCore[] problemsCore= null;
		if (problems != null) {
			List<IProblemLocationCore> problemsList= new ArrayList<>();
			for (IProblemLocation problem : problems) {
				problemsList.add((ProblemLocation)problem);
			}
			problemsCore= problemsList.toArray(new IProblemLocationCore[0]);
		}
		ICleanUpFixCore fix= cleanUpCore.createFix(compilationUnit, problemsCore);
		return fix == null ? null : new CleanUpFixWrapper(fix);
	}

	@Override
	public String[] getStepDescriptions() {
		return cleanUpCore.getStepDescriptions();
	}

	@Override
	public String getPreview() {
		return cleanUpCore.getPreview();
	}

	@Override
	public boolean canFix(ICompilationUnit compilationUnit, IProblemLocation problem) {
		IProblemLocationCore problemLocation= (ProblemLocation)problem;
		return cleanUpCore.canFix(compilationUnit, problemLocation);
	}

	@Override
	public int computeNumberOfFixes(CompilationUnit compilationUnit) {
		return cleanUpCore.computeNumberOfFixes(compilationUnit);
	}
}
