/*******************************************************************************
 * Copyright (c) 2017 GK Software AG, and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.contentassist;

import java.util.ArrayList;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.contentassist.IContentAssistSubjectControl;
import org.eclipse.jface.contentassist.ISubjectControlContentAssistProcessor;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposal;

/**
 * Content assist processor for a precomputed, fixed set of names.
 * For each name matching the current prefix a {@link JavaCompletionProposal} will be provided,
 * using the {@link Image} passed into the constructor.
 */
public class JavaPrecomputedNamesAssistProcessor implements ISubjectControlContentAssistProcessor {
	private Iterable<String> fNames;
	private Image fImage;
	private char[] fProposalAutoActivationSet;

	public JavaPrecomputedNamesAssistProcessor(Iterable<String> names, Image image) {
		fNames= names;
		fImage= image;

		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		String triggers= preferenceStore.getString(PreferenceConstants.CODEASSIST_AUTOACTIVATION_TRIGGERS_JAVA);
		fProposalAutoActivationSet= triggers.toCharArray();
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		Assert.isTrue(false, "ITextViewer not supported"); //$NON-NLS-1$
		return null;
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
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
		return null; //no context
	}

	@Override
	public IContextInformation[] computeContextInformation(IContentAssistSubjectControl contentAssistSubjectControl, int documentOffset) {
		return null;
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(IContentAssistSubjectControl contentAssistSubjectControl, int documentOffset) {
		ArrayList<JavaCompletionProposal> proposals= new ArrayList<>();
		String input= contentAssistSubjectControl.getDocument().get();
		String prefix= input.substring(0, documentOffset).trim();
		for (String name : fNames) {
			if (input.isEmpty() || name.startsWith(prefix)) {
				JavaCompletionProposal proposal= new JavaCompletionProposal(name, 0, input.length(), fImage, name, 0);
				proposals.add(proposal);
			}
		}
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}
}