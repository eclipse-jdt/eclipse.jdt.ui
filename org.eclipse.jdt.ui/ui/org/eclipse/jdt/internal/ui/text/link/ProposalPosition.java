/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.link;

import java.util.Arrays;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposal;

/**
 * LinkedPosition with added completion proposals.
 * 
 * @since 3.0
 */
class ProposalPosition extends LinkedPosition {

	/**
	 * Da proposals
	 */
	private ICompletionProposal[] fProposals;

	/**
	 * @param document
	 * @param offset
	 * @param length
	 * @param sequence
	 * @param proposals
	 */
	public ProposalPosition(IDocument document, int offset, int length, int sequence, ICompletionProposal[] proposals) {
		super(document, offset, length, sequence);
		fProposals= proposals;
	}
	
	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		if (o instanceof ProposalPosition) {
			if (super.equals(o)) {
				return Arrays.equals(fProposals, ((ProposalPosition)o).fProposals);
			}
		}
		return false;
	}

	/**
	 * @return an array of choices, including the initial one. Clients must not modify it.
	 */
	public ICompletionProposal[] getChoices() {
		updateChoicePositions();
		return fProposals;
	}

	private void updateChoicePositions() {
		for (int i= 0; i < fProposals.length; i++) {
			if (fProposals[i] instanceof JavaCompletionProposal) {
				((JavaCompletionProposal)fProposals[i]).setReplacementOffset(offset);
			}
		}
	}
}
