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

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistProcessorExtension;
import org.eclipse.jface.text.contentassist.IContentAssistSubject;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import org.eclipse.jdt.internal.corext.Assert;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposal;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposalComparator;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;

import org.eclipse.jdt.ui.PreferenceConstants;

public class JavaPackageCompletionProcessor implements IContentAssistProcessor, IContentAssistProcessorExtension {
	private static final ImageDescriptorRegistry IMAGE_DESC_REGISTRY= JavaPlugin.getImageDescriptorRegistry();
	
	private IPackageFragmentRoot fPackageFragmentRoot;
	private JavaCompletionProposalComparator fComparator;

	private String fErrorMessage;
	private char[] fProposalAutoActivationSet;

	/**
	 * Creates a <code>JavaPackageCompletionProcessor</code> to complete existing packages
	 * in the context of <code>packageFragmentRoot</code>.
	 *  
	 * @param packageFragmentRoot the context for package completion
	 */
	public JavaPackageCompletionProcessor(IPackageFragmentRoot packageFragmentRoot) {
		fPackageFragmentRoot= packageFragmentRoot;
		fComparator= new JavaCompletionProposalComparator();

		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getJavaTextTools().getPreferenceStore();
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
		String input= contentAssistSubject.getDocument().get();
		ICompletionProposal[] proposals= createPackagesProposals(documentOffset, input);
		Arrays.sort(proposals, fComparator);
		return proposals;
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
				Image image= getImage(JavaPluginImages.DESC_OBJS_PACKAGE);
				JavaCompletionProposal proposal= new JavaCompletionProposal(packName, 0, input.length(), image, packName, 0);
				proposals.add(proposal);
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
		return (ICompletionProposal[]) proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	private static Image getImage(ImageDescriptor descriptor) {
		return (descriptor == null) ? null : JavaPackageCompletionProcessor.IMAGE_DESC_REGISTRY.get(descriptor);
	}
}