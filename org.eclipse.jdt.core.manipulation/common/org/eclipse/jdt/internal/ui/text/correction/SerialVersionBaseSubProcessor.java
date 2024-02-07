/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

import org.eclipse.core.runtime.Assert;

import org.eclipse.jdt.internal.corext.fix.IProposableFix;
import org.eclipse.jdt.internal.corext.fix.PotentialProgrammingProblemsFixCore;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * Subprocessor for serial version quickfix proposals.
 *
 * @since 3.1
 */
public abstract class SerialVersionBaseSubProcessor<T> {
	public SerialVersionBaseSubProcessor() {
	}

	/**
	 * Determines the serial version quickfix proposals.
	 *
	 * @param context the invocation context
	 * @param location the problem location
	 * @param proposals the proposal collection to extend
	 */
	public void addSerialVersionProposals(final IInvocationContext context, final IProblemLocation location, final Collection<T> proposals) {

		Assert.isNotNull(context);
		Assert.isNotNull(location);
		Assert.isNotNull(proposals);

		IProposableFix[] fixes= PotentialProgrammingProblemsFixCore.createMissingSerialVersionFixes(context.getASTRoot(), location);
		if (fixes != null) {
			T p1 = createSerialVersionProposal(fixes[0], IProposalRelevance.MISSING_SERIAL_VERSION_DEFAULT, context, true);
			T p2 = createSerialVersionProposal(fixes[1], IProposalRelevance.MISSING_SERIAL_VERSION, context, false);
			if (p1 != null)
				proposals.add(p1);
			if (p2 != null)
				proposals.add(p2);
		}
	}

	protected abstract T createSerialVersionProposal(IProposableFix iProposableFix, int relevance, IInvocationContext context, boolean isDefault);

}
