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

import org.eclipse.swt.SWT;
import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jdt.ui.text.IJavaColorConstants;

import org.eclipse.jdt.internal.ui.text.JavaWhitespaceDetector;
import org.eclipse.jdt.internal.ui.text.JavaWordDetector;


/**
 * A Java code scanner.
 */
public class JavaCodeScanner extends RuleBasedScanner {
	
	
	private static String[] fgKeywords= { 
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
		"return", //$NON-NLS-1$
		"static", "super", "switch", "synchronized", //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
		"this", "throw", "throws", "transient", "try", //$NON-NLS-5$ //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
		"volatile", //$NON-NLS-1$
		"while" //$NON-NLS-1$
	};
	
	private static String[] fgTypes= { "void", "boolean", "char", "byte", "short", "strictfp", "int", "long", "float", "double" }; //$NON-NLS-1$ //$NON-NLS-5$ //$NON-NLS-7$ //$NON-NLS-6$ //$NON-NLS-8$ //$NON-NLS-9$  //$NON-NLS-10$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-2$
	
	private static String[] fgConstants= { "false", "null", "true" }; //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$

	
	private Token fKeyword;
	private Token fType;
	private Token fString;
	private Token fComment;
	
	private IColorManager fColorManager;
		
	/**
	 * Creates a Java code scanner
	 */
	public JavaCodeScanner(IColorManager manager) {
		super();
		
		setDefaultReturnToken(new Token(new TextAttribute(manager.getColor(IJavaColorConstants.JAVA_DEFAULT))));
		
		fColorManager= manager;
		
		fKeyword= new Token(new TextAttribute(fColorManager.getColor(IJavaColorConstants.JAVA_KEYWORD), null, SWT.BOLD));
		fType= new Token(new TextAttribute(fColorManager.getColor(IJavaColorConstants.JAVA_TYPE), null, SWT.BOLD));
		fString= new Token(new TextAttribute(fColorManager.getColor(IJavaColorConstants.JAVA_STRING)));
		fComment= new Token(new TextAttribute(fColorManager.getColor(IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT)));
		
		initializeRules();
		
	}
	
	private void initializeRules() {
		List rules= new ArrayList();
		
		// Add rule for single line comments.
		rules.add(new EndOfLineRule("//", fComment)); //$NON-NLS-1$
		
		// Add rule for strings and character constants.
		rules.add(new SingleLineRule("\"", "\"", fString, '\\')); //$NON-NLS-2$ //$NON-NLS-1$
		rules.add(new SingleLineRule("'", "'", fString, '\\')); //$NON-NLS-2$ //$NON-NLS-1$
				
		// Add generic whitespace rule.
		rules.add(new WhitespaceRule(new JavaWhitespaceDetector()));
		
		// Add word rule for keywords, types, and constants.
		WordRule wordRule= new WordRule(new JavaWordDetector(), getDefaultReturnToken());
		for (int i=0; i<fgKeywords.length; i++)
			wordRule.addWord(fgKeywords[i], fKeyword);
		for (int i=0; i<fgTypes.length; i++)
			wordRule.addWord(fgTypes[i], fType);
		for (int i=0; i<fgConstants.length; i++)
			wordRule.addWord(fgConstants[i], fType);
		rules.add(wordRule);
		
		IRule[] result= new IRule[rules.size()];
		rules.toArray(result);
		setRules(result);
	}
	
	public void colorManagerChanged() {
		
		IToken token= getDefaultReturnToken();
		if (token instanceof Token) 
			((Token) token).setData(new TextAttribute(fColorManager.getColor(IJavaColorConstants.JAVA_DEFAULT)));
		
		fKeyword.setData(new TextAttribute(fColorManager.getColor(IJavaColorConstants.JAVA_KEYWORD)));
		fType.setData(new TextAttribute(fColorManager.getColor(IJavaColorConstants.JAVA_TYPE)));
		fString.setData(new TextAttribute(fColorManager.getColor(IJavaColorConstants.JAVA_STRING)));
		fComment.setData(new TextAttribute(fColorManager.getColor(IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT)));
	}
}