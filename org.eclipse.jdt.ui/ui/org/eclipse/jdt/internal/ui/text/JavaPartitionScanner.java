package org.eclipse.jdt.internal.ui.text;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;


/**
 * This scanner recognizes the JavaDoc comments and Java multi line comments.
 */
public class JavaPartitionScanner extends BufferedRuleBasedScanner {

	private final static String SKIP= "__skip"; //$NON-NLS-1$
	
	public final static String JAVA_SINGLE_LINE_COMMENT= "__java_singleline_comment";
	public final static String JAVA_MULTI_LINE_COMMENT= "__java_multiline_comment"; //$NON-NLS-1$
	public final static String JAVA_DOC= "__java_javadoc"; //$NON-NLS-1$

	/**
	 * Detector for empty comments.
	 */
	static class EmptyCommentDetector implements IWordDetector {

		/**
		 * @see IWordDetector#isWordStart
		 */
		public boolean isWordStart(char c) {
			return (c == '/');
		}

		/**
		 * @see IWordDetector#isWordPart
		 */
		public boolean isWordPart(char c) {
			return (c == '*' || c == '/');
		}
	};

	/**
	 * Creates the partitioner and sets up the appropriate rules.
	 */
	public JavaPartitionScanner() {
		super();
		
		IToken skip= new Token(SKIP);
		IToken javaDoc= new Token(JAVA_DOC);
		IToken multiLineComment= new Token(JAVA_MULTI_LINE_COMMENT);
		IToken singleLineComment= new Token(JAVA_SINGLE_LINE_COMMENT);

		List rules= new ArrayList();

		// Add rule for single line comments.
		rules.add(new EndOfLineRule("//", singleLineComment)); //$NON-NLS-1$

		// Add rule for strings and character constants.
		rules.add(new SingleLineRule("\"", "\"", skip, '\\')); //$NON-NLS-2$ //$NON-NLS-1$
		rules.add(new SingleLineRule("'", "'", skip, '\\')); //$NON-NLS-2$ //$NON-NLS-1$

		// Add special case word rule.
		WordRule wordRule= new WordRule(new EmptyCommentDetector());
		wordRule.addWord("/**/", multiLineComment); //$NON-NLS-1$
		rules.add(wordRule);

		// Add rules for multi-line comments and javadoc.
		rules.add(new MultiLineRule("/**", "*/", javaDoc)); //$NON-NLS-1$ //$NON-NLS-2$
		rules.add(new MultiLineRule("/*", "*/", multiLineComment)); //$NON-NLS-1$ //$NON-NLS-2$

		IRule[] result= new IRule[rules.size()];
		rules.toArray(result);
		setRules(result);
	}
}
