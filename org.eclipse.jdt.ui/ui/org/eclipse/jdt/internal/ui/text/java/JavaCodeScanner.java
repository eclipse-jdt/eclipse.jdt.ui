/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.java;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWhitespaceDetector;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jdt.ui.text.IJavaColorConstants;

import org.eclipse.jdt.internal.ui.text.AbstractJavaScanner;
import org.eclipse.jdt.internal.ui.text.CombinedWordRule;
import org.eclipse.jdt.internal.ui.text.JavaWhitespaceDetector;
import org.eclipse.jdt.internal.ui.text.JavaWordDetector;
import org.eclipse.jdt.internal.ui.text.IVersionDependent;


/**
 * A Java code scanner.
 */
public final class JavaCodeScanner extends AbstractJavaScanner {

	/**
	 * Rule to detect java operators.
	 * 
	 * @since 3.0
	 */
	protected class OperatorRule implements IRule {
	
		/** Java operators */
		private final char[] JAVA_OPERATORS= { ';', '(', ')', '{', '}', '.', '=', '/', '\\', '+', '-', '*', '[', ']', '<', '>', ':', '?', '!', ',', '|', '&', '^', '%', '~'};
		/** Token to return for this rule */
		private final IToken fToken;
	
		/**
		 * Creates a new operator rule.
		 * 
		 * @param token Token to use for this rule
		 */
		public OperatorRule(IToken token) {
			fToken= token;
		}
		
		/**
		 * Is this character an operator character?
		 * 
		 * @param character Character to determine whether it is an operator character
		 * @return <code>true</code> iff the character is an operator, <code>false</code> otherwise.
		 */
		public boolean isOperator(char character) {
			for (int index= 0; index < JAVA_OPERATORS.length; index++) {
				if (JAVA_OPERATORS[index] == character)
					return true;
			}
			return false;
		}
	
		/*
		 * @see org.eclipse.jface.text.rules.IRule#evaluate(org.eclipse.jface.text.rules.ICharacterScanner)
		 */
		public IToken evaluate(ICharacterScanner scanner) {
	
			int character= scanner.read();
			if (isOperator((char) character)) {
				do {
					character= scanner.read();
				} while (isOperator((char) character));
				scanner.unread();
				return fToken;
			} else {
				scanner.unread();
				return Token.UNDEFINED;
			}
		}
	}

	/**
	 * Word matcher to detect java method names.
	 * 
	 * @since 3.0
	 */
	protected class MethodNameMatcher extends CombinedWordRule.WordMatcher {
		
		/** Token to return for this matcher */
		private final IToken fToken;

		/**
		 * Creates a new method name matcher.
		 * 
		 * @param token Token to use for this matcher
		 */
		public MethodNameMatcher(IToken token) {
			fToken= token;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.text.CombinedWordRule.WordMatcher#evaluate(org.eclipse.jface.text.rules.ICharacterScanner, org.eclipse.jdt.internal.ui.text.CombinedWordRule.CharacterBuffer)
		 */
		public IToken evaluate(ICharacterScanner scanner, CombinedWordRule.CharacterBuffer word) {

			int count= 1;
			IToken token= Token.UNDEFINED;
			int character= scanner.read();

			// Ignore trailing whitespace
			while (Character.isWhitespace((char) character)) {
				character= scanner.read();
				++count;
			}
			
			// Check for matching parenthesis
			if (character == '(') {
				boolean isKeyword= false;
				
				// Check for keywords
				for (int index= 0; index < JavaCodeScanner.fgKeywords.length; index++) {
					if (word.equals(JavaCodeScanner.fgKeywords[index])) {
						isKeyword= true;
						break;
					}
				}
				
				if (!isKeyword) {
					scanner.unread();
					return fToken;
				}
			}

			// Unwind scanner in case of detection failure
			for (int index= 0; index < count; index++)
				scanner.unread();

			return token;
		}
	}

	private static class VersionedWordMatcher extends CombinedWordRule.WordMatcher implements IVersionDependent {

