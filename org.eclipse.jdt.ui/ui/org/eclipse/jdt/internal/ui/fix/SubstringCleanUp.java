/*******************************************************************************
 * Copyright (c) 2021, 2022 Fabrice TIERCELIN and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fabrice TIERCELIN - initial API and implementation
 *     Red Hat - moved implementation to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Collections;
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
 * A fix that removes the second <code>substring()</code> parameter if this parameter is the length of the string:
 * <ul>
 * <li>It must reference the same expression,</li>
 * <li>The expression must be passive.</li>
 * </ul>
 */
public class SubstringCleanUp extends AbstractMultiFix {

	private final SubstringCleanUpCore cleanUpCore;

	public SubstringCleanUp(final Map<String, String> options) {
		this.cleanUpCore= new SubstringCleanUpCore();
		setOptions(options);
	}

	public SubstringCleanUp() {
		this(Collections.EMPTY_MAP);
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
