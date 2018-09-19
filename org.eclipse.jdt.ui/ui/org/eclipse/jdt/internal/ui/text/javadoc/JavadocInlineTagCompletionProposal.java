/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.text.javadoc;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.viewers.StyledString;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.BoldStylerProvider;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal;

/**
 * Completions of inline tags such as &#x7b;&#x40;link &#x7d;. See {@link CompletionProposal#JAVADOC_INLINE_TAG}.
 *
 * @since 3.2
 */
public class JavadocInlineTagCompletionProposal extends LazyJavaCompletionProposal {
	/** Triggers for types in javadoc. Do not modify. */
	protected static final char[] JDOC_INLINE_TAG_TRIGGERS= new char[] { '#', '}', ' ' };

	public JavadocInlineTagCompletionProposal(CompletionProposal proposal, JavaContentAssistInvocationContext context) {
		super(proposal, context);
		Assert.isTrue(isInJavadoc());
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal#computeReplacementString()
	 */
	@Override
	protected String computeReplacementString() {
		String replacement= super.computeReplacementString();
		// TODO respect the auto-close preference, but do so consistently with method completions
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=113544
//		if (!autocloseBrackets() && replacement.endsWith("}")) //$NON-NLS-1$
//			return replacement.substring(0, replacement.length() - 1);
		return replacement;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.LazyJavaTypeCompletionProposal#apply(org.eclipse.jface.text.IDocument, char, int)
	 */
	@Override
	public void apply(IDocument document, char trigger, int offset) {
		// TODO respect the auto-close preference, but do so consistently with method completions
		// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=113544
//		boolean needsLinkedMode= autocloseBrackets();
		boolean needsLinkedMode= true;
		if (needsLinkedMode)
			setCursorPosition(getCursorPosition() - 1); // before the closing curly brace

		super.apply(document, trigger, offset);

		if (needsLinkedMode)
			setUpLinkedMode(document, '}');
	}

	@Override
	protected boolean isPrefix(String pattern, String string) {
		if (string.charAt(0) == '{') {
			string= string.substring(1);
		}
		if (pattern.charAt(0) == '{') {
			pattern= pattern.substring(1);
		}
		return super.isPrefix(pattern, string);
	}

	@Override
	public StyledString getStyledDisplayString(IDocument document, int offset, BoldStylerProvider boldStylerProvider) {
		StyledString styledDisplayString= new StyledString();
		styledDisplayString.append(getStyledDisplayString());

		String pattern= getPatternToEmphasizeMatch(document, offset);
		if (pattern != null && pattern.length() > 0) {
			String displayString= styledDisplayString.getString().substring(1); // remove '{'
			boolean patternHasBrace= pattern.charAt(0) == '{';
			if (patternHasBrace) {
				pattern= pattern.substring(1);
			}
			if (displayString.charAt(0) == '@' && pattern.charAt(0) == '@') {
				displayString= displayString.substring(1);
				pattern= pattern.substring(1);
				int patternMatchRule= getPatternMatchRule(pattern, displayString);
				int[] matchingRegions= SearchPattern.getMatchingRegions(pattern, displayString, patternMatchRule);
				if (matchingRegions != null) {
					if (patternHasBrace) {
						Strings.markMatchingRegions(styledDisplayString, 0, new int[] { 0, 1 }, boldStylerProvider.getBoldStyler());
					}
					Strings.markMatchingRegions(styledDisplayString, 0, new int[] { 1, 1 }, boldStylerProvider.getBoldStyler());
					for (int i= 0; i < matchingRegions.length; i+= 2) {
						matchingRegions[i]+= 2;
					}
				}
				Strings.markMatchingRegions(styledDisplayString, 0, matchingRegions, boldStylerProvider.getBoldStyler());
			}
		}
		return styledDisplayString;
	}

}
