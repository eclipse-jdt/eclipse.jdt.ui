/*******************************************************************************
 * Copyright (c) 2016, 2025 Till Brychcy and others.
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
 *     IBM Corporation - changed to use refactored base subprocessor
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposalCore;

public class TypeAnnotationSubProcessor extends TypeAnnotationBaseSubProcessor<ICommandAccess> {

	public static void addMoveTypeAnnotationToTypeProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new TypeAnnotationSubProcessor().getMoveTypeAnnotationToTypeProposal(context, problem, proposals);
	}

	public static boolean hasFixFor(int problemId) {
		return TypeAnnotationBaseSubProcessor.hasFixForProblem(problemId);
	}

	private TypeAnnotationSubProcessor() {
	}

	@Override
	protected ICommandAccess fixCorrectionProposalCoreToT(FixCorrectionProposalCore core, int uid) {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		return new FixCorrectionProposal(core, image);
	}
}
