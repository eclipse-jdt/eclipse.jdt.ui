package org.eclipse.jdt.internal.ui.text.javadoc;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;

import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jdt.ui.text.IJavaColorConstants;

import org.eclipse.jdt.internal.ui.text.JavaWhitespaceDetector;




/**
 * A rule based JavaDoc scanner.
 */
public class JavaDocScanner extends BufferedRuleBasedScanner {
		
		
	/**
	 * A key word detector.
	 */
	static class JavaDocWordDetector implements IWordDetector {

		/**
		 * @see IWordDetector#isWordStart
		 */
		public boolean isWordStart(char c) {
			return (c == '@');
		}

		/**
		 * @see IWordDetector#isWordPart
		 */
		public boolean isWordPart(char c) {
			return Character.isLetter(c);
		}
	};
	
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
		
		private IToken checkForWhitespace(ICharacterScanner scanner) {
			
			try {
				
				char c= getDocument().getChar(getTokenOffset() + 1);
				if (!Character.isWhitespace(c)) 
					return fToken;
					
			} catch (BadLocationException x) {
			}
			
			return Token.UNDEFINED;
		}
				
		/*
		 * @see PatternRule#evaluate(ICharacterScanner)
		 */
		public IToken evaluate(ICharacterScanner scanner) {
			IToken result= super.evaluate(scanner);
			if (result == fToken)
				return checkForWhitespace(scanner);
			return result;
		}
	};
	
	
	private static String[] fgKeywords= {"@author", "@deprecated", "@exception", "@param", "@return", "@see", "@serial", "@serialData", "@serialField", "@since", "@throws", "@version"}; //$NON-NLS-12$ //$NON-NLS-11$ //$NON-NLS-10$ //$NON-NLS-7$ //$NON-NLS-9$ //$NON-NLS-8$ //$NON-NLS-6$ //$NON-NLS-5$ //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
	
	
	private Token fKeyword;
	private Token fTag;
	private Token fLink;
	
	private IColorManager fColorManager;
	
	
	public JavaDocScanner(IColorManager manager) {
		super();
		
		setDefaultReturnToken(new Token(new TextAttribute(manager.getColor(IJavaColorConstants.JAVADOC_DEFAULT))));
		
		fColorManager= manager;
		
		fKeyword= new Token(new TextAttribute(fColorManager.getColor(IJavaColorConstants.JAVADOC_KEYWORD)));
		fTag= new Token(new TextAttribute(fColorManager.getColor(IJavaColorConstants.JAVADOC_TAG)));
		fLink= new Token(new TextAttribute(fColorManager.getColor(IJavaColorConstants.JAVADOC_LINK)));
		
		initializeRules();
	}
	
	private void initializeRules() {
		
		List list= new ArrayList();
		
		// Add rule for tags.
		list.add(new TagRule(fTag));
		
		// Add rule for links.
		list.add(new SingleLineRule("{", "}", fLink)); //$NON-NLS-2$ //$NON-NLS-1$
		
		// Add generic whitespace rule.
		list.add(new WhitespaceRule(new JavaWhitespaceDetector()));
		
		// Add word rule for keywords.
		WordRule wordRule= new WordRule(new JavaDocWordDetector(), getDefaultReturnToken());
		for (int i= 0; i < fgKeywords.length; i++)
			wordRule.addWord(fgKeywords[i], fKeyword);
		list.add(wordRule);
		
		IRule[] result= new IRule[list.size()];
		list.toArray(result);
		setRules(result);
	}
	
	public IDocument getDocument() {
		return fDocument;
	}
	
	public void colorManagerChanged() {
		
		IToken token= getDefaultReturnToken();
		if (token instanceof Token) 
			((Token) token).setData(new TextAttribute(fColorManager.getColor(IJavaColorConstants.JAVADOC_DEFAULT)));
			
		fKeyword.setData(new TextAttribute(fColorManager.getColor(IJavaColorConstants.JAVADOC_KEYWORD)));
		fTag.setData(new TextAttribute(fColorManager.getColor(IJavaColorConstants.JAVADOC_TAG)));
		fLink.setData(new TextAttribute(fColorManager.getColor(IJavaColorConstants.JAVADOC_LINK)));
	}
}


