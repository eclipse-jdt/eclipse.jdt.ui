/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.fix;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * Create fixes which can remove unused code
 * @see org.eclipse.jdt.internal.corext.fix.UnusedCodeFix
 */
public class UnusedCodeCleanUp extends AbstractMultiFix {

	private final UnusedCodeCleanUpCore cleanUpCore;

	public UnusedCodeCleanUp(UnusedCodeCleanUpCore cleanupCore) {
		this.cleanUpCore= cleanupCore;
	}

	public UnusedCodeCleanUp(Map<String, String> options) {
		this(new UnusedCodeCleanUpCore(options));
	}

	public UnusedCodeCleanUp() {
		this(Collections.EMPTY_MAP);
	}

	@Override
	public void setOptions(CleanUpOptions options) {
		cleanUpCore.setOptions(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		return cleanUpCore.getRequirements();
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit compilationUnit) throws CoreException {
		return cleanUpCore.createFix(compilationUnit);
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit compilationUnit, IProblemLocation[] problems) throws CoreException {
        IProblemLocation[] problemsCore= null;
		if (problems != null) {
			List<IProblemLocation> problemsList= new ArrayList<>();
			for (IProblemLocation problem : problems) {
				problemsList.add(problem);
			}
			problemsCore= problemsList.toArray(new IProblemLocation[0]);
		}
		ICleanUpFix fix= cleanUpCore.createFix(compilationUnit, problemsCore);
		return fix;
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
		IProblemLocation problemLocation= problem;
		return cleanUpCore.canFix(compilationUnit, problemLocation);
	}

	@Override
	public int computeNumberOfFixes(CompilationUnit compilationUnit) {
		return cleanUpCore.computeNumberOfFixes(compilationUnit);
	}
}
