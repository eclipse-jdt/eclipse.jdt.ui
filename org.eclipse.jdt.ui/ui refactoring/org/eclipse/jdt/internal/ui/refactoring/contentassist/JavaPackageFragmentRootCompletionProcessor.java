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
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.CompletionProposalComparator;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposal;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;

/**
 * TODO: this class is not used anywhere yet.
 * (ContentAssist should be added to all source folder dialog fields.)
 */
public class JavaPackageFragmentRootCompletionProcessor implements IContentAssistProcessor, ISubjectControlContentAssistProcessor {
	private static final ImageDescriptorRegistry IMAGE_DESC_REGISTRY= JavaPlugin.getImageDescriptorRegistry();

	private IPackageFragmentRoot fPackageFragmentRoot;
	private CompletionProposalComparator fComparator;

	private char[] fProposalAutoActivationSet;

	/**
	 * Creates a <code>JavaPackageCompletionProcessor</code> to complete existing packages
	 * in the context of <code>packageFragmentRoot</code>.
	 *
	 * @param packageFragmentRoot the context for package completion
	 */
	public JavaPackageFragmentRootCompletionProcessor(IPackageFragmentRoot packageFragmentRoot) {
		fPackageFragmentRoot= packageFragmentRoot;
		fComparator= new CompletionProposalComparator();

		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		String triggers= preferenceStore.getString(PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA);
		fProposalAutoActivationSet = triggers.toCharArray();
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
		Assert.isTrue(false, "ITextViewer not supported"); //$NON-NLS-1$
		return null;
	}

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
		return fProposalAutoActivationSet;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
	 */
	@Override
	public String getErrorMessage() {
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
	 */
	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return null; //no context
	}

	/*
	 * @see ISubjectControlContentAssistProcessor#computeContextInformation(IContentAssistSubjectControl, int)
	 */
	@Override
	public IContextInformation[] computeContextInformation(IContentAssistSubjectControl contentAssistSubject,
			int documentOffset) {
		return null;
	}

	/*
	 * @see ISubjectControlContentAssistProcessor#computeCompletionProposals(IContentAssistSubjectControl, int)
	 */
	@Override
	public ICompletionProposal[] computeCompletionProposals(IContentAssistSubjectControl contentAssistSubjectControl, int documentOffset) {
		String input= contentAssistSubjectControl.getDocument().get();
		ICompletionProposal[] proposals= createPackagesProposals(documentOffset, input);
		Arrays.sort(proposals, fComparator);
		return proposals;
	}

	private ICompletionProposal[] createPackagesProposals(int documentOffset, String input) {
		ArrayList<JavaCompletionProposal> proposals= new ArrayList<>();
		String prefix= input.substring(0, documentOffset);
		try {
			for (IJavaElement packageFragment : fPackageFragmentRoot.getChildren()) {
				IPackageFragment pack= (IPackageFragment) packageFragment;
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
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	private static Image getImage(ImageDescriptor descriptor) {
		return (descriptor == null) ? null : JavaPackageFragmentRootCompletionProcessor.IMAGE_DESC_REGISTRY.get(descriptor);
	}
}
