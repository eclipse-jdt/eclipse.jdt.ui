/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

public class ConfigureProblemSeveritySubProcessor {

	public static final boolean hasConfigureProblemSeverityProposal(int problemId) {
		return JavaCore.getOptionForConfigurableSeverity(problemId) != null;
	}

	public static void addConfigureProblemSeverityProposal(final IInvocationContext context, final IProblemLocation problem, Collection<ICommandAccess> proposals) {
		final int problemId= problem.getProblemId();
	
		String optionId;
		if (problemId == IProblem.ProblemNotAnalysed) {
			String[] options= problem.getProblemArguments();
			if (options != null && options.length > 0) {
				optionId= options[0];
			} else {
				optionId= null;
			}
		} else {
			optionId= JavaCore.getOptionForConfigurableSeverity(problemId);
		}
		if (optionId == null)
			return;

		ChangeCorrectionProposal proposal= new ChangeCorrectionProposal(CorrectionMessages.ConfigureProblemSeveritySubProcessor_name, null, IProposalRelevance.CONFIGURE_PROBLEM_SEVERITY,
				JavaPluginImages.get(JavaPluginImages.IMG_CONFIGURE_PROBLEM_SEVERITIES)) {

			@Override
			public void apply(IDocument document) {
				ConfigureProblemSeverityAction problemSeverityAction= new ConfigureProblemSeverityAction(context.getCompilationUnit().getJavaProject(), optionId, (problemId & IProblem.Javadoc) != 0,
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
				return Messages.format(CorrectionMessages.ConfigureProblemSeveritySubProcessor_info, new String[] { problemMsg });
			}
		};

		proposals.add(proposal);
	}
}
