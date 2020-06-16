/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.correction.ChangeCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ICommandAccess;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.java.hover.ConfigureProblemSeverityAction;
import org.eclipse.jdt.internal.ui.text.java.hover.ConfigureProblemSeverityAction.PreferencePage;

public class ConfigureProblemSeveritySubProcessor {

	public static final boolean hasConfigureProblemSeverityProposal(int problemId) {
		return JavaCore.getOptionForConfigurableSeverity(problemId) != null;
	}

	public static void addConfigureProblemSeverityProposal(final IInvocationContext context, final IProblemLocation problem, Collection<ICommandAccess> proposals) {
		final int problemId= problem.getProblemId();

		String optionId= JavaCore.getOptionForConfigurableSeverity(problemId);
		if (optionId == null)
			return;

		ChangeCorrectionProposal proposal= new ChangeCorrectionProposal(CorrectionMessages.ConfigureProblemSeveritySubProcessor_name, null, IProposalRelevance.CONFIGURE_PROBLEM_SEVERITY,
				JavaPluginImages.get(JavaPluginImages.IMG_CONFIGURE_PROBLEM_SEVERITIES)) {


			@Override
			public void apply(IDocument document) {
				PreferencePage preferencePage;
				if ((problemId & IProblem.Javadoc) != 0) {
					preferencePage= PreferencePage.JAVADOC;
				} else if ((problemId & IProblem.Compliance) != 0) {
					preferencePage= PreferencePage.COMPILER;
				} else {
					preferencePage= PreferencePage.ERRORS_WARNINGS;
				}
				ConfigureProblemSeverityAction problemSeverityAction= new ConfigureProblemSeverityAction(context.getCompilationUnit().getJavaProject(), optionId, JavaCore.PLUGIN_ID,
						preferencePage,
						null);
				problemSeverityAction.run();
			}

			@Override
			public String getAdditionalProposalInfo(IProgressMonitor monitor) {
				String problemMsg= ""; //$NON-NLS-1$
				for (IProblem iProblem : context.getASTRoot().getProblems()) {
					if (problem.getProblemId() == iProblem.getID()
							&& problem.getOffset() == iProblem.getSourceStart()
							&& problem.getLength() == iProblem.getSourceEnd() - iProblem.getSourceStart() + 1) {
						problemMsg= iProblem.getMessage();
						break;
					}
				}
				String msg= CorrectionMessages.ConfigureProblemSeveritySubProcessor_info;
				if ((problemId & IProblem.Compliance) != 0) {
					msg= CorrectionMessages.ConfigureProblemSeveritySubProcessor_compiler_info;
				}
				return Messages.format(msg, new String[] { problemMsg });
			}
		};

		proposals.add(proposal);
	}

	private ConfigureProblemSeveritySubProcessor() {
	}
}
