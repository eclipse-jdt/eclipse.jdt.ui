/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.spelling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.text.spelling.SpellCheckEngine;
import org.eclipse.jdt.internal.ui.text.spelling.engine.AbstractSpellDictionary;
import org.eclipse.jdt.internal.ui.text.spelling.engine.DefaultPhoneticDistanceAlgorithm;
import org.eclipse.jdt.internal.ui.text.spelling.engine.ISpellCheckEngine;
import org.eclipse.jdt.internal.ui.text.spelling.engine.ISpellChecker;
import org.eclipse.jdt.internal.ui.text.spelling.engine.RankedWordProposal;



public class SpellCheckEngineTestCase {

	protected static class TestDictionary extends AbstractSpellDictionary {

		@Override
		public void addWord(String word) {
			hashWord(word);
		}

		@Override
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

	private final TestDictionary fDEDictionary= new TestDictionary();
	private final ISpellCheckEngine fEngine= SpellCheckEngine.getInstance();
	private final TestDictionary fGlobalDictionary= new TestDictionary();
	private final IPreferenceStore fPreferences= PreferenceConstants.getPreferenceStore();
	private final TestDictionary fUKDictionary= new TestDictionary();
	private final TestDictionary fUSDictionary= new TestDictionary();


	protected final boolean contains(Set<RankedWordProposal> words, String word) {

		RankedWordProposal proposal= null;
		for (Iterator<RankedWordProposal> iterator= words.iterator(); iterator.hasNext();) {
			proposal= iterator.next();
			if (proposal.getText().equals(word))
				return true;
		}
		return false;
	}

	@Before
	public void setUp() throws Exception {

		fUSDictionary.addWord(TRUCK);
		fUKDictionary.addWord(LORRY);
		fDEDictionary.addWord(LASTWAGEN);
		fGlobalDictionary.addWord(GLOBAL);

		fEngine.registerDictionary(Locale.US, fUSDictionary);
		fEngine.registerDictionary(Locale.UK, fUKDictionary);
		fEngine.registerDictionary(Locale.GERMANY, fDEDictionary);
		fEngine.registerGlobalDictionary(fGlobalDictionary);
	}

	@After
	public void tearDown() throws Exception {

		fUSDictionary.unload();
		fUKDictionary.unload();
		fDEDictionary.unload();
		fGlobalDictionary.unload();

		fEngine.unregisterDictionary(fUSDictionary);
		fEngine.unregisterDictionary(fUKDictionary);
		fEngine.unregisterDictionary(fDEDictionary);
		fEngine.unregisterDictionary(fGlobalDictionary);
	}

	@Test
	public void testAvailableLocales() {
		final Set<Locale> result= SpellCheckEngine.getLocalesWithInstalledDictionaries();
		assertTrue(result.size() >= 0);
	}

	@Test
	public void testDefaultLocale() {
		assertEquals(SpellCheckEngine.getDefaultLocale(), Locale.getDefault());
	}

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
	public void testUnknownSpellChecker() {
		fPreferences.setValue(PreferenceConstants.SPELLING_LOCALE, Locale.CHINA.toString());
		final ISpellChecker checker= fEngine.getSpellChecker();
		assertNotNull(checker);
		assertEquals(Locale.CHINA, checker.getLocale());
	}

	@Test
	public void testUSSpellChecker() {
		fPreferences.setValue(PreferenceConstants.SPELLING_LOCALE, Locale.US.toString());
		final ISpellChecker checker= fEngine.getSpellChecker();
		assertNotNull(checker);

		System.out.println(Charset.defaultCharset().displayName());

		assertEquals(Locale.US, checker.getLocale());

		assertTrue(fUSDictionary.isLoaded());
		assertTrue(fGlobalDictionary.isLoaded());

		assertTrue(checker.isCorrect(TRUCK));
		assertTrue(checker.isCorrect(GLOBAL));
		assertFalse(checker.isCorrect(LORRY));
		assertFalse(checker.isCorrect(LASTWAGEN));

	}

	@Test
	public void testWordProposals() {
		fPreferences.setValue(PreferenceConstants.SPELLING_LOCALE, Locale.US.toString());
		final ISpellChecker checker= fEngine.getSpellChecker();
		assertNotNull(checker);

		fUSDictionary.addWord(SENTENCESTART);

		Set<RankedWordProposal> proposals= checker.getProposals(SENTENCESTART, true);
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
		assertEquals(- DefaultPhoneticDistanceAlgorithm.COST_CASE, proposals.iterator().next().getRank());

		fDEDictionary.addWord(SENTENCESTART);

		proposals= checker.getProposals(SENTENCESTART, false);
		assertTrue(proposals.size() >= 1);
		assertEquals(0, proposals.iterator().next().getRank());

		proposals= checker.getProposals(SENTENCESTART, true);
		assertTrue(proposals.size() >= 1);
		assertEquals(0, proposals.iterator().next().getRank());

		proposals= checker.getProposals(SENTENCECONTENT, true);
		assertTrue(proposals.size() >= 1);
		assertEquals(- DefaultPhoneticDistanceAlgorithm.COST_CASE, proposals.iterator().next().getRank());

		proposals= checker.getProposals(SENTENCECONTENT, false);
		assertTrue(proposals.size() >= 1);
		assertEquals(- DefaultPhoneticDistanceAlgorithm.COST_CASE, proposals.iterator().next().getRank());
	}
}
