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
package org.eclipse.jdt.internal.ui.text.javadoc;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Preferences;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;

import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jdt.ui.text.IJavaColorConstants;

import org.eclipse.jdt.internal.ui.text.CombinedWordRule;
import org.eclipse.jdt.internal.ui.text.JavaCommentScanner;
import org.eclipse.jdt.internal.ui.text.JavaWhitespaceDetector;
import org.eclipse.jdt.internal.ui.text.CombinedWordRule.WordMatcher;

/**
 * A rule based JavaDoc scanner.
 */
public final class JavaDocScanner extends JavaCommentScanner {
		
		
	/**
	 * Detector for HTML comment delimiters.
	 */
	static class HTMLCommentDetector implements IWordDetector {

		/**
		 * @see IWordDetector#isWordStart(char)
		 */
		public boolean isWordStart(char c) {
			return (c == '<' || c == '-');
		}

		/**
		 * @see IWordDetector#isWordPart(char)
		 */
		public boolean isWordPart(char c) {
			return (c == '-' || c == '!' || c == '>');
		}
	}
	
	class TagRule extends SingleLineRule {
		
		/*
		 * @see SingleLineRule
		 */
		public TagRule(IToken token) {
			super("<", ">", token, (char) 0); //$NON-NLS-2$ //$NON-NLS-1$
		}
		
		/*
		 * @see SingleLineRule 
		 */
		public TagRule(IToken token, char escapeCharacter) {
			super("<", ">", token, escapeCharacter); //$NON-NLS-2$ //$NON-NLS-1$
		}
		
		private IToken evaluateToken() {
			try {
				final String token= getDocument().get(getTokenOffset(), getTokenLength()) + "."; //$NON-NLS-1$

				int offset= 0;
				char character= token.charAt(++offset);

				if (character == '/')
					character= token.charAt(++offset);

				while (Character.isWhitespace(character))
					character= token.charAt(++offset);

				while (Character.isLetterOrDigit(character))
					character= token.charAt(++offset);

				while (Character.isWhitespace(character))
					character= token.charAt(++offset);

				if (offset >= 2 && token.charAt(offset) == fEndSequence[0])
					return fToken;

			} catch (BadLocationException exception) {
				// Do nothing
			}
			return getToken(IJavaColorConstants.JAVADOC_DEFAULT);
		}
				
		/*
		 * @see PatternRule#evaluate(ICharacterScanner)
		 */
		public IToken evaluate(ICharacterScanner scanner) {
			IToken result= super.evaluate(scanner);
			if (result == fToken)
				return evaluateToken();
			return result;
		}
	}
	
	private static String[] fgKeywords= {"@author", "@deprecated", "@exception", "@inheritDoc", "@param", "@return", "@see", "@serial", "@serialData", "@serialField", "@since", "@throws", "@version"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$
	
	private static String[] fgTokenProperties= {
		IJavaColorConstants.JAVADOC_KEYWORD,
		IJavaColorConstants.JAVADOC_TAG,
		IJavaColorConstants.JAVADOC_LINK,
		IJavaColorConstants.JAVADOC_DEFAULT,
		TASK_TAG
	};			
	
	
	public JavaDocScanner(IColorManager manager, IPreferenceStore store, Preferences coreStore) {
		super(manager, store, coreStore, IJavaColorConstants.JAVADOC_DEFAULT, fgTokenProperties);
	}
	
	/**
	 * Initialize with the given arguments
	 * @param manager	Color manager
	 * @param store	Preference store
	 * 
	 * @since 3.0
	 */
	public JavaDocScanner(IColorManager manager, IPreferenceStore store) {
		this(manager, store, null);
	}

	public IDocument getDocument() {
		return fDocument;
	}
	
	/*
	 * @see AbstractJavaScanner#createRules()
	 */
	protected List createRules() {
		
		List list= new ArrayList();
		
		// Add rule for tags.
		Token token= getToken(IJavaColorConstants.JAVADOC_TAG);
		list.add(new TagRule(token));
		
		
		// Add rule for HTML comments
		WordRule wordRule= new WordRule(new HTMLCommentDetector(), token);
		wordRule.addWord("<!--", token); //$NON-NLS-1$
		wordRule.addWord("--!>", token); //$NON-NLS-1$
		list.add(wordRule);
		
		
		// Add rule for links.
		token= getToken(IJavaColorConstants.JAVADOC_LINK);
		list.add(new SingleLineRule("{@link", "}", token)); //$NON-NLS-2$ //$NON-NLS-1$
		
		
		// Add generic whitespace rule.
		list.add(new WhitespaceRule(new JavaWhitespaceDetector()));
		
		
		list.addAll(super.createRules());
		return list;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.text.JavaCommentScanner#createMatchers()
	 */
	protected List createMatchers() {
		List list= super.createMatchers();
		
		// Add word rule for keywords.
		WordMatcher matcher= new CombinedWordRule.WordMatcher();
		IToken token= getToken(IJavaColorConstants.JAVADOC_KEYWORD);
		for (int i= 0; i < fgKeywords.length; i++)
			matcher.addWord(fgKeywords[i], token);
		list.add(matcher);
		
		return list;
	}
}


