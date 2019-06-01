/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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

import org.eclipse.osgi.util.TextProcessor;

import org.eclipse.swt.graphics.Image;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.viewers.StyledString;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.BoldStylerProvider;

import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;


public class JavaCompletionProposal extends AbstractJavaCompletionProposal {

	/**
	 * Creates a new completion proposal. All fields are initialized based on the provided
	 * information.
	 *
	 * @param replacementString the actual string to be inserted into the document
	 * @param replacementOffset the offset of the text to be replaced
	 * @param replacementLength the length of the text to be replaced
	 * @param image the image to display for this proposal
	 * @param displayString the string to be displayed for the proposal If set to <code>null</code>, the replacement string will be taken as display string.
	 * @param relevance the relevance
	 */
	public JavaCompletionProposal(String replacementString, int replacementOffset, int replacementLength, Image image, String displayString, int relevance) {
		this(replacementString, replacementOffset, replacementLength, image, new StyledString(displayString), relevance, false);
	}

	/**
	 * Creates a new completion proposal. All fields are initialized based on the provided
	 * information.
	 *
	 * @param replacementString the actual string to be inserted into the document
	 * @param replacementOffset the offset of the text to be replaced
	 * @param replacementLength the length of the text to be replaced
	 * @param image the image to display for this proposal
	 * @param displayString the string to be displayed for the proposal If set to <code>null</code>, the replacement string will be taken as display string.
	 * @param relevance the relevance
	 */
	public JavaCompletionProposal(String replacementString, int replacementOffset, int replacementLength, Image image, StyledString displayString, int relevance) {
		this(replacementString, replacementOffset, replacementLength, image, displayString, relevance, false);
	}

	/**
	 * Creates a new completion proposal. All fields are initialized based on the provided
	 * information.
	 *
	 * @param replacementString the actual string to be inserted into the document
	 * @param replacementOffset the offset of the text to be replaced
	 * @param replacementLength the length of the text to be replaced
	 * @param image the image to display for this proposal
	 * @param displayString the string to be displayed for the proposal If set to <code>null</code>,
	 *        the replacement string will be taken as display string.
	 * @param relevance the relevance
	 * @param inJavadoc <code>true</code> for a javadoc proposal
	 */
	public JavaCompletionProposal(String replacementString, int replacementOffset, int replacementLength, Image image, StyledString displayString, int relevance, boolean inJavadoc) {
		this(replacementString, replacementOffset, replacementLength, image, displayString, relevance, inJavadoc, null);
	}

	/**
	 * Creates a new completion proposal. All fields are initialized based on the provided
	 * information.
	 *
	 * @param replacementString the actual string to be inserted into the document
	 * @param replacementOffset the offset of the text to be replaced
	 * @param replacementLength the length of the text to be replaced
	 * @param image the image to display for this proposal
	 * @param displayString the string to be displayed for the proposal If set to <code>null</code>,
	 *        the replacement string will be taken as display string.
	 * @param relevance the relevance
	 * @param inJavadoc <code>true</code> for a javadoc proposal
	 * @param invocationContext the invocation context of this completion proposal or <code>null</code> not available
	 */
	public JavaCompletionProposal(String replacementString, int replacementOffset, int replacementLength, Image image, StyledString displayString, int relevance, boolean inJavadoc, JavaContentAssistInvocationContext invocationContext) {
		super(invocationContext);
		Assert.isNotNull(replacementString);
		Assert.isTrue(replacementOffset >= 0);
		Assert.isTrue(replacementLength >= 0);

		setReplacementString(replacementString);
		setReplacementOffset(replacementOffset);
		setReplacementLength(replacementLength);
		setImage(image);
		setStyledDisplayString(displayString == null ? new StyledString(replacementString) : displayString);
		setRelevance(relevance);
		setCursorPosition(replacementString.length());
		setInJavadoc(inJavadoc);
		setSortString(displayString == null ? replacementString : displayString.getString());
	}

	@Override
	protected boolean isValidPrefix(String prefix) {
		String word= TextProcessor.deprocess(getDisplayString());
		if (isInJavadoc()) {
			if (word.indexOf("{@link ") == 0) { //$NON-NLS-1$
				word= word.substring(7);
			} else if (word.indexOf("{@value ") == 0) { //$NON-NLS-1$
				word= word.substring(8);
			} else if (word.indexOf('<') == 0) {
				word= word.substring(1);
				boolean isClosing= word.indexOf('/') == 0;
				if (isClosing) {
					word= word.substring(1);
				}
				if (prefix.indexOf('<') == 0) {
					prefix= prefix.substring(1);
					if (isClosing && prefix.indexOf('/') == 0) {
						prefix= prefix.substring(1);
					}
				}
			}
		} else if (word.contains("this.")) { //$NON-NLS-1$
			word= word.substring(word.indexOf("this.") + 5); //$NON-NLS-1$
		}
		return isPrefix(prefix, word);
	}

	@Override
	public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
		String string= getReplacementString();
		int pos= string.indexOf('(');
		if (pos > 0)
			return string.subSequence(0, pos);
		else if (string.startsWith("this.")) //$NON-NLS-1$
			return string.substring(5);
		else
			return string;
	}

	@Override
	public StyledString getStyledDisplayString(IDocument document, int offset, BoldStylerProvider boldStylerProvider) {
		StyledString styledDisplayString= new StyledString();
		styledDisplayString.append(getStyledDisplayString());
		String displayString= styledDisplayString.getString();
		if (isInJavadoc() && displayString.indexOf('<') == 0) {
			displayString= displayString.substring(1);
			boolean isClosing= displayString.indexOf('/') == 0;
			if (isClosing) {
				displayString= displayString.substring(1);
			}
			String pattern= getPatternToEmphasizeMatch(document, offset);
			if (pattern != null && pattern.length() > 0) {
				if (pattern.indexOf('<') == 0) {
					pattern= pattern.substring(1);
					if (isClosing && pattern.indexOf('/') == 0) {
						pattern= pattern.substring(1);
						Strings.markMatchingRegions(styledDisplayString, 0, new int[] { 1, 1 }, boldStylerProvider.getBoldStyler());
					}
					Strings.markMatchingRegions(styledDisplayString, 0, new int[] { 0, 1 }, boldStylerProvider.getBoldStyler());
				}
				int patternMatchRule= getPatternMatchRule(pattern, displayString);
				int[] matchingRegions= SearchPattern.getMatchingRegions(pattern, displayString, patternMatchRule);
				if (matchingRegions != null) {
					int inc= isClosing ? 2 : 1;
					for (int i= 0; i < matchingRegions.length; i+= 2) {
						matchingRegions[i]= matchingRegions[i] + inc;
					}
				}
				Strings.markMatchingRegions(styledDisplayString, 0, matchingRegions, boldStylerProvider.getBoldStyler());
			}
			return styledDisplayString;
		}
		return super.getStyledDisplayString(document, offset, boldStylerProvider);
	}
}
