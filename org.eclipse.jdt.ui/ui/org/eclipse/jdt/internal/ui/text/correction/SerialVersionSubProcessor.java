/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.IProposableFix;
import org.eclipse.jdt.internal.corext.fix.PotentialProgrammingProblemsFix;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.fix.PotentialProgrammingProblemsCleanUp;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;

/**
 * Subprocessor for serial version quickfix proposals.
 *
 * @since 3.1
 */
public final class SerialVersionSubProcessor {

	public static final class SerialVersionProposal extends FixCorrectionProposal {
		private boolean fIsDefaultProposal;

		public SerialVersionProposal(IProposableFix fix, int relevance, IInvocationContext context, boolean isDefault) {
			super(fix, createCleanUp(isDefault), relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD), context);
			fIsDefaultProposal= isDefault;
		}

		private static ICleanUp createCleanUp(boolean isDefault) {
			Map<String, String> options= new Hashtable<>();
			options.put(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID, CleanUpOptions.TRUE);
			if (isDefault) {
				options.put(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_DEFAULT, CleanUpOptions.TRUE);
			} else {
				options.put(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED, CleanUpOptions.TRUE);
			}
			return new PotentialProgrammingProblemsCleanUp(options);
		}

		public boolean isDefaultProposal() {
			return fIsDefaultProposal;
		}

		@Override
		public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
			if (fIsDefaultProposal) {
				return CorrectionMessages.SerialVersionDefaultProposal_message_default_info;
			} else {
				return CorrectionMessages.SerialVersionHashProposal_message_generated_info;
			}
		}
	}


	/**
	 * Determines the serial version quickfix proposals.
	 *
	 * @param context
	 *        the invocation context
	 * @param location
	 *        the problem location
	 * @param proposals
	 *        the proposal collection to extend
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
