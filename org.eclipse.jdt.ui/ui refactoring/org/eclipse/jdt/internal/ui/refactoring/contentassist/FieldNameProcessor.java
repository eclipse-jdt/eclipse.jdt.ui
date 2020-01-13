/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

package org.eclipse.jdt.internal.ui.refactoring.contentassist;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.contentassist.IContentAssistSubjectControl;
import org.eclipse.jface.contentassist.ISubjectControlContentAssistProcessor;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import org.eclipse.jdt.internal.corext.refactoring.code.PromoteTempToFieldRefactoring;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposal;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

public class FieldNameProcessor implements IContentAssistProcessor, ISubjectControlContentAssistProcessor {

	private String[] fFieldNameProposals;
	private String fErrorMessage;
	private ImageDescriptorRegistry fImageRegistry= JavaPlugin.getImageDescriptorRegistry();
	private PromoteTempToFieldRefactoring fRefactoring;

	public FieldNameProcessor(String[] guessedFieldNames, PromoteTempToFieldRefactoring refactoring) {
		fRefactoring= refactoring;
		fFieldNameProposals= refactoring.guessFieldNames();
		Arrays.sort(fFieldNameProposals);
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
	 */
	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
		Assert.isTrue(false, "ITextViewer not supported"); //$NON-NLS-1$
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer, int)
	 */
	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
		Assert.isTrue(false, "ITextViewer not supported"); //$NON-NLS-1$
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
	 */
	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return null; //no context
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
	 */
	@Override
	public String getErrorMessage() {
		return fErrorMessage;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
	 */
	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return null; //no context
	}

	/*
	 * @see org.eclipse.jface.contentassist.IContentAssistProcessorExtension#computeContextInformation(org.eclipse.jface.contentassist.IContentAssistSubject, int)
	 */
	@Override
	public IContextInformation[] computeContextInformation(IContentAssistSubjectControl contentAssistSubject, int documentOffset) {
		return null; //no context
	}

	/*
	 * @see org.eclipse.jface.contentassist.IContentAssistProcessorExtension#computeCompletionProposals(org.eclipse.jface.contentassist.IContentAssistSubject, int)
	 */
	@Override
	public ICompletionProposal[] computeCompletionProposals(IContentAssistSubjectControl contentAssistSubject, int documentOffset) {
		if (fFieldNameProposals.length == 0)
			return null;
		String input= contentAssistSubject.getDocument().get();

		ArrayList<JavaCompletionProposal> proposals= new ArrayList<>();
		String prefix= input.substring(0, documentOffset);
		ImageDescriptor imageDescriptor= JavaElementImageProvider.getFieldImageDescriptor(false, fRefactoring.getVisibility());
		Image image= fImageRegistry.get(imageDescriptor);
		for (String tempName : fFieldNameProposals) {
			if (tempName.length() == 0 || ! tempName.startsWith(prefix))
				continue;
			JavaCompletionProposal proposal= new JavaCompletionProposal(tempName, 0, input.length(), image, tempName, 0);
			proposals.add(proposal);
		}
		fErrorMessage= proposals.size() > 0 ? null : JavaUIMessages.JavaEditor_codeassist_noCompletions;
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

}
