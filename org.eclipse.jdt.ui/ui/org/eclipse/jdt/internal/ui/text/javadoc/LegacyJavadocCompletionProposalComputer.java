/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.text.javadoc;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.IJavadocCompletionProcessor;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Java doc completion processor using contributed IJavaDocCompletionProcessor's
 * to evaluate proposals.
 *
 * @since 3.2
 */
public class LegacyJavadocCompletionProposalComputer implements IJavaCompletionProposalComputer {

	private static final String PROCESSOR_CONTRIBUTION_ID= "javadocCompletionProcessor"; //$NON-NLS-1$

	private IJavadocCompletionProcessor[] fSubProcessors;

	private String fErrorMessage;

	public LegacyJavadocCompletionProposalComputer() {
		fSubProcessors= null;
	}


	private IJavadocCompletionProcessor[] getContributedProcessors() {
		if (fSubProcessors == null) {
			try {
				IExtensionRegistry registry= Platform.getExtensionRegistry();
				IConfigurationElement[] elements=	registry.getConfigurationElementsFor(JavaUI.ID_PLUGIN, PROCESSOR_CONTRIBUTION_ID);
				IJavadocCompletionProcessor[] result= new IJavadocCompletionProcessor[elements.length];
				for (int i= 0; i < elements.length; i++) {
					result[i]= (IJavadocCompletionProcessor) elements[i].createExecutableExtension("class"); //$NON-NLS-1$
				}
				fSubProcessors= result;
			} catch (CoreException e) {
				JavaPlugin.log(e);
				fSubProcessors= new IJavadocCompletionProcessor[0];
			}
		}
		return fSubProcessors;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#computeContextInformation(org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public List<IContextInformation> computeContextInformation(ContentAssistInvocationContext context, IProgressMonitor monitor) {
		if (context instanceof JavaContentAssistInvocationContext) {
			JavaContentAssistInvocationContext javaContext= (JavaContentAssistInvocationContext) context;

			ICompilationUnit cu= javaContext.getCompilationUnit();
			int offset= javaContext.getInvocationOffset();

			ArrayList<IContextInformation> result= new ArrayList<>();

			String error= null;
			for (IJavadocCompletionProcessor curr : getContributedProcessors()) {
				IContextInformation[] contextInfos= curr.computeContextInformation(cu, offset);
				if (contextInfos != null) {
					result.addAll(Arrays.asList(contextInfos));
				} else if (error == null) {
					error= curr.getErrorMessage();
				}
			}
			fErrorMessage= error;
			return result;
		}
		return Collections.emptyList();
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#computeCompletionProposals(org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext context, IProgressMonitor monitor) {
		if (context instanceof JavadocContentAssistInvocationContext) {
			JavadocContentAssistInvocationContext javaContext= (JavadocContentAssistInvocationContext) context;

			ICompilationUnit cu= javaContext.getCompilationUnit();
			int offset= javaContext.getInvocationOffset();
			int length= javaContext.getSelectionLength();
			ITextSelection selection= javaContext.getTextSelection();
			if (selection != null && selection.getLength() > 0) {
				offset= selection.getOffset();
				length= selection.getLength();
			}

			ArrayList<ICompletionProposal> result= new ArrayList<>();

			for (IJavadocCompletionProcessor curr : getContributedProcessors()) {
				IJavaCompletionProposal[] proposals= curr.computeCompletionProposals(cu, offset, length, javaContext.getFlags());
				if (proposals != null) {
					result.addAll(Arrays.asList(proposals));
				}
			}
			return result;
		}
		return Collections.emptyList();
	}


	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#getErrorMessage()
	 */
	@Override
	public String getErrorMessage() {
		return fErrorMessage;
	}


	/*
	 * @see org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer#sessionStarted()
	 */
	@Override
	public void sessionStarted() {
	}


	/*
	 * @see org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer#sessionEnded()
	 */
	@Override
	public void sessionEnded() {
		fErrorMessage= null;
	}
}
