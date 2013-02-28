/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.spelling;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.text.spelling.SpellCheckEngine;
import org.eclipse.jdt.internal.ui.text.spelling.engine.AbstractSpellDictionary;
import org.eclipse.jdt.internal.ui.text.spelling.engine.DefaultPhoneticDistanceAlgorithm;
import org.eclipse.jdt.internal.ui.text.spelling.engine.ISpellCheckEngine;
import org.eclipse.jdt.internal.ui.text.spelling.engine.ISpellChecker;
import org.eclipse.jdt.internal.ui.text.spelling.engine.RankedWordProposal;


public class SpellCheckEngineTestCase extends TestCase {

	protected static class TestDictionary extends AbstractSpellDictionary {

		/*
		 * @see org.eclipse.jdt.ui.text.spelling.engine.ISpellDictionary#addWord(java.lang.String)
		 */
		public void addWord(String word) {
			hashWord(word);
		}

		/*
		 * @see org.eclipse.jdt.ui.text.spelling.engine.AbstractSpellDictionary#getURL()
		 */
		protected URL getURL() throws MalformedURLException {
			return getClass().getResource("Dictionary"); //$NON-NLS-1$
		}
	}

	public static final String GLOBAL= "Global"; //$NON-NLS-1$
	public static final String LASTWAGEN= "Lastwagen"; //$NON-NLS-1$
	public static final String LORRY= "Lorry"; //$NON-NLS-1$
	public static final String SENTENCECONTENT= "sentence"; //$NON-NLS-1$
	public static final String SENTENCESTART= "Sentence"; //$NON-NLS-1$
	public static final String TRUCK= "Truck"; //$NON-NLS-1$

	public static Test suite() {
		return new TestSuite(SpellCheckEngineTestCase.class);
	}

	private final TestDictionary fDEDictionary= new TestDictionary();
	private final ISpellCheckEngine fEngine= SpellCheckEngine.getInstance();
	private final TestDictionary fGlobalDictionary= new TestDictionary();
	private final IPreferenceStore fPreferences= PreferenceConstants.getPreferenceStore();
	private final TestDictionary fUKDictionary= new TestDictionary();
	private final TestDictionary fUSDictionary= new TestDictionary();

	public SpellCheckEngineTestCase(String name) {
		super(name);
	}

	protected final boolean contains(Set words, String word) {

		RankedWordProposal proposal= null;
		for (final Iterator iterator= words.iterator(); iterator.hasNext();) {

			proposal= (RankedWordProposal)iterator.next();
			if (proposal.getText().equals(word))
				return true;
		}
		return false;
	}

