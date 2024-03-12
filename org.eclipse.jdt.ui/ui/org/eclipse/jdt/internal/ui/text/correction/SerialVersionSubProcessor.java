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

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.internal.corext.fix.IProposableFix;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;

/**
 * Subprocessor for serial version quickfix proposals.
 *
 * @since 3.1
 */
public final class SerialVersionSubProcessor extends SerialVersionBaseSubProcessor<ICommandAccess> {

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
	public static void getSerialVersionProposals(final IInvocationContext context, final IProblemLocation location, final Collection<ICommandAccess> proposals) {
		new SerialVersionSubProcessor().addSerialVersionProposals(context, location, proposals);
	}

	private SerialVersionSubProcessor() {
	}

	@Override
	protected ICommandAccess createSerialVersionProposal(IProposableFix iProposableFix, int missingSerialVersion, IInvocationContext context, boolean b) {
		return new SerialVersionProposal(iProposableFix, missingSerialVersion, context, b);
	}
}