		private final IToken fDefaultToken;
		private final String fVersion;
		private boolean fIsVersionMatch;

		public VersionedWordMatcher(IToken defaultToken, String version, String currentVersion) {
			fDefaultToken= defaultToken;
			fVersion= version;
			setCurrentVersion(currentVersion);
		}
		
		/*
		 * @see org.eclipse.jdt.internal.ui.text.IVersionDependent#setCurrentVersion(java.lang.String)
		 */
		public void setCurrentVersion(String version) {
			fIsVersionMatch= fVersion.compareTo(version) <= 0;
		}
	
		/*
		 * @see org.eclipse.jdt.internal.ui.text.CombinedWordRule.WordMatcher#evaluate(org.eclipse.jface.text.rules.ICharacterScanner, org.eclipse.jdt.internal.ui.text.CombinedWordRule.CharacterBuffer)
		 */
		public IToken evaluate(ICharacterScanner scanner, CombinedWordRule.CharacterBuffer word) {
			IToken token= super.evaluate(scanner, word);

			if (fIsVersionMatch || token.isUndefined())
				return token;
			
			return fDefaultToken;
		}
	}
	
	private static class AnnotationRule implements IRule, IVersionDependent {
		private static class ResettableScanner implements ICharacterScanner {
			private final ICharacterScanner fDelegate;
			private int fReadCount;

			public ResettableScanner(final ICharacterScanner scanner) {
				fDelegate= scanner;
				fReadCount= 0;
			}
			
			/*
			 * @see org.eclipse.jface.text.rules.ICharacterScanner#getColumn()
			 */
			public int getColumn() {
				return fDelegate.getColumn();
			}
			
			/*
			 * @see org.eclipse.jface.text.rules.ICharacterScanner#getLegalLineDelimiters()
			 */
			public char[][] getLegalLineDelimiters() {
				return fDelegate.getLegalLineDelimiters();
			}
			
			/*
			 * @see org.eclipse.jface.text.rules.ICharacterScanner#read()
			 */
			public int read() {
				int ch= fDelegate.read();
				if (ch != ICharacterScanner.EOF)
					fReadCount++;
				return ch;
			}
			
			/*
			 * @see org.eclipse.jface.text.rules.ICharacterScanner#unread()
			 */
			public void unread() {
				if (fReadCount > 0)
					fReadCount--;
				fDelegate.unread();
			}
			
			/**
			 * Marks an offset in the scanned content.
			 */
			public void mark() {
				fReadCount= 0;
			}
			
			/**
			 * Resets the scanner to the marked position.
			 */
			public void reset() {
				while (fReadCount > 0)
					unread();
				
				while (fReadCount < 0)
					read();
			}
		}

		private final IWhitespaceDetector fWhitespaceDetector= new JavaWhitespaceDetector();
		private final IWordDetector fWordDetector= new JavaWordDetector();
		private final IToken fInterfaceToken;
		private final IToken fAnnotationToken;
		private final String fVersion;
		private boolean fIsVersionMatch;
		
		public AnnotationRule(IToken interfaceToken, Token annotationToken, String version, String currentVersion) {
			fInterfaceToken= interfaceToken;
			fAnnotationToken= annotationToken;
			fVersion= version;
			setCurrentVersion(currentVersion);
		}

		public IToken evaluate(ICharacterScanner scanner) {
			if (!fIsVersionMatch)
				return Token.UNDEFINED;
			
			ResettableScanner resettable= new ResettableScanner(scanner);
			if (resettable.read() == '@')
				if (skipWhitespace(resettable))
					return readAnnotation(resettable);
			
			resettable.reset();
			return Token.UNDEFINED;
		}

		private IToken readAnnotation(ResettableScanner scanner) {
			StringBuffer buffer= new StringBuffer();
			
			int ch= scanner.read();
			while (ch != ICharacterScanner.EOF && fWordDetector.isWordPart((char) ch)) {
				buffer.append((char) ch);
				ch= scanner.read();
			}
			
			if (ch != ICharacterScanner.EOF)
				scanner.unread();
			
			if ("interface".equals(buffer.toString())) //$NON-NLS-1$
				return fInterfaceToken;
			
			if (buffer.length() > 0)
				return fAnnotationToken;
			
			scanner.reset();

			return Token.UNDEFINED;
		}

