/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;

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
	public List computeCompletionProposals(ContentAssistInvocationContext context, IProgressMonitor monitor) {
		if (!(context instanceof JavaContentAssistInvocationContext))
			return Collections.EMPTY_LIST;
		
		JavaContentAssistInvocationContext javaContext= (JavaContentAssistInvocationContext) context;
		ICompilationUnit unit= javaContext.getCompilationUnit();
		if (unit == null)
			return Collections.EMPTY_LIST;

		fEngine= computeCompletionEngine(javaContext);
		if (fEngine != null) {
			
			fEngine.reset();
			fEngine.complete(javaContext.getViewer(), javaContext.getInvocationOffset(), unit);

			TemplateProposal[] templateProposals= fEngine.getResults();
			List result= new ArrayList(Arrays.asList(templateProposals));

			IJavaCompletionProposal[] keyWordResults= javaContext.getKeywordProposals();
			if (keyWordResults.length > 0) {
				List removals= new ArrayList();
				
				// update relevance of template proposals that match with a keyword
				// give those templates slightly more relevance than the keyword to
				// sort them first
				// remove keyword templates that don't have an equivalent
				// keyword proposal
				if (keyWordResults.length > 0) {
					outer: for (int k= 0; k < templateProposals.length; k++) {
						TemplateProposal curr= templateProposals[k];
						String name= curr.getTemplate().getName();
						for (int i= 0; i < keyWordResults.length; i++) {
							String keyword= keyWordResults[i].getDisplayString();
							if (name.startsWith(keyword)) {
								curr.setRelevance(keyWordResults[i].getRelevance() + 1);
								continue outer;
							}
						}
						if (isKeyword(name))
							removals.add(curr);
					}
				}
				
				result.removeAll(removals);
			}
			return result;
		}
		
		return Collections.EMPTY_LIST;
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
	public List computeContextInformation(ContentAssistInvocationContext context, IProgressMonitor monitor) {
		return Collections.EMPTY_LIST;
	}

	private static final Set KEYWORDS;
	
	static {
		Set keywords= new HashSet(42);
		keywords.add("abstract"); //$NON-NLS-1$
		keywords.add("assert"); //$NON-NLS-1$
		keywords.add("break"); //$NON-NLS-1$
		keywords.add("case"); //$NON-NLS-1$
		keywords.add("catch"); //$NON-NLS-1$
		keywords.add("class"); //$NON-NLS-1$
		keywords.add("continue"); //$NON-NLS-1$
		keywords.add("default"); //$NON-NLS-1$
		keywords.add("do"); //$NON-NLS-1$
		keywords.add("else"); //$NON-NLS-1$
		keywords.add("elseif"); //$NON-NLS-1$
		keywords.add("extends"); //$NON-NLS-1$
		keywords.add("final"); //$NON-NLS-1$
		keywords.add("finally"); //$NON-NLS-1$
		keywords.add("for"); //$NON-NLS-1$
		keywords.add("if"); //$NON-NLS-1$
		keywords.add("implements"); //$NON-NLS-1$
		keywords.add("import"); //$NON-NLS-1$
		keywords.add("instanceof"); //$NON-NLS-1$
		keywords.add("interface"); //$NON-NLS-1$
		keywords.add("native"); //$NON-NLS-1$
		keywords.add("new"); //$NON-NLS-1$
		keywords.add("package"); //$NON-NLS-1$
		keywords.add("private"); //$NON-NLS-1$
		keywords.add("protected"); //$NON-NLS-1$
		keywords.add("public"); //$NON-NLS-1$
		keywords.add("return"); //$NON-NLS-1$
		keywords.add("static"); //$NON-NLS-1$
		keywords.add("strictfp"); //$NON-NLS-1$
		keywords.add("super"); //$NON-NLS-1$
		keywords.add("switch"); //$NON-NLS-1$
		keywords.add("synchronized"); //$NON-NLS-1$
		keywords.add("this"); //$NON-NLS-1$
		keywords.add("throw"); //$NON-NLS-1$
		keywords.add("throws"); //$NON-NLS-1$
		keywords.add("transient"); //$NON-NLS-1$
		keywords.add("try"); //$NON-NLS-1$
		keywords.add("volatile"); //$NON-NLS-1$
		keywords.add("while"); //$NON-NLS-1$
		keywords.add("true"); //$NON-NLS-1$
		keywords.add("false"); //$NON-NLS-1$
		keywords.add("null"); //$NON-NLS-1$
		KEYWORDS= Collections.unmodifiableSet(keywords);
	}

	private boolean isKeyword(String name) {
		return KEYWORDS.contains(name);
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#getErrorMessage()
	 */
	public String getErrorMessage() {
		return null;
	}

	/*
	 * @see org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer#sessionStarted()
	 */
	public void sessionStarted() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer#sessionEnded()
	 */
	public void sessionEnded() {
		if (fEngine != null) {
			fEngine.reset();
			fEngine= null;
		}
	}

}
