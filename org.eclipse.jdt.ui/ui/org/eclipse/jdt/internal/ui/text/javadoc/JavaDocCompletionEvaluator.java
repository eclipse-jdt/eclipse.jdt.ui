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
package org.eclipse.jdt.internal.ui.text.javadoc;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.IContextInformation;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IJavadocCompletionProcessor;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposal;

public class JavaDocCompletionEvaluator implements IJavadocCompletionProcessor, IJavaDocTagConstants, IHtmlTagConstants {

	private static final String[] fgHTMLProposals= new String[HTML_GENERAL_TAGS.length * 2];
	{
		String tag= null;

		int index= 0;
		int offset= 0;

		while (index < fgHTMLProposals.length) {

			tag= HTML_GENERAL_TAGS[offset];
			fgHTMLProposals[index++]= HTML_TAG_PREFIX + tag + HTML_TAG_POSTFIX;
			fgHTMLProposals[index++]= HTML_CLOSE_PREFIX + tag + HTML_TAG_POSTFIX;
			offset++;
		}
	}

	private IDocument fDocument;
	private int fCurrentPos;
	private int fCurrentLength;
	private String fErrorMessage;
	private List fResult;

	private boolean fRestrictToMatchingCase;

	public JavaDocCompletionEvaluator() {
		fResult= new ArrayList();
	}

	private static boolean isWordPart(char ch) {
		return Character.isJavaIdentifierPart(ch) || (ch == '#') || (ch == '.') || (ch == '/');
	}

	private static int findCharBeforeWord(IDocument doc, int lineBeginPos, int pos) {
		int currPos= pos - 1;
		if (currPos > lineBeginPos) {
			try {
				while (currPos > lineBeginPos && isWordPart(doc.getChar(currPos))) {
					currPos--;
				}
				return currPos;
			} catch (BadLocationException e) {
				// ignore
			}
		}
		return pos;
	}

	private static int findClosingCharacter(IDocument doc, int pos, int end, char endChar) throws BadLocationException {
		int curr= pos;
		while (curr < end && (doc.getChar(curr) != endChar)) {
			curr++;
		}
		if (curr < end) {
			return curr + 1;
		}
		return pos;
	}

	private static int findReplaceEndPos(IDocument doc, String newText, String oldText, int pos) {
		if (oldText.length() == 0 || oldText.equals(newText)) {
			return pos;
		}

		try {
			IRegion lineInfo= doc.getLineInformationOfOffset(pos);
			int end= lineInfo.getOffset() + lineInfo.getLength();

			// for html, search the tag end character
			return findClosingCharacter(doc, pos, end, '>');
		} catch (BadLocationException e) {
			// ignore
		}
		return pos;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IJavaDocCompletionProcessor#computeCompletionProposals(org.eclipse.jdt.core.ICompilationUnit, int, int, int)
	 */
	public IJavaCompletionProposal[] computeCompletionProposals(ICompilationUnit cu, int offset, int length, int flags) {
		fCurrentPos= offset;
		fCurrentLength= length;
		fRestrictToMatchingCase= (flags & RESTRICT_TO_MATCHING_CASE) != 0;

		IEditorInput editorInput= new FileEditorInput((IFile) cu.getResource());
		fDocument= JavaUI.getDocumentProvider().getDocument(editorInput);
		if (fDocument == null) {
			return null;
		}

		try {
			evalProposals();
			return (JavaCompletionProposal[]) fResult.toArray(new JavaCompletionProposal[fResult.size()]);
		} catch (JavaModelException e) {
			fErrorMessage= e.getLocalizedMessage();
		} finally {
			fResult.clear();
		}
		return null;
	}

	private void evalProposals() throws JavaModelException {
		try {

			IRegion info= fDocument.getLineInformationOfOffset(fCurrentPos);
			int lineBeginPos= info.getOffset();

			int word1Begin= findCharBeforeWord(fDocument, lineBeginPos, fCurrentPos);
			if (word1Begin == fCurrentPos)
				return;
			
			char firstChar= fDocument.getChar(word1Begin);
			if (firstChar == '<') {
				String prefix= fDocument.get(word1Begin, fCurrentPos - word1Begin);
				addProposals(prefix, fgHTMLProposals, JavaPluginImages.IMG_OBJS_HTMLTAG);
				return;
			} else if (!Character.isWhitespace(firstChar)) {
				return;
			}

			// TODO really show all tags when there is no prefix?
			// TODO find any unclosed open tag and offer the corresponding close tag
			String prefix= fDocument.get(word1Begin + 1, fCurrentPos - word1Begin - 1);
			addAllTags(prefix);
		} catch (BadLocationException e) {
			// ignore
		}
	}

	private boolean prefixMatches(String prefix, String proposal) {
		if (fRestrictToMatchingCase) {
			return proposal.startsWith(prefix);
		} else if (proposal.length() >= prefix.length()) {
			return prefix.equalsIgnoreCase(proposal.substring(0, prefix.length()));
		}
		return false;
	}

	private void addAllTags(String prefix) {
		String htmlPrefix= "<" + prefix; //$NON-NLS-1$
		for (int i= 0; i < fgHTMLProposals.length; i++) {
			String curr= fgHTMLProposals[i];
			if (prefixMatches(htmlPrefix, curr)) {
				fResult.add(createCompletion(curr, prefix, curr, JavaPluginImages.get(JavaPluginImages.IMG_OBJS_HTMLTAG), 0));
			}
		}
	}

	private void addProposals(String prefix, String[] choices, String imageName) {
		for (int i= 0; i < choices.length; i++) {
			String curr= choices[i];
			if (prefixMatches(prefix, curr)) {
				fResult.add(createCompletion(curr, prefix, curr, JavaPluginImages.get(imageName), 0));
			}
		}
	}

	private JavaCompletionProposal createCompletion(String newText, String oldText, String labelText, Image image, int severity) {
		int offset= fCurrentPos - oldText.length();
		int length= fCurrentLength + oldText.length();
		if (fCurrentLength == 0)
			length= findReplaceEndPos(fDocument, newText, oldText, fCurrentPos) - offset;

		JavaCompletionProposal proposal= new JavaCompletionProposal(newText, offset, length, image, labelText, severity) {
			/*
			 * @see org.eclipse.jdt.internal.ui.text.java.AbstractJavaCompletionProposal#isInJavadoc()
			 */
			protected boolean isInJavadoc() {
				return true;
			}
		};
		proposal.setTriggerCharacters( new char[] { '>' });
		return proposal;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IJavaDocCompletionProcessor#computeContextInformation(org.eclipse.jdt.core.ICompilationUnit, int)
	 */
	public IContextInformation[] computeContextInformation(ICompilationUnit cu, int offset) {
		fErrorMessage= null;
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IJavaDocCompletionProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		return fErrorMessage;
	}
}
