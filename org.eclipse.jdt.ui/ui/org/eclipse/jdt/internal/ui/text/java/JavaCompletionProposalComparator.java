package org.eclipse.jdt.internal.ui.text.java;

import java.util.Comparator;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

public class JavaCompletionProposalComparator implements Comparator {

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
		IJavaCompletionProposal c1= (IJavaCompletionProposal) o1;
		IJavaCompletionProposal c2= (IJavaCompletionProposal) o2;
		if (!fOrderAlphabetically) {
			int relevanceDif= c2.getRelevance() - c1.getRelevance();
			if (relevanceDif != 0) {
				return relevanceDif;
			}
		}
		return c1.getDisplayString().compareToIgnoreCase(c2.getDisplayString());
	}	
	
}