		private boolean skipWhitespace(ICharacterScanner scanner) {
			while (fWhitespaceDetector.isWhitespace((char) scanner.read())) {
				// do nothing
			} 
			
			scanner.unread();
			return true;
		}

		public void setCurrentVersion(String version) {
			fIsVersionMatch= fVersion.compareTo(version) <= 0; //$NON-NLS-1$
		}
		
	}
	
	private static final String SOURCE_VERSION= JavaCore.COMPILER_SOURCE;
	
	static String[] fgKeywords= { 
		"abstract", //$NON-NLS-1$
		"break", //$NON-NLS-1$
		"case", "catch", "class", "const", "continue", //$NON-NLS-5$ //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
		"default", "do", //$NON-NLS-2$ //$NON-NLS-1$
		"else", "extends", //$NON-NLS-2$ //$NON-NLS-1$
		"final", "finally", "for", //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
		"goto", //$NON-NLS-1$
		"if", "implements", "import", "instanceof", "interface", //$NON-NLS-5$ //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
		"native", "new", //$NON-NLS-2$ //$NON-NLS-1$
		"package", "private", "protected", "public", //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
		"static", "super", "switch", "synchronized", //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
		"this", "throw", "throws", "transient", "try", //$NON-NLS-5$ //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
		"volatile", //$NON-NLS-1$
		"while" //$NON-NLS-1$
	};
	
	private static final String RETURN= "return"; //$NON-NLS-1$
	private static String[] fgJava14Keywords= { "assert" }; //$NON-NLS-1$
	private static String[] fgJava15Keywords= { "enum" }; //$NON-NLS-1$
	
	private static String[] fgTypes= { "void", "boolean", "char", "byte", "short", "strictfp", "int", "long", "float", "double" }; //$NON-NLS-1$ //$NON-NLS-5$ //$NON-NLS-7$ //$NON-NLS-6$ //$NON-NLS-8$ //$NON-NLS-9$  //$NON-NLS-10$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-2$
	
	private static String[] fgConstants= { "false", "null", "true" }; //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$

	
	private static String[] fgTokenProperties= {
		IJavaColorConstants.JAVA_KEYWORD,
		IJavaColorConstants.JAVA_STRING,
		IJavaColorConstants.JAVA_DEFAULT,
		IJavaColorConstants.JAVA_METHOD_NAME,
		IJavaColorConstants.JAVA_KEYWORD_RETURN,
		IJavaColorConstants.JAVA_OPERATOR,
		IJavaColorConstants.JAVA_ANNOTATION,
	};
	
	private List fVersionDependentRules= new ArrayList(3);
	
	/**
	 * Creates a Java code scanner
	 * 
	 * @param manager	the color manager
	 * @param store		the preference store
	 */
	public JavaCodeScanner(IColorManager manager, IPreferenceStore store) {
		super(manager, store);
		initialize();
	}
	
	/*
	 * @see AbstractJavaScanner#getTokenProperties()
	 */
	protected String[] getTokenProperties() {
		return fgTokenProperties;
	}

