/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.propertiesfileeditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.TextInvocationContext;

import org.eclipse.ui.texteditor.spelling.SpellingCorrectionProcessor;

import org.eclipse.ltk.core.refactoring.NullChange;

import org.eclipse.jdt.ui.text.java.CompletionProposalComparator;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.text.correction.proposals.ChangeCorrectionProposal;

/**
 * The properties file correction processor. Clients can set pre-computed proposals, and if set the
 * processor returns only these pre-computed proposals.
 * 
 * @since 3.7
 */
public class PropertiesCorrectionProcessor implements org.eclipse.jface.text.quickassist.IQuickAssistProcessor {

	private String fErrorMessage;

	private SpellingCorrectionProcessor fSpellingCorrectionProcessor;

	private ICompletionProposal[] fPreComputedProposals;

	public PropertiesCorrectionProcessor() {
		fSpellingCorrectionProcessor= new SpellingCorrectionProcessor();
	}

	/*
	 * @see IContentAssistProcessor#computeCompletionProposals(ITextViewer, int)
	 */
	public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext quickAssistContext) {

		ISourceViewer viewer= quickAssistContext.getSourceViewer();
		int length= viewer != null ? viewer.getSelectedRange().y : 0;
		TextInvocationContext context= new TextInvocationContext(viewer, quickAssistContext.getOffset(), length);

		fErrorMessage= null;
		ICompletionProposal[] res= null;
		if (fPreComputedProposals != null) {
			res= fPreComputedProposals;
		} else {
			try {
				List<ICompletionProposal> proposals= new ArrayList<ICompletionProposal>();
				ICompletionProposal[] spellingProposals= fSpellingCorrectionProcessor.computeQuickAssistProposals(quickAssistContext);
				if (spellingProposals.length > 1) {
					for (int i= 0; i < spellingProposals.length; i++) {
						proposals.add(spellingProposals[i]);
					}
				}
				ICompletionProposal[] assists= collectAssists(context);
				if (assists != null) {
					for (int i= 0; i < assists.length; i++) {
						proposals.add(assists[i]);
					}
				}
				res= proposals.toArray(new ICompletionProposal[proposals.size()]);
			} catch (BadLocationException e) {
				fErrorMessage= CorrectionMessages.JavaCorrectionProcessor_error_quickassist_message;
				JavaPlugin.log(e);
			} catch (BadPartitioningException e) {
				fErrorMessage= CorrectionMessages.JavaCorrectionProcessor_error_quickassist_message;
				JavaPlugin.log(e);
			}
		}

		if (res == null || res.length == 0) {
			return new ICompletionProposal[] { new ChangeCorrectionProposal(CorrectionMessages.NoCorrectionProposal_description, new NullChange(""), 0, null) }; //$NON-NLS-1$
		}
		if (res.length > 1) {
			Arrays.sort(res, new CompletionProposalComparator());
		}
		fPreComputedProposals= null;
		return res;
	}

	private static ICompletionProposal[] collectAssists(IQuickAssistInvocationContext invocationContext) throws BadLocationException, BadPartitioningException {
		ISourceViewer sourceViewer= invocationContext.getSourceViewer();
		IDocument document= sourceViewer.getDocument();
		Point selectedRange= sourceViewer.getSelectedRange();
		int selectionOffset= selectedRange.x;
		int selectionLength= selectedRange.y;
		int proposalOffset;
		int proposalLength;
		String text;
		if (selectionLength == 0) {
			if (selectionOffset != document.getLength()) {
				char ch= document.getChar(selectionOffset);
				if (ch == '=' || ch == ':') { //see PropertiesFilePartitionScanner()
					return null;
				}
			}

			ITypedRegion partition= null;
			if (document instanceof IDocumentExtension3)
				partition= ((IDocumentExtension3)document).getPartition(IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING, invocationContext.getOffset(), false);
			if (partition == null)
				return null;

			String type= partition.getType();
			if (!(type.equals(IPropertiesFilePartitions.PROPERTY_VALUE) || type.equals(IDocument.DEFAULT_CONTENT_TYPE))) {
				return null;
			}
			proposalOffset= partition.getOffset();
			proposalLength= partition.getLength();
			text= document.get(proposalOffset, proposalLength);

			if (type.equals(IPropertiesFilePartitions.PROPERTY_VALUE)) {
				text= text.substring(1); //see PropertiesFilePartitionScanner()
				proposalOffset++;
				proposalLength--;
			}
		} else {
			proposalOffset= selectionOffset;
			proposalLength= selectionLength;
			text= document.get(proposalOffset, proposalLength);
		}

		if (PropertiesFileEscapes.containsUnescapedBackslash(text))
			return new ICompletionProposal[] { new EscapeBackslashCompletionProposal(PropertiesFileEscapes.escape(text, false, true, false), proposalOffset, proposalLength, true) };
		if (PropertiesFileEscapes.containsEscapedBackslashes(text))
			return new ICompletionProposal[] { new EscapeBackslashCompletionProposal(PropertiesFileEscapes.unescapeBackslashes(text), proposalOffset, proposalLength, false) };
		return null;
	}

	/*
	 * @see IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		return fErrorMessage;
	}

	/*
	 * @see org.eclipse.jface.text.quickassist.IQuickAssistProcessor#canFix(org.eclipse.jface.text.source.Annotation)
	 */
	public boolean canFix(Annotation annotation) {
		return fSpellingCorrectionProcessor.canFix(annotation);
	}

	/*
	 * @see org.eclipse.jface.text.quickassist.IQuickAssistProcessor#canAssist(org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext)
	 */
	public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
		try {
			return collectAssists(invocationContext) != null;
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		} catch (BadPartitioningException e) {
			JavaPlugin.log(e);
		}
		return false;
	}

	/**
	 * Sets the pre-computed proposals.
	 * 
	 * @param preComputedProposals the pre-computed proposals
	 */
	public void setProposals(ICompletionProposal[] preComputedProposals) {
		fPreComputedProposals= preComputedProposals;
	}
}
