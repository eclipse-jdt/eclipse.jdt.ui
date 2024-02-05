/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposalCore;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.MissingReturnTypeCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.MissingReturnTypeCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.MissingReturnTypeInLambdaCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.MissingReturnTypeInLambdaCorrectionProposalCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ReplaceCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ReplaceCorrectionProposalCore;


public class ReturnTypeSubProcessor extends ReturnTypeBaseSubProcessor<ICommandAccess> {
	protected ReturnTypeSubProcessor() {
	}

	public static void addMethodWithConstrNameProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) {
		new ReturnTypeSubProcessor().collectMethodWithConstrNameProposals(context, problem, proposals);
	}

	public static void addVoidMethodReturnsProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) {
		new ReturnTypeSubProcessor().collectVoidMethodReturnsProposals(context, problem, proposals);
	}



	public static void addMissingReturnTypeProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) {
		new ReturnTypeSubProcessor().collectMissingReturnTypeProposals(context, problem, proposals);
	}

	public static void addMissingReturnStatementProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) {
		new ReturnTypeSubProcessor().collectMissingReturnStatementProposals(context, problem, proposals);
	}

	public static void replaceReturnWithYieldStatementProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) {
		new ReturnTypeSubProcessor().collectReplaceReturnWithYieldStatementProposals(context, problem, proposals);
	}

	public static void addMethodRetunsVoidProposals(IInvocationContext context, IProblemLocationCore problem, Collection<ICommandAccess> proposals) throws JavaModelException {
		new ReturnTypeSubProcessor().collectMethodReturnsVoidProposals(context, problem, proposals);
	}



	@Override
	protected TypeMismatchBaseSubProcessor<ICommandAccess> getTypeMismatchSubProcessor() {
		return new TypeMismatchSubProcessor();
	}

	@Override
	protected ICommandAccess linkedCorrectionProposal1ToT(LinkedCorrectionProposalCore proposal, int uid) {
		return new LinkedCorrectionProposal(proposal, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
	}

	@Override
	protected ICommandAccess rewriteCorrectionProposalToT(ASTRewriteCorrectionProposalCore p, int uid) {
		return new ASTRewriteCorrectionProposal(p, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
	}

	@Override
	protected ICommandAccess replaceCorrectionProposalToT(ReplaceCorrectionProposalCore core, int uid) {
		return new ReplaceCorrectionProposal(core);
	}

	@Override
	protected ICommandAccess missingReturnTypeProposalToT(MissingReturnTypeCorrectionProposalCore core, int uid) {
		return new MissingReturnTypeCorrectionProposal(core);
	}

	@Override
	protected ICommandAccess missingReturnTypeInLambdaProposalToT(MissingReturnTypeInLambdaCorrectionProposalCore core, int uid) {
		return new MissingReturnTypeInLambdaCorrectionProposal(core);
	}

}
