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
 *     Red Hat Inc. - modified to wrapper core class
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.function.Function;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.IProblemLocationCore;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocationCore;

/**
 * Class to wrap an ICleanUpFixCore and be used as an ICleanUpFix
 *
 */
public class CleanUpFixWrapper implements ICleanUpFix {

	private ICleanUpFixCore cleanUpFixCore;

	public CleanUpFixWrapper(ICleanUpFixCore cleanUpFixCore) {
		this.cleanUpFixCore= cleanUpFixCore;
	}

	@Override
	public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {
		return cleanUpFixCore.createChange(progressMonitor);
	}

	public static CleanUpFixWrapper create(ICleanUpFixCore cleanUpFixCore) {
		return cleanUpFixCore == null ? null : new CleanUpFixWrapper(cleanUpFixCore);
	}

	public static ICleanUpFix create(IProblemLocation[] problems, Function<IProblemLocationCore[], ICleanUpFixCore> createFunction) {
		IProblemLocationCore[] problemLocationArray= null;
		if (problems != null) {
			problemLocationArray= new ProblemLocationCore[problems.length];
			for (int i= 0; i < problems.length; i++) {
				problemLocationArray[i]=  (ProblemLocation)problems[i];
			}
		}

		return CleanUpFixWrapper.create(createFunction.apply(problemLocationArray));
	}

}
