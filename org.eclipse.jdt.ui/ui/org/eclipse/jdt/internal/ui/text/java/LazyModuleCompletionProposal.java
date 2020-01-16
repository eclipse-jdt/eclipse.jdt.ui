/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
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

package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

public class LazyModuleCompletionProposal extends LazyJavaCompletionProposal {

	private ICompilationUnit fCompilationUnit;

	public LazyModuleCompletionProposal(CompletionProposal proposal, JavaContentAssistInvocationContext context) {
		super(proposal, context);
		fCompilationUnit= context.getCompilationUnit();
	}
	@Override
	protected ProposalInfo computeProposalInfo() {

		IJavaProject project;
		if (fCompilationUnit != null)
			project= fCompilationUnit.getJavaProject();
		else
			project= fInvocationContext.getProject();
		if (project != null) {
			return new ModuleProposalInfo(project, fProposal);
		}

		return super.computeProposalInfo();
	}
}