	/*
	 * @see AbstractJavaScanner#createRules()
	 */
	protected List createRules() {
				
		List rules= new ArrayList();		
		
		// Add rule for character constants.
		Token token= getToken(IJavaColorConstants.JAVA_STRING);
		rules.add(new SingleLineRule("'", "'", token, '\\')); //$NON-NLS-2$ //$NON-NLS-1$
				
		
		// Add generic whitespace rule.
		rules.add(new WhitespaceRule(new JavaWhitespaceDetector()));
		
		String version= getPreferenceStore().getString(SOURCE_VERSION);

		// Add JLS3 rule for /@\s*interface/
		AnnotationRule atInterfaceRule= new AnnotationRule(getToken(IJavaColorConstants.JAVA_KEYWORD), getToken(IJavaColorConstants.JAVA_ANNOTATION), "1.5", version); //$NON-NLS-1$
		rules.add(atInterfaceRule);
		fVersionDependentRules.add(atInterfaceRule);
		
		// Add word rule for new keywords, 4077
		JavaWordDetector wordDetector= new JavaWordDetector();
		token= getToken(IJavaColorConstants.JAVA_DEFAULT);
		CombinedWordRule combinedWordRule= new CombinedWordRule(wordDetector, token);
		
		token= getToken(IJavaColorConstants.JAVA_DEFAULT);
		VersionedWordMatcher j14Matcher= new VersionedWordMatcher(token, "1.4", version); //$NON-NLS-1$
		
		token= getToken(IJavaColorConstants.JAVA_KEYWORD);
		for (int i=0; i<fgJava14Keywords.length; i++)
			j14Matcher.addWord(fgJava14Keywords[i], token);

		combinedWordRule.addWordMatcher(j14Matcher);
		fVersionDependentRules.add(j14Matcher);

		token= getToken(IJavaColorConstants.JAVA_DEFAULT);
		VersionedWordMatcher j15Matcher= new VersionedWordMatcher(token, "1.5", version); //$NON-NLS-1$
		token= getToken(IJavaColorConstants.JAVA_KEYWORD);
		for (int i=0; i<fgJava15Keywords.length; i++)
			j15Matcher.addWord(fgJava15Keywords[i], token);

		combinedWordRule.addWordMatcher(j15Matcher);
		fVersionDependentRules.add(j15Matcher);

		// Add rule for operators and brackets
		token= getToken(IJavaColorConstants.JAVA_OPERATOR);
		rules.add(new OperatorRule(token));
		
		// Add word rule for keyword 'return'.
		CombinedWordRule.WordMatcher returnWordRule= new CombinedWordRule.WordMatcher();
		token= getToken(IJavaColorConstants.JAVA_KEYWORD_RETURN);
		returnWordRule.addWord(RETURN, token);  //$NON-NLS-1$
		combinedWordRule.addWordMatcher(returnWordRule);

		// Add word rule for method names.
		token= getToken(IJavaColorConstants.JAVA_METHOD_NAME);
		combinedWordRule.addWordMatcher(new MethodNameMatcher(token));
		
		// Add word rule for keywords, types, and constants.
		CombinedWordRule.WordMatcher wordRule= new CombinedWordRule.WordMatcher();
		token= getToken(IJavaColorConstants.JAVA_KEYWORD);
		for (int i=0; i<fgKeywords.length; i++)
			wordRule.addWord(fgKeywords[i], token);
		for (int i=0; i<fgTypes.length; i++)
			wordRule.addWord(fgTypes[i], token);
		for (int i=0; i<fgConstants.length; i++)
			wordRule.addWord(fgConstants[i], token);
			
		combinedWordRule.addWordMatcher(wordRule);

		rules.add(combinedWordRule);
		
		setDefaultReturnToken(getToken(IJavaColorConstants.JAVA_DEFAULT));
		return rules;
	}

	/*
	 * @see AbstractJavaScanner#affectsBehavior(PropertyChangeEvent)
	 */	
	public boolean affectsBehavior(PropertyChangeEvent event) {
		return event.getProperty().equals(SOURCE_VERSION) || super.affectsBehavior(event);
	}

	/*
	 * @see AbstractJavaScanner#adaptToPreferenceChange(PropertyChangeEvent)
	 */
	public void adaptToPreferenceChange(PropertyChangeEvent event) {
		
		if (event.getProperty().equals(SOURCE_VERSION)) {
			Object value= event.getNewValue();

			if (value instanceof String) {
				String s= (String) value;
	
				for (Iterator it= fVersionDependentRules.iterator(); it.hasNext();) {
					IVersionDependent dependent= (IVersionDependent) it.next();
					dependent.setCurrentVersion(s);
				}
			}
			
		} else if (super.affectsBehavior(event)) {
			super.adaptToPreferenceChange(event);
		}
	}
}
