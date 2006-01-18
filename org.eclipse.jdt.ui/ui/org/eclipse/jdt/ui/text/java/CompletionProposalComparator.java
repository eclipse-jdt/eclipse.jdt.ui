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
package org.eclipse.jdt.ui.text.java;

import java.util.Comparator;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.TemplateProposal;

import org.eclipse.jdt.internal.corext.util.History;
import org.eclipse.jdt.internal.corext.util.TypeInfoHistory;

import org.eclipse.jdt.internal.ui.text.correction.AddImportCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal;
import org.eclipse.jdt.internal.ui.text.java.LazyJavaTypeCompletionProposal;

/**
 * Comparator for java completion proposals. Completion proposals can be sorted
 * by relevance or alphabetically.
 *
 * @since 3.1
 */
public final class CompletionProposalComparator implements Comparator {

	private boolean fOrderAlphabetically;

	/**
	 * Creates a comparator that sorts by relevance.
	 */
	public CompletionProposalComparator() {
		fOrderAlphabetically= false;
	}

	/**
	 * Sets the sort order. Default is <code>false</code>, i.e. order by
	 * relevance.
	 *
	 * @param orderAlphabetically <code>true</code> to order alphabetically,
	 *        <code>false</code> to order by relevance
	 */
	public void setOrderAlphabetically(boolean orderAlphabetically) {
		fOrderAlphabetically= orderAlphabetically;
	}

	/*
	 * @see Comparator#compare(Object, Object)
	 */
	public int compare(Object o1, Object o2) {
		ICompletionProposal p1= (ICompletionProposal) o1;
		ICompletionProposal p2= (ICompletionProposal) o2;

		if (!fOrderAlphabetically) {
			
			if (p1 instanceof AddImportCorrectionProposal && p2 instanceof AddImportCorrectionProposal) {
				AddImportCorrectionProposal addImport1= (AddImportCorrectionProposal)p1;
				AddImportCorrectionProposal addImport2= (AddImportCorrectionProposal)p2;
				
				String key1= addImport1.getQualifiedTypeName();
				String key2= addImport2.getQualifiedTypeName();
				
				History history= TypeInfoHistory.getDefault();
				int histCompare= history.compareByKeys(key1, key2);
				if (histCompare != 0)
					return histCompare;
			}
			
			if (p1 instanceof LazyJavaTypeCompletionProposal && p2 instanceof LazyJavaTypeCompletionProposal) {
				LazyJavaTypeCompletionProposal typeProposal1= (LazyJavaTypeCompletionProposal)p1;
				LazyJavaTypeCompletionProposal typeProposal2= (LazyJavaTypeCompletionProposal)p2;
				
				String key1= typeProposal1.getQualifiedTypeName();
				String key2= typeProposal2.getQualifiedTypeName();
				
				History history= TypeInfoHistory.getDefault();
				int histCompare= history.compareByKeys(key1, key2);
				if (histCompare != 0)
					return histCompare;
			}
			
			int r1= getRelevance(p1);
			int r2= getRelevance(p2);
			int relevanceDif= r2 - r1;
			if (relevanceDif != 0) {
				return relevanceDif;
			}
		}
		/*
		 * TODO the correct (but possibly much slower) sorting would use a
		 * collator.
		 */
		// fix for bug 67468
		return getSortKey(p1).compareToIgnoreCase(getSortKey(p2));
	}

	private String getSortKey(ICompletionProposal p) {
		if (p instanceof LazyJavaCompletionProposal)
			return ((LazyJavaCompletionProposal) p).getSortString();
		return p.getDisplayString();
	}

	private int getRelevance(ICompletionProposal obj) {
		if (obj instanceof IJavaCompletionProposal) {
			IJavaCompletionProposal jcp= (IJavaCompletionProposal) obj;
			return jcp.getRelevance();
		} else if (obj instanceof TemplateProposal) {
			TemplateProposal tp= (TemplateProposal) obj;
			return tp.getRelevance();
		}
		// catch all
		return 0;
	}

}
