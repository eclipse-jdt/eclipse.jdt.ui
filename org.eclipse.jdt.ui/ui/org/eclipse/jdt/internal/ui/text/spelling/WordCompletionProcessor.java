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

package org.eclipse.jdt.internal.ui.text.spelling;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IJavadocCompletionProcessor;
import org.eclipse.jdt.internal.ui.text.spelling.engine.ISpellCheckEngine;
import org.eclipse.jdt.internal.ui.text.spelling.engine.ISpellChecker;
import org.eclipse.jdt.internal.ui.text.spelling.engine.RankedWordProposal;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposal;

/**
 * Content assist processor to complete words.
 * 
 * @since 3.0
 */
public class WordCompletionProcessor implements IContentAssistProcessor, IJavadocCompletionProcessor {

	/** The prefix rank shift */
	protected static final int PREFIX_RANK_SHIFT= 4096;

	/** The error message */
	private String fError= null;

	/*
	 * @see org.eclipse.jdt.ui.text.java.IJavadocCompletionProcessor#computeCompletionProposals(org.eclipse.jdt.core.ICompilationUnit,int,int,int)
	 */
	public final IJavaCompletionProposal[] computeCompletionProposals(final ICompilationUnit cu, final int offset, final int length, final int flags) {
		if (contributes()) {
			IEditorInput editorInput= new FileEditorInput((IFile) cu.getResource());
			IDocument document= JavaUI.getDocumentProvider().getDocument(editorInput);
			return computeCompletionProposals(document, offset);
		}
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer,int)
	 */
	public ICompletionProposal[] computeCompletionProposals(final ITextViewer viewer, final int offset) {
		if (contributes())
			return computeCompletionProposals(viewer.getDocument(), offset);
		return null;
	}
	
	private boolean contributes() {
		return PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.SPELLING_ENABLE_CONTENTASSIST);
	}

	private IJavaCompletionProposal[] computeCompletionProposals(final IDocument document, final int offset) {
		IJavaCompletionProposal[] result= null;
		try {

			final IRegion region= document.getLineInformationOfOffset(offset);
			final String content= document.get(region.getOffset(), region.getLength());

			int index= offset - region.getOffset() - 1;
			while (index >= 0 && Character.isLetter(content.charAt(index)))
				index--;

			final int start= region.getOffset() + index + 1;
			final String candidate= content.substring(index + 1, offset - region.getOffset());

			if (candidate.length() > 0) {

				final ISpellCheckEngine engine= SpellCheckEngine.getInstance();
				final ISpellChecker checker= engine.createSpellChecker(engine.getLocale(), PreferenceConstants.getPreferenceStore());

				if (checker != null) {

					final ArrayList proposals= new ArrayList(checker.getProposals(candidate, Character.isUpperCase(candidate.charAt(0))));
					result= new IJavaCompletionProposal[proposals.size()];

					RankedWordProposal word= null;
					for (int proposal= 0; proposal < result.length; proposal++) {

						word= (RankedWordProposal)proposals.get(proposal);
						if (word.getText().startsWith(candidate))
							word.setRank(word.getRank() + PREFIX_RANK_SHIFT);
					}

//					Collections.sort(proposals);

					String text= null;
					for (int proposal= 0; proposal < result.length; proposal++) {

						word= (RankedWordProposal)proposals.get(proposal);
						text= word.getText();

//						result[result.length - proposal - 1]= new JavaCompletionProposal(text, start, candidate.length(), JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_RENAME), text, 0);
						result[proposal]= new JavaCompletionProposal(text, start, candidate.length(), JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_RENAME), text, word.getRank());
					}
				}
			}
		} catch (BadLocationException exception) {
			fError= (result == null) ? exception.getLocalizedMessage() : null;
		}
		return result;
	}

	/*
	 * @see org.eclipse.jdt.ui.text.java.IJavadocCompletionProcessor#computeContextInformation(org.eclipse.jdt.core.ICompilationUnit,int)
	 */
	public final IContextInformation[] computeContextInformation(final ICompilationUnit cu, final int offset) {
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer,int)
	 */
	public final IContextInformation[] computeContextInformation(final ITextViewer viewer, final int offset) {
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
	 */
	public final char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	public final char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
	 */
	public final IContextInformationValidator getContextInformationValidator() {
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
	 */
	public final String getErrorMessage() {
		return fError;
	}
}
