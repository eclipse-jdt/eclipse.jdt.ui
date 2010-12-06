/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.propertiesfileeditor;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Proposal to escape or unescape backslashes.
 * 
 * @since 3.7
 */
public class EscapeBackslashCompletionProposal implements ICompletionProposal {

	private final String fProposalText;

	private final int fOffset;

	private final int fLength;

	private final boolean fEscapeBackslashes;

	/**
	 * Constructor for EscapeBackslashCompletionProposal.
	 * 
	 * @param proposalText the proposal text
	 * @param offset offset of the proposal
	 * @param length length of the proposal
	 * @param escapeBackslashes if <code>true</code> create 'Escape backslashes' proposal, otherwise
	 *            create 'Unescape backslashes' proposal
	 */
	public EscapeBackslashCompletionProposal(String proposalText, int offset, int length, boolean escapeBackslashes) {
		fProposalText= proposalText;
		fOffset= offset;
		fLength= length;
		fEscapeBackslashes= escapeBackslashes;
	}

	public void apply(IDocument document) {
		try {
			document.replace(fOffset, fLength, fProposalText);
		} catch (BadLocationException e) {
			//do nothing
		}
	}

	public Point getSelection(IDocument document) {
		return null;
	}

	public String getAdditionalProposalInfo() {
		return fProposalText;
	}

	public String getDisplayString() {
		return fEscapeBackslashes
				? PropertiesFileEditorMessages.EscapeBackslashCompletionProposal_escapeBackslashes
				: PropertiesFileEditorMessages.EscapeBackslashCompletionProposal_unescapeBackslashes;
	}

	public Image getImage() {
		return JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
	}

	public IContextInformation getContextInformation() {
		return null;
	}
}