/*******************************************************************************
 * Copyright (c) 2016 Till Brychcy and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.internal.corext.fix.TypeAnnotationFix;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.fix.NullAnnotationsCleanUp;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;

public class TypeAnnotationSubProcessor {

	public static void addMoveTypeAnnotationToTypeProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		TypeAnnotationFix fix= TypeAnnotationFix.createMoveAnnotationsToTypeAnnotationsFix(context.getASTRoot(), problem);
		if (fix == null)
			return;

		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		Map<String, String> options= new Hashtable<>();
		FixCorrectionProposal proposal= new FixCorrectionProposal(fix, new NullAnnotationsCleanUp(options, problem.getProblemId()), IProposalRelevance.REMOVE_REDUNDANT_NULLNESS_ANNOTATION, image, context);
		proposals.add(proposal);
	}

	public static boolean hasFixFor(int problemId) {
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

	private TypeAnnotationSubProcessor() {
	}
}
