package org.eclipse.jdt.internal.ui.text.java;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;

import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jdt.ui.text.IJavaColorConstants;

import org.eclipse.jdt.internal.ui.text.JavaWhitespaceDetector;
import org.eclipse.jdt.internal.ui.text.JavaWordDetector;


/**
 * A Java code scanner.
 */
public class JavaCodeScanner extends RuleBasedScanner {
	
	
	private static String[] fgKeywords= { 
		"abstract",
		"break",
		"case", "catch", "class", "continue",
		"default", "do",
		"else", "extends",
		"final", "finally", "for",
		"if", "implements", "import", "instanceof", "interface",
		"native", "new",
		"package", "private", "protected", "public",
		"return",
		"static", "super", "switch", "synchronized",
		"this", "throw", "throws", "transient", "try",
		"volatile",
		"while"
	};
	
	private static String[] fgTypes= { "void", "boolean", "char", "byte", "short", "int", "long", "float", "double" };
	
	private static String[] fgConstants= { "false", "null", "true" };

	
	private TextAttribute fComment;
	private TextAttribute fKeyword;
	private TextAttribute fType;
	private TextAttribute fString;
	private IColorManager fColorManager;
	
	/**
	 * Creates a Java code scanner
	 */
	public JavaCodeScanner(IColorManager manager) {
	
		IToken keyword= new Token(new TextAttribute(manager.getColor(IJavaColorConstants.JAVA_KEYWORD)));
		IToken type= new Token(new TextAttribute(manager.getColor(IJavaColorConstants.JAVA_TYPE)));
		IToken string= new Token(new TextAttribute(manager.getColor(IJavaColorConstants.JAVA_STRING)));
		IToken comment= new Token(new TextAttribute(manager.getColor(IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT)));
		IToken other= new Token(new TextAttribute(manager.getColor(IJavaColorConstants.JAVA_DEFAULT)));
		
				
		List rules= new ArrayList();
				
		// Add rule for single line comments.
		rules.add(new EndOfLineRule("//", comment));
		
		// Add rule for strings and character constants.
		rules.add(new SingleLineRule("\"", "\"", string, '\\'));
		rules.add(new SingleLineRule("'", "'", string, '\\'));
				
		// Add generic whitespace rule.
		rules.add(new WhitespaceRule(new JavaWhitespaceDetector()));
		
		// Add word rule for keywords, types, and constants.
		WordRule wordRule= new WordRule(new JavaWordDetector(), other);
		for (int i=0; i<fgKeywords.length; i++)
			wordRule.addWord(fgKeywords[i], keyword);
		for (int i=0; i<fgTypes.length; i++)
			wordRule.addWord(fgTypes[i], type);
		for (int i=0; i<fgConstants.length; i++)
			wordRule.addWord(fgConstants[i], type);
		rules.add(wordRule);
		
		
		IRule[] result= new IRule[rules.size()];
		rules.toArray(result);
		setRules(result);
	}
}