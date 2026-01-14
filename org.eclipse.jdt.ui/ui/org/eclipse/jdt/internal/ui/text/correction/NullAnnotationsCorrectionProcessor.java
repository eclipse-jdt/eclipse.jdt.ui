/*******************************************************************************
 * Copyright (c) 2011, 2026 GK Software AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - [quick fix] Add quick fixes for null annotations - https://bugs.eclipse.org/337977
 *     IBM Corporation - bug fixes
 *     IBM Corporation - extend core processor
 *     Red Hat Ltd. - refactor methods to NullAnnotationsCorrectionsProcessorCore
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.internal.corext.fix.NullAnnotationsRewriteOperations.ChangeKind;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CreatePackageInfoWithDefaultNullnessProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CreatePackageInfoWithDefaultNullnessProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ExtractToNullCheckedLocalProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ExtractToNullCheckedLocalProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.MakeLocalVariableNonNullProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.MakeLocalVariableNonNullProposalCore;

/**
 * Quick Fixes for null-annotation related problems.
 */
public class NullAnnotationsCorrectionProcessor extends NullAnnotationsCorrectionProcessorCore<ICommandAccess> {

	// pre: changeKind != OVERRIDDEN
	public static void addReturnAndArgumentTypeProposal(IInvocationContext context, IProblemLocation problem, ChangeKind changeKind,
			Collection<ICommandAccess> proposals) {
		new NullAnnotationsCorrectionProcessor().getReturnAndArgumentTypeProposal(context, problem, changeKind, proposals);
	}

	public static void addNullAnnotationInSignatureProposal(IInvocationContext context, IProblemLocation problem,
			Collection<ICommandAccess> proposals, ChangeKind changeKind, boolean isArgumentProblem) {
		new NullAnnotationsCorrectionProcessor().getNullAnnotationInSignatureProposal(context, problem, proposals, changeKind, isArgumentProblem);
	}

	public static void addRemoveRedundantAnnotationProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new NullAnnotationsCorrectionProcessor().getRemoveRedundantAnnotationProposal(context, problem, proposals);
	}

	public static void addAddMissingDefaultNullnessProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
		new NullAnnotationsCorrectionProcessor().getAddMissingDefaultNullnessProposal(context, problem, proposals);
	}

	/**
	 * Fix for {@link IProblem#NullableFieldReference}
	 * @param context context
	 * @param problem problem to be fixed
	 * @param proposals accumulator for computed proposals
	 */
	public static void addExtractCheckedLocalProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new NullAnnotationsCorrectionProcessor().getExtractCheckedLocalProposal(context, problem, proposals);
	}

	public static void addLocalVariableAnnotationProposal(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) {
		new NullAnnotationsCorrectionProcessor().getLocalVariableAnnotationProposal(context, problem, proposals);
	}

	private NullAnnotationsCorrectionProcessor() {
	}

	@Override
	protected ICommandAccess fixCorrectionProposalCoreToT(FixCorrectionProposalCore core, int uid) {
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		return new FixCorrectionProposal(core, image);
	}

	@Override
	protected ICommandAccess makeLocalVariableNonNullProposalCoreToT(MakeLocalVariableNonNullProposalCore core) {
		return new MakeLocalVariableNonNullProposal(core);
	}

	@Override
	protected ICommandAccess extractToNullCheckedLocalProposalCoreToT(ExtractToNullCheckedLocalProposalCore core, int uid) {
		return new ExtractToNullCheckedLocalProposal(core);
	}

	@Override
	protected ICommandAccess createPackageInfoWithDefaultNullnessProposalCoreToT(CreatePackageInfoWithDefaultNullnessProposalCore core, int uid) {
		return new CreatePackageInfoWithDefaultNullnessProposal(core);
	}
}
