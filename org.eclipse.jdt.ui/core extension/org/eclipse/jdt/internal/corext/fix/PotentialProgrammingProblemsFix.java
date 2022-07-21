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
 *     Red Hat Inc. - modified to use PotentialProgrammingProblemsFixCore
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.IProblemLocationCore;


public class PotentialProgrammingProblemsFix {

	public static IProposableFix[] createMissingSerialVersionFixes(CompilationUnit compilationUnit, IProblemLocation problem) {
		return PotentialProgrammingProblemsFixCore.createMissingSerialVersionFixes(compilationUnit, (IProblemLocationCore) problem);
	}

}