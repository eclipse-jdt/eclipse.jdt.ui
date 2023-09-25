/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
 *     Red Hat Inc. - body copied from SerialVersionProposal
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.manipulation.CleanUpOptionsCore;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.ICleanUpCore;
import org.eclipse.jdt.internal.corext.fix.IProposableFix;

import org.eclipse.jdt.internal.ui.fix.PotentialProgrammingProblemsCleanUpCore;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposalCore;

public class SerialVersionProposalCore extends FixCorrectionProposalCore {
	private boolean fIsDefaultProposal;
	public SerialVersionProposalCore(IProposableFix fix, int relevance, IInvocationContextCore context, boolean isDefault) {
		super(fix, createCleanUp(isDefault), relevance, context);
		fIsDefaultProposal= isDefault;
	}

	private static ICleanUpCore createCleanUp(boolean isDefault) {
		Map<String, String> options= new Hashtable<>();
		options.put(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID, CleanUpOptionsCore.TRUE);
		if (isDefault) {
			options.put(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_DEFAULT, CleanUpOptionsCore.TRUE);
		} else {
			options.put(CleanUpConstants.ADD_MISSING_SERIAL_VERSION_ID_GENERATED, CleanUpOptionsCore.TRUE);
		}
		return new PotentialProgrammingProblemsCleanUpCore(options);
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