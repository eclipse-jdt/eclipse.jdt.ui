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
package org.eclipse.jdt.internal.ui.refactoring.contentassist;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.contentassist.IContentAssistProcessorExtension;
import org.eclipse.jface.contentassist.IContentAssistSubject;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.jdt.internal.corext.Assert;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposal;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposalComparator;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.PreferenceConstants;

public class JavaPackageCompletionProcessor implements IContentAssistProcessor, IContentAssistProcessorExtension {
	
	private IPackageFragmentRoot fPackageFragmentRoot;
	private JavaCompletionProposalComparator fComparator;
	private ILabelProvider fLabelProvider;

	private String fErrorMessage;
	private char[] fProposalAutoActivationSet;

	/**
	 * Creates a <code>JavaPackageCompletionProcessor</code>.
	 * The completion context must be set via {@link #setPackageFragmentRoot(IPackageFragmentRoot)}.
	 */
	public JavaPackageCompletionProcessor() {
	    this(new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_SMALL_ICONS));
	}
	
    /**
     * Creates a <code>JavaPackageCompletionProcessor</code>.
     * The Processor uses the given <code>ILabelProvider</code> to show text and icons for the 
     * possible completions.
     * @param labelProvider Used for the popups.
     */
	public JavaPackageCompletionProcessor(ILabelProvider labelProvider) {
		fComparator= new JavaCompletionProposalComparator();
		fLabelProvider= labelProvider;

		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		String triggers= preferenceStore.getString(PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA);
		fProposalAutoActivationSet = triggers.toCharArray();
	}

	/**
	 * Computing proposals on a <code>ITextViewer</code> is not supported.
	 * 
	 * @see #computeCompletionProposals(IContentAssistSubject, int)
	 * @see IContentAssistProcessor#computeCompletionProposals(ITextViewer,
	 *      int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
		Assert.isTrue(false, "ITextViewer not supported"); //$NON-NLS-1$
		return null;
	}

	/**
	 * Computing context information on a <code>ITextViewer</code> is not
	 * supported.
	 * 
	 * @see #computeContextInformation(IContentAssistSubject, int)
	 * @see IContentAssistProcessor#computeContextInformation(ITextViewer, int)
	 */
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
		Assert.isTrue(false, "ITextViewer not supported"); //$NON-NLS-1$
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
	 */
	public char[] getCompletionProposalAutoActivationCharacters() {
		return fProposalAutoActivationSet;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		return fErrorMessage;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		return null; //no context
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessorExtension#computeContextInformation(org.eclipse.jface.text.contentassist.IContentAssistSubject, int)
	 */
	public IContextInformation[] computeContextInformation(IContentAssistSubject contentAssistSubject,
			int documentOffset) {
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessorExtension#computeCompletionProposals(org.eclipse.jface.text.contentassist.IContentAssistSubject,
	 *      int)
	 */
	public ICompletionProposal[] computeCompletionProposals(IContentAssistSubject contentAssistSubject, int documentOffset) {
		if (fPackageFragmentRoot == null)
			return null;
		String input= contentAssistSubject.getDocument().get();
		ICompletionProposal[] proposals= createPackagesProposals(documentOffset, input);
		Arrays.sort(proposals, fComparator);
		return proposals;
	}
	
	public void setPackageFragmentRoot(IPackageFragmentRoot packageFragmentRoot) {
		fPackageFragmentRoot= packageFragmentRoot;
	}

	private ICompletionProposal[] createPackagesProposals(int documentOffset, String input) {
		ArrayList proposals= new ArrayList();
		String prefix= input.substring(0, documentOffset);
		try {
			IJavaElement[] packageFragments= fPackageFragmentRoot.getChildren();
			for (int i= 0; i < packageFragments.length; i++) {
				IPackageFragment pack= (IPackageFragment) packageFragments[i];
				String packName= pack.getElementName();
				if (packName.length() == 0 || ! packName.startsWith(prefix))
					continue;
				Image image= fLabelProvider.getImage(pack);
				JavaCompletionProposal proposal= new JavaCompletionProposal(packName, 0, input.length(), image, fLabelProvider.getText(pack), 0);
				proposals.add(proposal);
			}
		} catch (JavaModelException e) {
			//fPackageFragmentRoot is not a proper root -> no proposals
		}
		return (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);
	}
}