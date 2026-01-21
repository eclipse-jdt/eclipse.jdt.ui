/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
 *     Red Hat Inc - separate core logic from UI images
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.Change;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.manipulation.ChangeCorrectionProposalCore;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.correction.ChangeCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Subprocessor for converting a type to record proposals.
 *
 * @since 3.1
 */
public final class ConvertRecordSubProcessor extends ConvertRecordBaseSubProcessor<ICommandAccess> {


	private ConvertRecordSubProcessor() {
	}

	/**
	 * Converts a type to a record proposal.
	 *
	 * @param context the invocation context
	 * @param proposals the proposal collection to extend
	 */
	public static boolean getConvertToRecordProposals(final IInvocationContext context, final ASTNode node, final Collection<ICommandAccess> proposals) {
		return new ConvertRecordSubProcessor().addConvertToRecordProposals(context, node, proposals);
	}

	@Override
	protected ICommandAccess changeCorrectionProposalToT(final ChangeCorrectionProposalCore proposal, int uid) {
		ChangeCorrectionProposal ret= new ChangeCorrectionProposal(proposal.getName(), null, proposal.getRelevance()) {
			@Override
			protected Change createChange() throws CoreException {
				return proposal.getChange();
			}
		};
		ret.setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
		return ret;
	}

}
