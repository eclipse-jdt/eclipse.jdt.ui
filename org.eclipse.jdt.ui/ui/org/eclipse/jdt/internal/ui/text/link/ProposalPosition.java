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

import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposal;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

/**
 * 
 */
public class ProposalPosition extends TypedPosition {
	
	/** The choices available for this position, fChoices[0] is the original type. */
	private final ICompletionProposal[] fChoices;

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		if (o instanceof ProposalPosition) {
			if (super.equals(o)) {
				return Arrays.equals(fChoices, ((ProposalPosition)o).fChoices);
			}
		}
		return false;
	}
	
	/**
	 * @param offset
	 * @param length
	 * @param type
	 */
	public ProposalPosition(int offset, int length, String type, ICompletionProposal[] choices) {
		super(offset, length, type);
		fChoices= new ICompletionProposal[choices.length]; 
		System.arraycopy(choices, 0, fChoices, 0, choices.length);
	}
	
	/**
	 * 
	 * @return an array of choices, including the initial one. Clients must not modify it.
	 */
	public ICompletionProposal[] getChoices() {
		updateChoicePositions();
		return fChoices;
	}

	/**
	 * 
	 */
	private void updateChoicePositions() {
		for (int i= 0; i < fChoices.length; i++) {
			if (fChoices[i] instanceof JavaCompletionProposal)
				((JavaCompletionProposal)fChoices[i]).setReplacementOffset(offset);
		}
	}
}
