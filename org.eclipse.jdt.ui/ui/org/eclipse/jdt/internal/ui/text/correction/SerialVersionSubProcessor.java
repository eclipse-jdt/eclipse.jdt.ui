/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.internal.corext.fix.IProposableFix;
import org.eclipse.jdt.internal.corext.fix.PotentialProgrammingProblemsFix;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;

/**
 * Subprocessor for serial version quickfix proposals.
 *
 * @since 3.1
 */
public final class SerialVersionSubProcessor {

	public static final class SerialVersionProposal extends FixCorrectionProposal {
		public SerialVersionProposal(IProposableFix fix, int relevance, IInvocationContext context, boolean isDefault) {
			super(fix, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD), context, new SerialVersionProposalCore(fix, relevance, context, isDefault));
		}

		public boolean isDefaultProposal() {
			return ((SerialVersionProposalCore) getDelegate()).isDefaultProposal();
		}

		@Override
		public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
			return ((SerialVersionProposalCore) getDelegate()).getAdditionalProposalInfo(monitor);
		}
	}

	/**
	 * Determines the serial version quickfix proposals.
	 *
	 * @param context the invocation context
	 * @param location the problem location
	 * @param proposals the proposal collection to extend
	 */
	public static void getSerialVersionProposals(final IInvocationContext context, final IProblemLocationCore location, final Collection<ICommandAccess> proposals) {

		Assert.isNotNull(context);
		Assert.isNotNull(location);
		Assert.isNotNull(proposals);

		IProposableFix[] fixes= PotentialProgrammingProblemsFix.createMissingSerialVersionFixes(context.getASTRoot(), location);
		if (fixes != null) {
			proposals.add(new SerialVersionProposal(fixes[0], IProposalRelevance.MISSING_SERIAL_VERSION_DEFAULT, context, true));
			proposals.add(new SerialVersionProposal(fixes[1], IProposalRelevance.MISSING_SERIAL_VERSION, context, false));
		}
	}

	private SerialVersionSubProcessor() {
	}
}
