/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

import java.util.Comparator;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.TemplateProposal;

import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

public class JavaCompletionProposalComparator implements Comparator {
	
	private static JavaCompletionProposalComparator fgInstance= new JavaCompletionProposalComparator();

	public static JavaCompletionProposalComparator getInstance() {
		return fgInstance;
	}
	
	private boolean fOrderAlphabetically;

	/**
	 * Constructor for CompletionProposalComparator.
	 */
	public JavaCompletionProposalComparator() {
		fOrderAlphabetically= false;
	}
	
	public void setOrderAlphabetically(boolean orderAlphabetically) {
		fOrderAlphabetically= orderAlphabetically;
	}
	
	/* (non-Javadoc)
	 * @see Comparator#compare(Object, Object)
	 */
	public int compare(Object o1, Object o2) {
		ICompletionProposal p1= (ICompletionProposal) o1;
		ICompletionProposal p2= (ICompletionProposal) o2;

		if (!fOrderAlphabetically) {
			int r1= getRelevance(p1);
			int r2= getRelevance(p2);
			int relevanceDif= r2 - r1;
			if (relevanceDif != 0) {
				return relevanceDif;
			}
		}
		// fix for bug 67468 
		return p1.getDisplayString().compareToIgnoreCase(p2.getDisplayString());
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
