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
 *     Red Hat Inc. - modified to use CodeStyleCleanUpCore
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

/**
 * Creates fixes which can resolve code style issues
 * @see org.eclipse.jdt.internal.corext.fix.CodeStyleFix
 */
public class CodeStyleCleanUp extends AbstractMultiFix {

	private CodeStyleCleanUpCore coreCleanUp= new CodeStyleCleanUpCore();

	public CodeStyleCleanUp() {
	}

	public CodeStyleCleanUp(Map<String, String> options) {
		super();
		setOptions(options);
	}

	@Override
	public void setOptions(CleanUpOptions options) {
		coreCleanUp.setOptions(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(coreCleanUp.getRequirementsCore());
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit compilationUnit) throws CoreException {
		ICleanUpFixCore fix= coreCleanUp.createFix(compilationUnit);
		return fix == null ? null : new CleanUpFixWrapper(fix);
	}


	@Override
	protected ICleanUpFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
		IProblemLocationCore[] locations= null;
		if (problems != null) {
			List<IProblemLocationCore> problemsList= new ArrayList<>();
			for (IProblemLocation location : problems) {
				IProblemLocationCore problem= (ProblemLocation)location;
				problemsList.add(problem);
			}
			locations= problemsList.toArray(new IProblemLocationCore[0]);
		}
		ICleanUpFixCore fix= coreCleanUp.createFix(compilationUnit, locations);
		return fix == null ? null : new CleanUpFixWrapper(fix);
	}


	@Override
	public String[] getStepDescriptions() {
		return coreCleanUp.getStepDescriptions();
	}

	@Override
	public String getPreview() {
		return coreCleanUp.getPreview();
	}

	@Override
	public boolean canFix(ICompilationUnit compilationUnit, IProblemLocation problem) {
		ProblemLocation location= (ProblemLocation)problem;
		return coreCleanUp.canFix(compilationUnit, location);
	}

	@Override
	public int computeNumberOfFixes(CompilationUnit compilationUnit) {
		return coreCleanUp.computeNumberOfFixes(compilationUnit);
	}

}
