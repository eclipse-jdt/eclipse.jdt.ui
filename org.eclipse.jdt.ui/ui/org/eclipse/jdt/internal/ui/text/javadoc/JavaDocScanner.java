/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.internal.ui.text.javadoc;



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
			super("<", ">", token, (char) 0);
		}
		
		/*
		 * @see SingleLineRule 
		 */
		public TagRule(IToken token, char escapeCharacter) {
			super("<", ">", token, escapeCharacter);
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
	
	
	private static String[] fgKeywords= {"@author", "@deprecated", "@exception", "@param", "@return", "@see", "@serial", "@serialData", "@serialField", "@since", "@throws", "@version"};
	
	
	public JavaDocScanner(IColorManager manager) {
		super();
		
		IToken keyword= new Token(new TextAttribute(manager.getColor(IJavaColorConstants.JAVADOC_KEYWORD)));
		IToken tag= new Token(new TextAttribute(manager.getColor(IJavaColorConstants.JAVADOC_TAG)));
		IToken link= new Token(new TextAttribute(manager.getColor(IJavaColorConstants.JAVADOC_LINK)));
		
		
		List list= new ArrayList();
		
		// Add rule for tags.
		list.add(new TagRule(tag));
		
		// Add rule for links.
		list.add(new SingleLineRule("{", "}", link));
		
		// Add generic whitespace rule.
		list.add(new WhitespaceRule(new JavaWhitespaceDetector()));
		
		// Add word rule for keywords.
		WordRule wordRule= new WordRule(new JavaDocWordDetector());
		for (int i= 0; i < fgKeywords.length; i++)
			wordRule.addWord(fgKeywords[i], keyword);
		list.add(wordRule);
		
		IRule[] result= new IRule[list.size()];
		list.toArray(result);
		setRules(result);
	}
	
	public IDocument getDocument() {
		return fDocument;
	}
}


