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


import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jdt.ui.text.IJavaColorConstants;
import org.eclipse.jdt.internal.ui.text.AbstractJavaScanner;
import org.eclipse.jdt.internal.ui.text.JavaWhitespaceDetector;
import org.eclipse.jdt.internal.ui.text.JavaWordDetector;


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
	 * Word rule to detect java method names.
	 * 
	 * @since 3.0
	 */
	protected class MethodNameRule implements IRule {
		
		/** Token to return for this rule */
		private final IToken fToken;
		/** Detector to determine the method names */
		private final IWordDetector fDetector;

		/**
		 * Creates a new method name rule.
		 * 
		 * @param detector Detector to detect the method names
		 * @param token Token to use for this rule
		 */
		public MethodNameRule(IWordDetector detector, IToken token) {
			fDetector= detector;
			fToken= token;
		}

		/*
		 * @see org.eclipse.jface.text.rules.IRule#evaluate(org.eclipse.jface.text.rules.ICharacterScanner)
		 */
		public IToken evaluate(ICharacterScanner scanner) {

			int count= 1;
			IToken token= Token.UNDEFINED;
			int character= scanner.read();
			final StringBuffer buffer= new StringBuffer(32); // Average word length?

			// Scan for valid word start
			if (fDetector.isWordStart((char) character)) {

				do {
					buffer.append((char) character);
					character= scanner.read();
					++count;
				} while (fDetector.isWordPart((char) character));
				
				// Ignore trailing whitespaces
				while (Character.isWhitespace((char) character)) {
					character= scanner.read();
					++count;
				}
				
				// Check for matching parenthesis
				if (character == '(') {
					boolean isKeyword= false;
					final String word= buffer.toString();
					
					// Check for keywords
					for (int index= 0; index < JavaCodeScanner.fgKeywords.length; index++) {
						if (JavaCodeScanner.fgKeywords[index].equals(word)) {
							isKeyword= true;
							break;
						}
					}
					
					if (!isKeyword) {
						scanner.unread();
						return fToken;
					}
				}
			}

			// Unwind scanner in case of detection failure
			for (int index= 0; index < count; index++)
				scanner.unread();

			return token;
		}
	}

	private static class VersionedWordRule extends WordRule {

		private final IToken fDefaultToken;
		private final String fVersion;
		private final boolean fEnable;
		
		private String fCurrentVersion;

		public VersionedWordRule(IWordDetector detector, IToken defaultToken, String version, boolean enable, String currentVersion) {
			super(detector);

			fDefaultToken= defaultToken;
			fVersion= version;
			fEnable= enable;
			fCurrentVersion= currentVersion;
		}
		
		public void setCurrentVersion(String version) {
			fCurrentVersion= version;
		}
	
		/*
		 * @see IRule#evaluate
		 */
		public IToken evaluate(ICharacterScanner scanner) {
			IToken token= super.evaluate(scanner);

			if (fEnable) {
				if (fCurrentVersion.equals(fVersion) || token.isUndefined())
					return token;
				return fDefaultToken;
			} else {
				if (fCurrentVersion.equals(fVersion))
					return Token.UNDEFINED;
					
				return token;
			}
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
	
	private static String[] fgNewKeywords= { "assert" }; //$NON-NLS-1$
	
	private static String[] fgTypes= { "void", "boolean", "char", "byte", "short", "strictfp", "int", "long", "float", "double" }; //$NON-NLS-1$ //$NON-NLS-5$ //$NON-NLS-7$ //$NON-NLS-6$ //$NON-NLS-8$ //$NON-NLS-9$  //$NON-NLS-10$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-2$
	
	private static String[] fgConstants= { "false", "null", "true" }; //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$

	
	private static String[] fgTokenProperties= {
		IJavaColorConstants.JAVA_KEYWORD,
		IJavaColorConstants.JAVA_STRING,
		IJavaColorConstants.JAVA_DEFAULT,
		IJavaColorConstants.JAVA_METHOD_NAME,
		IJavaColorConstants.JAVA_KEYWORD_RETURN,
		IJavaColorConstants.JAVA_OPERATOR
	};
	
	private VersionedWordRule fVersionedWordRule;

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
		
		
		// Add word rule for new keywords, 4077
		String version= getPreferenceStore().getString(SOURCE_VERSION);
		token= getToken(IJavaColorConstants.JAVA_DEFAULT);
		fVersionedWordRule= new VersionedWordRule(new JavaWordDetector(), token, "1.4", true, version); //$NON-NLS-1$
		
		token= getToken(IJavaColorConstants.JAVA_KEYWORD);
		for (int i=0; i<fgNewKeywords.length; i++)
			fVersionedWordRule.addWord(fgNewKeywords[i], token);

		rules.add(fVersionedWordRule);

		// Add rule for operators and brackets
		token= getToken(IJavaColorConstants.JAVA_OPERATOR);
		rules.add(new OperatorRule(token));
		
		
		// Add word rule for keyword 'return'.
		WordRule returnWordRule= new WordRule(new JavaWordDetector(), Token.UNDEFINED);
		token= getToken(IJavaColorConstants.JAVA_KEYWORD_RETURN);
		returnWordRule.addWord("return", token);  //$NON-NLS-1$
		rules.add(returnWordRule);

		// Add word rule for method names.
		token= getToken(IJavaColorConstants.JAVA_METHOD_NAME);
		rules.add(new MethodNameRule(new JavaWordDetector(), token));
		
		// Add word rule for keywords, types, and constants.
		token= getToken(IJavaColorConstants.JAVA_DEFAULT);
		WordRule wordRule= new WordRule(new JavaWordDetector(), token);
		token= getToken(IJavaColorConstants.JAVA_KEYWORD);
		for (int i=0; i<fgKeywords.length; i++)
			wordRule.addWord(fgKeywords[i], token);
		for (int i=0; i<fgTypes.length; i++)
			wordRule.addWord(fgTypes[i], token);
		for (int i=0; i<fgConstants.length; i++)
			wordRule.addWord(fgConstants[i], token);
			
		rules.add(wordRule);

		
		setDefaultReturnToken(getToken(IJavaColorConstants.JAVA_DEFAULT));
		return rules;
	}

	/*
	 * @see RuleBasedScanner#setRules(IRule[])
	 */
	public void setRules(IRule[] rules) {
		int i;
		for (i= 0; i < rules.length; i++)
			if (rules[i].equals(fVersionedWordRule))
				break;

		// not found - invalidate fVersionedWordRule
		if (i == rules.length)
			fVersionedWordRule= null;
		
		super.setRules(rules);	
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
	
				if (fVersionedWordRule != null)
					fVersionedWordRule.setCurrentVersion(s);			
			}
			
		} else if (super.affectsBehavior(event)) {
			super.adaptToPreferenceChange(event);
		}
	}
}
