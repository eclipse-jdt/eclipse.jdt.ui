/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor.breadcrumb;


import com.ibm.icu.text.BreakIterator;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.jdt.internal.ui.util.StringMatcher;

/**
 * PatternFilter filter used in conjunction with <code>FilteredTable</code>.  In order to 
 * determine if a node should be filtered it uses the content provider of the 
 * table to do pattern matching on its elements.
 * 
 * @since 3.4
 * @see org.eclipse.ui.dialogs.PatternFilter
 */
public class PatternFilter extends ViewerFilter {

	/**
	 * The string pattern matcher used for this pattern filter.  
	 */
	private StringMatcher fMatcher;

	public PatternFilter() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public final boolean select(Viewer viewer, Object parentElement, Object element) {
		String labelText= ((ILabelProvider) ((StructuredViewer) viewer).getLabelProvider()).getText(element);

		if (labelText == null)
			return false;

		return wordMatches(labelText);
	}

	/**
	 * The pattern string for which this filter should select 
	 * elements in the viewer.
	 * 
	 * @param patternString
	 */
	public void setPattern(String patternString) {
		if (patternString == null || patternString.equals("")) { //$NON-NLS-1$
			fMatcher= null;
		} else {
			String pattern= patternString + "*"; //$NON-NLS-1$ 

			fMatcher= new StringMatcher(pattern, true, false);
		}
	}

	/**
	 * Answers whether the given String matches the pattern.
	 * 
	 * @param string the String to test
	 * 
	 * @return whether the string matches the pattern
	 */
	private boolean match(String string) {
		if (fMatcher == null)
			return true;

		return fMatcher.match(string);
	}

	/**
	 * Take the given filter text and break it down into words using a 
	 * BreakIterator.  
	 * 
	 * @param text
	 * @return an array of words
	 */
	private String[] getWords(String text) {
		List words= new ArrayList();
		// Break the text up into words, separating based on whitespace and
		// common punctuation.
		// Previously used String.split(..., "\\W"), where "\W" is a regular
		// expression (see the Javadoc for class Pattern).
		// Need to avoid both String.split and regular expressions, in order to
		// compile against JCL Foundation (bug 80053).
		// Also need to do this in an NL-sensitive way. The use of BreakIterator
		// was suggested in bug 90579.
		BreakIterator iter= BreakIterator.getWordInstance();
		iter.setText(text);
		int i= iter.first();
		while (i != java.text.BreakIterator.DONE && i < text.length()) {
			int j= iter.following(i);
			if (j == java.text.BreakIterator.DONE) {
				j= text.length();
			}
			// match the word
			if (Character.isLetterOrDigit(text.charAt(i))) {
				String word= text.substring(i, j);
				words.add(word);
			}
			i= j;
		}
		return (String[]) words.toArray(new String[words.size()]);
	}

	/**
	 * Return whether or not if any of the words in text satisfy the
	 * match criteria.
	 * 
	 * @param text the text to match
	 * @return boolean <code>true</code> if one of the words in text 
	 * 					satisfies the match criteria.
	 */
	private boolean wordMatches(String text) {
		if (text == null)
			return false;

		//If the whole text matches we are all set
		if (match(text))
			return true;

		// Otherwise check if any of the words of the text matches
		String[] words= getWords(text);
		for (int i= 0; i < words.length; i++) {
			String word= words[i];
			if (match(word)) {
				return true;
			}
		}

		return false;
	}

}
