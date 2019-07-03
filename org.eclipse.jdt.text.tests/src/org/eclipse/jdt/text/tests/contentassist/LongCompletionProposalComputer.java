/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc., and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.jdt.text.tests.contentassist;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class LongCompletionProposalComputer implements IJavaCompletionProposalComputer {

	public static final String CONTENT_TRIGGER_STRING = "longCompletion";

	@Override
	public void sessionStarted() {
	}

	@Override
	public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext context, IProgressMonitor monitor) {
		if (context.getDocument().get().contains(CONTENT_TRIGGER_STRING)) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				JavaPlugin.log(e);
			}
			return Collections.singletonList(new CompletionProposal(CONTENT_TRIGGER_STRING, 0, 0, 0, null, CONTENT_TRIGGER_STRING, null, null));
		}
		return Collections.emptyList();
	}

	@Override
	public List<IContextInformation> computeContextInformation(ContentAssistInvocationContext context, IProgressMonitor monitor) {
		return Collections.emptyList();
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public void sessionEnded() {
	}

}
