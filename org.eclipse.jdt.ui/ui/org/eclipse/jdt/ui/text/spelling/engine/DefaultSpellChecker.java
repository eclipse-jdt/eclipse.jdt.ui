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

package org.eclipse.jdt.ui.text.spelling.engine;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Default spell checker for standard text.
 * 
 * @since 3.0
 */
public class DefaultSpellChecker implements ISpellChecker {

	/** Array of url prefixes */
	public static final String[] URL_PREFIXES= new String[] { "http://", "https://", "www.", "ftp://", "ftps://", "news://", "mailto://" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$

	/**
	 * Does this word contain digits?
	 * 
	 * @param word
	 *                   The word to check
	 * @return <code>true</code> iff this word contains digits, <code>false></code>
	 *               otherwise
	 */
	protected static boolean isDigits(final String word) {

		for (int index= 0; index < word.length(); index++) {

			if (Character.isDigit(word.charAt(index)))
				return true;
		}
		return false;
	}

	/**
	 * Does this word contain mixed-case letters?
	 * 
	 * @param word
	 *                   The word to check
	 * @param sentence
	 *                   <code>true</code> iff the specified word starts a new
	 *                   sentence, <code>false</code> otherwise
	 * @return <code>true</code> iff the contains mixed-case letters, <code>false</code>
	 *               otherwise
	 */
	protected static boolean isMixedCase(final String word, final boolean sentence) {

		final int length= word.length();
		boolean upper= Character.isUpperCase(word.charAt(0));

		if (sentence && upper && (length > 1))
			upper= Character.isUpperCase(word.charAt(1));

		if (upper) {

			for (int index= length - 1; index > 0; index--) {
				if (Character.isLowerCase(word.charAt(index)))
					return true;
			}
		} else {

			for (int index= length - 1; index > 0; index--) {
				if (Character.isUpperCase(word.charAt(index)))
					return true;
			}
		}
		return false;
	}

	/**
	 * Does this word contain upper-case letters only?
	 * 
	 * @param word
	 *                   The word to check
	 * @return <code>true</code> iff this word only contains upper-case
	 *               letters, <code>false</code> otherwise
	 */
	protected static boolean isUpperCase(final String word) {

		for (int index= word.length() - 1; index >= 0; index--) {

			if (Character.isLowerCase(word.charAt(index)))
				return false;
		}
		return true;
	}

	/**
	 * Does this word look like an URL?
	 * 
	 * @param word
	 *                   The word to check
	 * @return <code>true</code> iff this word looks like an URL, <code>false</code>
	 *               otherwise
	 */
	protected static boolean isUrl(final String word) {

		for (int index= 0; index < URL_PREFIXES.length; index++) {

			if (word.startsWith(URL_PREFIXES[index]))
				return true;
		}
		return false;
	}

	/** The dictionaries to use for spell-checking */
	private final Set fDictionaries= new HashSet();

	/** The words to be ignored */
	private final Set fIgnored= new HashSet();

	/** The spell event listeners */
	private final Set fListeners= new HashSet();

	/** The preference store */
	private final IPreferenceStore fPreferences;

	/**
	 * Creates a new default spell-checker.
	 * 
	 * @param store
	 *                   The preference store for this spell-checker
	 */
	public DefaultSpellChecker(final IPreferenceStore store) {
		fPreferences= store;
	}

	/*
	 * @see org.eclipse.spelling.done.ISpellChecker#addDictionary(org.eclipse.spelling.done.ISpellDictionary)
	 */
	public final void addDictionary(final ISpellDictionary dictionary) {
		fDictionaries.add(dictionary);
	}

	/*
	 * @see org.eclipse.spelling.done.ISpellChecker#addListener(org.eclipse.spelling.done.ISpellEventListener)
	 */
	public final void addListener(final ISpellEventListener listener) {
		fListeners.add(listener);
	}
	
	/*
	 * @see org.eclipse.jdt.ui.text.spelling.engine.ISpellChecker#acceptsWords()
	 */
	public boolean acceptsWords() {
		ISpellDictionary dictionary= null;
		for (final Iterator iterator= fDictionaries.iterator(); iterator.hasNext();) {

			dictionary= (ISpellDictionary)iterator.next();
			if (dictionary.acceptsWords())
				return true;
		}
		return false;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.spelling.engine.ISpellChecker#addWord(java.lang.String)
	 */
	public void addWord(final String word) {

		final String addable= word.toLowerCase();
		fIgnored.add(addable);

		ISpellDictionary dictionary= null;
		for (final Iterator iterator= fDictionaries.iterator(); iterator.hasNext();) {

			dictionary= (ISpellDictionary)iterator.next();
			dictionary.addWord(addable);
		}
	}

	/*
	 * @see org.eclipse.jdt.ui.text.spelling.engine.ISpellChecker#checkWord(java.lang.String)
	 */
	public final void checkWord(final String word) {
		fIgnored.remove(word.toLowerCase());
	}

	/*
	 * @see org.eclipse.spelling.done.ISpellChecker#execute(org.eclipse.spelling.ISpellCheckTokenizer)
	 */
	public void execute(final ISpellCheckIterator iterator) {

		final boolean digits= fPreferences.getBoolean(ISpellCheckPreferenceKeys.SPELLING_IGNORE_DIGITS);
		final boolean mixed= fPreferences.getBoolean(ISpellCheckPreferenceKeys.SPELLING_IGNORE_MIXED);
		final boolean sentence= fPreferences.getBoolean(ISpellCheckPreferenceKeys.SPELLING_IGNORE_SENTENCE);
		final boolean upper= fPreferences.getBoolean(ISpellCheckPreferenceKeys.SPELLING_IGNORE_UPPER);
		final boolean urls= fPreferences.getBoolean(ISpellCheckPreferenceKeys.SPELLING_IGNORE_URLS);

		String word= null;
		boolean starts= false;

		while (iterator.hasNext()) {

			word= (String)iterator.next();
			if (word != null) {

				if (!fIgnored.contains(word)) {

					starts= iterator.startsSentence();
					if (!isCorrect(word)) {

						if (!((mixed && !sentence && isMixedCase(word, starts)) || (upper && isUpperCase(word)) || (digits && isDigits(word)) || (urls && isUrl(word))))
							fireEvent(new SpellEvent(this, word, iterator.getBegin(), iterator.getEnd(), starts, false));

					} else {

						if (!sentence && starts && Character.isLowerCase(word.charAt(0)))
							fireEvent(new SpellEvent(this, word, iterator.getBegin(), iterator.getEnd(), true, true));
					}
				}
			}
		}
	}

	/**
	 * Fires the specified event.
	 * 
	 * @param event
	 *                   Event to fire
	 */
	protected final void fireEvent(final ISpellEvent event) {

		ISpellEventListener listener= null;
		for (final Iterator iterator= fListeners.iterator(); iterator.hasNext();) {

			listener= (ISpellEventListener)iterator.next();
			listener.handle(event);
		}
	}

	/*
	 * @see org.eclipse.spelling.done.ISpellChecker#getProposals(java.lang.String,boolean)
	 */
	public Set getProposals(final String word, final boolean sentence) {

		ISpellDictionary dictionary= null;
		final HashSet proposals= new HashSet();

		for (final Iterator iterator= fDictionaries.iterator(); iterator.hasNext();) {

			dictionary= (ISpellDictionary)iterator.next();
			proposals.addAll(dictionary.getProposals(word, sentence));
		}
		return proposals;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.spelling.engine.ISpellChecker#ignoreWord(java.lang.String)
	 */
	public final void ignoreWord(final String word) {
		fIgnored.add(word.toLowerCase());
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.spelling.engine.ISpellChecker#isCorrect(java.lang.String)
	 */
	public final boolean isCorrect(final String word) {

		if (fIgnored.contains(word))
			return true;

		ISpellDictionary dictionary= null;
		for (final Iterator iterator= fDictionaries.iterator(); iterator.hasNext();) {

			dictionary= (ISpellDictionary)iterator.next();
			if (dictionary.isCorrect(word))
				return true;
		}
		return false;
	}

	/*
	 * @see org.eclipse.spelling.done.ISpellChecker#removeDictionary(org.eclipse.spelling.done.ISpellDictionary)
	 */
	public final void removeDictionary(final ISpellDictionary dictionary) {
		fDictionaries.remove(dictionary);
	}

	/*
	 * @see org.eclipse.spelling.done.ISpellChecker#removeListener(org.eclipse.spelling.done.ISpellEventListener)
	 */
	public final void removeListener(final ISpellEventListener listener) {
		fListeners.remove(listener);
	}
}