	/*
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();

		fUSDictionary.addWord(TRUCK);
		fUKDictionary.addWord(LORRY);
		fDEDictionary.addWord(LASTWAGEN);
		fGlobalDictionary.addWord(GLOBAL);

		fEngine.registerDictionary(Locale.US, fUSDictionary);
		fEngine.registerDictionary(Locale.UK, fUKDictionary);
		fEngine.registerDictionary(Locale.GERMANY, fDEDictionary);
		fEngine.registerGlobalDictionary(fGlobalDictionary);
	}

	/*
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();

		fUSDictionary.unload();
		fUKDictionary.unload();
		fDEDictionary.unload();
		fGlobalDictionary.unload();

		fEngine.unregisterDictionary(fUSDictionary);
		fEngine.unregisterDictionary(fUKDictionary);
		fEngine.unregisterDictionary(fDEDictionary);
		fEngine.unregisterDictionary(fGlobalDictionary);
	}

	public void testAvailableLocales() {
		final Set result= SpellCheckEngine.getLocalesWithInstalledDictionaries();
		assertTrue(result.size() >= 0);
	}

	public void testDefaultLocale() {
		assertTrue(SpellCheckEngine.getDefaultLocale().equals(Locale.getDefault()));
	}

	public void testDefaultSpellChecker() {
		fPreferences.setValue(PreferenceConstants.SPELLING_LOCALE, Locale.US.toString());
		fEngine.unregisterDictionary(fUSDictionary);

		final ISpellChecker checker= fEngine.getSpellChecker();
		assertNotNull(checker);

		assertFalse(checker.isCorrect(TRUCK));
		assertTrue(checker.isCorrect(LORRY)); // us in UK dictionary
		assertFalse(checker.isCorrect(LASTWAGEN));
		assertTrue(checker.isCorrect(GLOBAL));
		fEngine.registerDictionary(Locale.US, fUSDictionary);

		assertFalse(checker.isCorrect(TRUCK));
		fUSDictionary.addWord(TRUCK);

		assertTrue(checker.isCorrect(LORRY)); // is in UK dictionary
		assertFalse(checker.isCorrect(LASTWAGEN));
		assertTrue(checker.isCorrect(GLOBAL));
	}

	public void testDESpellChecker() {
		fPreferences.setValue(PreferenceConstants.SPELLING_LOCALE, Locale.GERMANY.toString());
		final ISpellChecker checker= fEngine.getSpellChecker();
		assertNotNull(checker);

		assertFalse(checker.isCorrect(TRUCK));
		assertTrue(checker.isCorrect(GLOBAL));
		assertFalse(checker.isCorrect(LORRY));
		assertTrue(checker.isCorrect(LASTWAGEN));

		assertTrue(fDEDictionary.isLoaded());
		assertTrue(fGlobalDictionary.isLoaded());
	}

	public void testIgnoredWord() {
		fPreferences.setValue(PreferenceConstants.SPELLING_LOCALE, Locale.US.toString());
		final ISpellChecker checker= fEngine.getSpellChecker();
		assertNotNull(checker);

		assertFalse(checker.isCorrect(LORRY));
		checker.ignoreWord(LORRY);
		assertTrue(checker.isCorrect(LORRY));
		checker.checkWord(LORRY);
		assertFalse(checker.isCorrect(LORRY));
	}

	public void testUKSpellChecker() {
		fPreferences.setValue(PreferenceConstants.SPELLING_LOCALE, Locale.UK.toString());
		final ISpellChecker checker= fEngine.getSpellChecker();
		assertNotNull(checker);

		assertFalse(checker.isCorrect(TRUCK));
		assertTrue(checker.isCorrect(GLOBAL));
		assertTrue(checker.isCorrect(LORRY));
		assertFalse(checker.isCorrect(LASTWAGEN));

		assertTrue(fUKDictionary.isLoaded());
		assertTrue(fGlobalDictionary.isLoaded());
	}

	public void testUnknownSpellChecker() {
		fPreferences.setValue(PreferenceConstants.SPELLING_LOCALE, Locale.CHINA.toString());
		final ISpellChecker checker= fEngine.getSpellChecker();
		assertNotNull(checker);
		assertEquals(Locale.CHINA, checker.getLocale());
	}

	public void testUSSpellChecker() {
		fPreferences.setValue(PreferenceConstants.SPELLING_LOCALE, Locale.US.toString());
		final ISpellChecker checker= fEngine.getSpellChecker();
		assertNotNull(checker);

		System.out.println(System.getProperty("file.encoding"));

		assertTrue(Locale.US.equals(checker.getLocale()));

		assertTrue(fUSDictionary.isLoaded());
		assertTrue(fGlobalDictionary.isLoaded());

		assertTrue(checker.isCorrect(TRUCK));
		assertTrue(checker.isCorrect(GLOBAL));
		assertFalse(checker.isCorrect(LORRY));
		assertFalse(checker.isCorrect(LASTWAGEN));

	}

	public void testWordProposals() {
		fPreferences.setValue(PreferenceConstants.SPELLING_LOCALE, Locale.US.toString());
		final ISpellChecker checker= fEngine.getSpellChecker();
		assertNotNull(checker);

		fUSDictionary.addWord(SENTENCESTART);

		Set proposals= checker.getProposals(SENTENCESTART, true);
		assertTrue(proposals.size() >= 1);

		assertTrue(contains(proposals, SENTENCESTART));
		assertFalse(contains(proposals, SENTENCECONTENT));

		proposals= checker.getProposals(SENTENCECONTENT, true);
		assertTrue(proposals.size() >= 1);

		assertTrue(contains(proposals, SENTENCESTART));
		assertFalse(contains(proposals, SENTENCECONTENT));

		proposals= checker.getProposals(SENTENCECONTENT, false);
		assertTrue(proposals.size() >= 1);

		assertTrue(contains(proposals, SENTENCESTART));
		assertFalse(contains(proposals, SENTENCECONTENT));
		assertTrue(((RankedWordProposal) proposals.iterator().next()).getRank() == - DefaultPhoneticDistanceAlgorithm.COST_CASE);

		fDEDictionary.addWord(SENTENCESTART);

		proposals= checker.getProposals(SENTENCESTART, false);
		assertTrue(proposals.size() >= 1);
		assertTrue(((RankedWordProposal) proposals.iterator().next()).getRank() == 0);

		proposals= checker.getProposals(SENTENCESTART, true);
		assertTrue(proposals.size() >= 1);
		assertTrue(((RankedWordProposal) proposals.iterator().next()).getRank() == 0);

		proposals= checker.getProposals(SENTENCECONTENT, true);
		assertTrue(proposals.size() >= 1);
		assertTrue(((RankedWordProposal) proposals.iterator().next()).getRank() == - DefaultPhoneticDistanceAlgorithm.COST_CASE);

		proposals= checker.getProposals(SENTENCECONTENT, false);
		assertTrue(proposals.size() >= 1);
		assertTrue(((RankedWordProposal) proposals.iterator().next()).getRank() == - DefaultPhoneticDistanceAlgorithm.COST_CASE);
	}
}
