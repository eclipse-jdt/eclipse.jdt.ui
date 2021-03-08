/*******************************************************************************
 * Copyright (c) 2007, 2019 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.graphics.Point;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.text.template.contentassist.TemplateEngine;
import org.eclipse.jdt.internal.ui.text.template.contentassist.TemplateProposal;


/**
 * An template completion proposal computer can generate template completion proposals
 * from a given TemplateEngine.
 *
 * Subclasses must implement {@link #computeCompletionEngine(JavaContentAssistInvocationContext)}
 *
 * @since 3.4
 */
public abstract class AbstractTemplateCompletionProposalComputer implements IJavaCompletionProposalComputer {

	/**
	 * The engine for the current session, if any
	 */
	private TemplateEngine fEngine;

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#computeCompletionProposals(org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext context, IProgressMonitor monitor) {
		if (!(context instanceof JavaContentAssistInvocationContext)) {
			return Collections.emptyList();
		}

		JavaContentAssistInvocationContext javaContext= (JavaContentAssistInvocationContext) context;
		ICompilationUnit unit= javaContext.getCompilationUnit();
		if (unit == null) {
			return Collections.emptyList();
		}

		TemplateEngine engine= fEngine= computeCompletionEngine(javaContext);
		if (engine == null) {
			return Collections.emptyList();
		}

		engine.reset();
		ITextSelection viewerSelection= context.getTextSelection();
		if (viewerSelection == null) {
			viewerSelection = new TextSelection(context.getDocument(), context.getInvocationOffset(), 0);
		}
		Point selectionAsPoint = new Point(viewerSelection.getOffset(), viewerSelection.getLength());
		engine.complete(javaContext.getViewer(), selectionAsPoint, javaContext.getInvocationOffset(), unit);

		TemplateProposal[] templateProposals= engine.getResults();
		List<ICompletionProposal> result= new ArrayList<>(Arrays.asList(templateProposals));

		IJavaCompletionProposal[] keyWordResults= javaContext.getKeywordProposals();
		if (keyWordResults.length == 0) {
			return result;
		}

		/* Update relevance of template proposals that match with a keyword
		 * give those templates slightly more relevance than the keyword to
		 * sort them first.
		 */
		for (TemplateProposal curr : templateProposals) {
			String name= curr.getTemplate().getName();
			for (IJavaCompletionProposal keyWordResult : keyWordResults) {
				String keyword= keyWordResult.getDisplayString();
				if (name.startsWith(keyword)) {
					String content= curr.getTemplate().getPattern();
					if (content.startsWith(keyword)) {
						curr.setRelevance(keyWordResult.getRelevance() + 1);
						break;
					}
				}
			}
		}
		return result;
	}

	/**
	 * Compute the engine used to retrieve completion proposals in the given context
	 *
	 * @param context the context where proposals will be made
	 * @return the engine or <code>null</code> if no engine available in the context
	 */
	protected abstract TemplateEngine computeCompletionEngine(JavaContentAssistInvocationContext context);

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#computeContextInformation(org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public List<IContextInformation> computeContextInformation(ContentAssistInvocationContext context, IProgressMonitor monitor) {
		return Collections.emptyList();
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#getErrorMessage()
	 */
	@Override
	public String getErrorMessage() {
		return null;
	}

	/*
	 * @see org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer#sessionStarted()
	 */
	@Override
	public void sessionStarted() {
	}

	@Override
	public void sessionEnded() {
		TemplateEngine engine= fEngine;
		if (engine != null) {
			engine.reset();
			fEngine= null;
		}
	}

}
