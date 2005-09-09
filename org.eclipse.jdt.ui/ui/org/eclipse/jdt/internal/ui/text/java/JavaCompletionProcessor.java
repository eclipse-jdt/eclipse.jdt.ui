/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.text.java.CompletionProposalComparator;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaUIMessages;

/**
 * Java completion processor.
 */
public class JavaCompletionProcessor extends ContentAssistProcessor {

	private final static String VISIBILITY= JavaCore.CODEASSIST_VISIBILITY_CHECK;
	private final static String ENABLED= "enabled"; //$NON-NLS-1$
	private final static String DISABLED= "disabled"; //$NON-NLS-1$
	private static final Set KEYWORDS;
	
	private IContextInformationValidator fValidator;

	private int fNumberOfComputedResults= 0;
	
	private final CompletionProposalComparator fAlphaComparator;
	private final CompletionProposalComparator fComparator;
	protected final IEditorPart fEditor;
	

	public JavaCompletionProcessor(IEditorPart editor, String partition) {
		super(partition);
		fEditor= editor;
		fAlphaComparator= new CompletionProposalComparator();
		fAlphaComparator.setOrderAlphabetically(true);
		fComparator= new CompletionProposalComparator();
	}

	/**
	 * Tells this processor to restrict its proposal to those element
	 * visible in the actual invocation context.
	 *
	 * @param restrict <code>true</code> if proposals should be restricted
	 */
	public void restrictProposalsToVisibility(boolean restrict) {
		Hashtable options= JavaCore.getOptions();
		Object value= options.get(VISIBILITY);
		if (value instanceof String) {
			String newValue= restrict ? ENABLED : DISABLED;
			if ( !newValue.equals(value)) {
				options.put(VISIBILITY, newValue);
				JavaCore.setOptions(options);
			}
		}
	}

	/**
	 * Tells this processor to order the proposals alphabetically.
	 *
	 * @param order <code>true</code> if proposals should be ordered.
	 */
	public void orderProposalsAlphabetically(boolean order) {
		fComparator.setOrderAlphabetically(order);
	}

	/**
	 * Tells this processor to restrict is proposals to those
	 * starting with matching cases.
	 *
	 * @param restrict <code>true</code> if proposals should be restricted
	 */
	public void restrictProposalsToMatchingCases(boolean restrict) {
		// not yet supported
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		if (fNumberOfComputedResults == 0)
			return JavaUIMessages.JavaEditor_codeassist_noCompletions;
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		if (fValidator == null)
			fValidator= new JavaParameterListValidator();
		return fValidator;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.ContentAssistProcessor#filterAndSort(java.util.List, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected List filterAndSortProposals(List proposals, IProgressMonitor monitor, TextContentAssistInvocationContext context) {
		filter(proposals, context);
		Collections.sort(proposals, fComparator);
		fNumberOfComputedResults= proposals.size();
		return proposals;
	}

	private void filter(List proposals, TextContentAssistInvocationContext context) {

		/*
		 * TODO filtering is hard if the subjects come from multiple,
		 * often unknown sources. This method implements some heuristics
		 * that seem to work well.
		 */
		
		/*
		 * Duplicate filter:
		 *  Goal: remove hippie proposals for stuff that already have java proposals
		 *  Implementation:
		 *  - sort alphanumerically, sorting hippies on top
		 *  - remove any non-IJavaCompletionProposals that happen to have
		 *    the same prefix completion text as another proposal.
		 */
		Collections.sort(proposals, fAlphaComparator);

		IDocument document= context.getDocument();
		int offset= context.getInvocationOffset();

		ICompletionProposalExtension3 last= null;
		for (ListIterator it= proposals.listIterator(proposals.size()); it.hasPrevious();) {
			ICompletionProposal proposal= (ICompletionProposal) it.previous();
			ICompletionProposalExtension3 current= null;
			if (proposal instanceof ICompletionProposalExtension3) {
				current= (ICompletionProposalExtension3) proposal;
				if (last != null) {
					String lastCompletion= getPrefixCompletionText(document, offset, last);
					String curCompletion= getPrefixCompletionText(document, offset, current);
					if (lastCompletion.equals(curCompletion) && !(current instanceof IJavaCompletionProposal))
						it.remove();
				}
			}
			last= current;
		}

		/*
		 * Keyword filter: 
		 *  Goal: remove any proposal that is equal to a
		 *            Java keyword but is not a IJavaCompletionProposal
		 *  Implementation:
		 *   - remove all proposals that hava keyword display string
		 */
		for (Iterator iter= proposals.iterator(); iter.hasNext();) {
			ICompletionProposal proposal= (ICompletionProposal) iter.next();
			if (!(proposal instanceof IJavaCompletionProposal)) {
				if (isKeyword(proposal.getDisplayString()))
					iter.remove();
			}

		}
	}

	private String getPrefixCompletionText(IDocument document, int offset, ICompletionProposalExtension3 last) {
		CharSequence prefixCompletionText= last.getPrefixCompletionText(document, offset);
		return prefixCompletionText == null ? ((ICompletionProposal) last).getDisplayString() : prefixCompletionText.toString();
	}
	
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
	 * @see org.eclipse.jdt.internal.ui.text.java.ContentAssistProcessor#filterAndSortContextInformation(java.util.List, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected List filterAndSortContextInformation(List contexts, IProgressMonitor monitor) {
		fNumberOfComputedResults= contexts.size();
		return super.filterAndSortContextInformation(contexts, monitor);
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.ContentAssistProcessor#createContext(org.eclipse.jface.text.ITextViewer, int)
	 */
	protected TextContentAssistInvocationContext createContext(ITextViewer viewer, int offset) {
		return new JavaContentAssistInvocationContext(viewer, offset, fEditor);
	}
}
