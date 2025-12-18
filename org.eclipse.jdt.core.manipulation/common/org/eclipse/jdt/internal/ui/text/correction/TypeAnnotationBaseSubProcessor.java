/*******************************************************************************
 * Copyright (c) 2025 Till Brychcy and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Till Brychcy - initial API and implementation
 *     IBM Corporation - refactored to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.internal.corext.fix.TypeAnnotationFixCore;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.fix.NullAnnotationsCleanUpCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposalCore;

public abstract class TypeAnnotationBaseSubProcessor<T> {

	protected int CORRECTION_CHANGE= 1;

	public void getMoveTypeAnnotationToTypeProposal(IInvocationContext context, IProblemLocation problem, Collection<T> proposals) {
		TypeAnnotationFixCore fix= TypeAnnotationFixCore.createMoveAnnotationsToTypeAnnotationsFix(context.getASTRoot(), problem);
		if (fix == null)
			return;

		Map<String, String> options= new Hashtable<>();
		FixCorrectionProposalCore proposal= new FixCorrectionProposalCore(fix, new NullAnnotationsCleanUpCore(options, problem.getProblemId()), IProposalRelevance.REMOVE_REDUNDANT_NULLNESS_ANNOTATION, context);
		proposals.add(fixCorrectionProposalCoreToT(proposal, CORRECTION_CHANGE));
	}

	public static boolean hasFixForProblem(int problemId) {
		switch (problemId) {
			case IProblem.TypeAnnotationAtQualifiedName:
			case IProblem.IllegalTypeAnnotationsInStaticMemberAccess:
			case IProblem.NullAnnotationAtQualifyingType:
			case IProblem.IllegalAnnotationForBaseType:
				return true;
			default:
				return false;
		}
	}

	protected abstract T fixCorrectionProposalCoreToT(FixCorrectionProposalCore core, int uid);

}
