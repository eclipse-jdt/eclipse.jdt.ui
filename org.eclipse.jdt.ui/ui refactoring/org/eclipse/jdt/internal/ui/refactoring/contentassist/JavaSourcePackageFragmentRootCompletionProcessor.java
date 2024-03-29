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
import java.util.List;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.Assert;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.contentassist.IContentAssistSubjectControl;
import org.eclipse.jface.contentassist.ISubjectControlContentAssistProcessor;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.CompletionProposalComparator;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposal;

/**
 * Simple completion processor that completes package fragment roots that are source
 * folders.
 */
public class JavaSourcePackageFragmentRootCompletionProcessor implements ISubjectControlContentAssistProcessor {

	private char[] fProposalAutoActivationSet;
	private IJavaModel fRoot;
	private CompletionProposalComparator fComparator;
	private JavaElementLabelProvider fLabelProvider;

	public JavaSourcePackageFragmentRootCompletionProcessor() {
		fRoot= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		String triggers= preferenceStore.getString(PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA);
		fProposalAutoActivationSet= triggers.toCharArray();
		fComparator= new CompletionProposalComparator();
		fLabelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_SMALL_ICONS);
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

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return fProposalAutoActivationSet;
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(IContentAssistSubjectControl contentAssistSubject, int documentOffset) {
		if (fRoot == null) {
			return null;
		}

		String input= contentAssistSubject.getDocument().get();
		String prefix= input.substring(0, documentOffset);

		ICompletionProposal[] proposals= createSourcePackageFragmentRootProposals(prefix, input.length());
		Arrays.sort(proposals, fComparator);
		return proposals;
	}

	/**
	 * @param prefix prefixString with thatthe sourcepackagefragmentroots must begin
	 * @param replacementLength length of the text to replace
	 * @return array with sourcepackagefragmentroots
	 */
	private ICompletionProposal[] createSourcePackageFragmentRootProposals(String prefix, int replacementLength) {
		List<JavaCompletionProposal> proposals= new ArrayList<>();
		try {
			for (IJavaProject project : fRoot.getJavaProjects()) {
				for (IPackageFragmentRoot root : project.getPackageFragmentRoots()) {
					if (root.exists() && (root.getKind() == IPackageFragmentRoot.K_SOURCE)) {
						String name= root.getPath().toString();
						if (name.length() > 1) {
							name= name.substring(1);
						}
						if (name.startsWith(prefix)) {
							Image image= fLabelProvider.getImage(root);
							JavaCompletionProposal proposal= new JavaCompletionProposal(name, 0, replacementLength, image,
								name, 0);
							proposals.add(proposal);
						}
					}
				}
			}
		} catch (JavaModelException e) {
			// nothing to do
		}

		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	@Override
	public IContextInformation[] computeContextInformation(IContentAssistSubjectControl contentAssistSubject, int documentOffset) {
		return null;
	}

}
