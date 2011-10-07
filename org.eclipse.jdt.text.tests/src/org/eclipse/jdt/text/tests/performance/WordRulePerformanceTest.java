/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Doug Satchwell <doug.satchwell@ymail.com> - provided Java only test code on which #measureIgnoreCase(...) is built
 *******************************************************************************/
package org.eclipse.jdt.text.tests.performance;

import junit.framework.Test;

import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;


/**
 * Performance tests for {@link WordRule}.
 * 
 * @since 3.6
 */
public class WordRulePerformanceTest extends TextPerformanceTestCase2 {


	private static final String RULE_WORD_PREFIX= "aTeSt";
	private static final int WORDS_PER_RULE= 200;
	private static final String WORD_IN_DOCUMENT= "AtEsT199";
	private static final int WORDS_IN_DOCUMENT= 100000;


	public static Test suite() {
		return new PerfTestSuite(WordRulePerformanceTest.class);
	}

	private final class MyWordDetector implements IWordDetector {
		public boolean isWordPart(char c) {
			return Character.isLetterOrDigit(c);
		}

		public boolean isWordStart(char c) {
			return Character.isLetterOrDigit(c);
		}
	}


	private Document fDocument;
	private WordRule fWordRule;


	protected void setUp() throws Exception {
		super.setUp();
		StringBuffer sb= new StringBuffer();
		for (int i= 0; i < WORDS_IN_DOCUMENT; i++)
			sb.append(WORD_IN_DOCUMENT).append("\n");

		fDocument= new Document(sb.toString());
		fWordRule= new WordRule(new MyWordDetector(), Token.UNDEFINED, true);
		for (int i= 0; i < WORDS_PER_RULE; i++)
			fWordRule.addWord(RULE_WORD_PREFIX + i, new Token("myToken_" + i));

	}

	public void measureIgnoreCase(PerformanceMeter meter) {
		RuleBasedScanner scanner= new RuleBasedScanner();
		scanner.setRules(new IRule[] { fWordRule });
		scanner.setRange(fDocument, 0, fDocument.getLength());

		meter.start();
		while (scanner.nextToken() != Token.EOF) {
		}
		meter.stop();
	}
}
