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
package org.eclipse.jdt.internal.ui.propertiesfileeditor;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.WordPatternRule;

/**
 * A white space predicate rule.
 *
 * @since 3.1
 */
public final class WhitespacePredicateRule extends WordPatternRule {
	
	private static class DummyDetector implements IWordDetector {
		
		/*
		 * @see IWordDetector#isWordStart
		 */
		public boolean isWordStart(char c) {
			return false;
		}
		
		/*
		 * @see IWordDetector#isWordPart
		 */
		public boolean isWordPart(char c) {
			return false;
		}
	}

	
	/**
	 * Creates a white space rule for the given <code>token</code>.
	 * 
	 * @param token the token to be returned on success
	 */
	public WhitespacePredicateRule(IToken token) {
		super(new DummyDetector(), "dummy", "dummy", token); //$NON-NLS-1$//$NON-NLS-2$
	}
	
	/*
	 * @see org.eclipse.jface.text.rules.WordPatternRule#endSequenceDetected(org.eclipse.jface.text.rules.ICharacterScanner)
	 */
	protected boolean endSequenceDetected(ICharacterScanner scanner) {
		int c;
		do {
			c= scanner.read();
		} while (Character.isWhitespace((char) c));

		scanner.unread();
		
		return true;
	}
	
	/*
	 * @see org.eclipse.jface.text.rules.PatternRule#sequenceDetected(org.eclipse.jface.text.rules.ICharacterScanner, char[], boolean)
	 */
	protected boolean sequenceDetected(ICharacterScanner scanner, char[] sequence, boolean eofAllowed) {
		if (Character.isWhitespace((char)scanner.read()))
			return true;
		
		scanner.unread();
		return false;
	}
}
