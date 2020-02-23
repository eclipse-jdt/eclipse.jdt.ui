/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.text.spelling.engine;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;


/**
 * Partial implementation of a spell dictionary.
 *
 * @since 3.0
 */
public abstract class AbstractSpellDictionary implements ISpellDictionary {

	/**
	 * Byte array wrapper
	 * @since 3.6
	 */
	private static class ByteArrayWrapper {

		private byte[] byteArray;

		public ByteArrayWrapper(byte[] byteArray) {
			this.byteArray= byteArray;
		}
		@Override
		public int hashCode() {
			return 31 + Arrays.hashCode(byteArray);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof ByteArrayWrapper))
				return false;
			ByteArrayWrapper other= (ByteArrayWrapper)obj;
			if (!Arrays.equals(byteArray, other.byteArray))
				return false;
			return true;
		}
	}


	/**
	 * Canonical name for UTF-8 encoding
	 * @since 3.6
	 */
	private static final String UTF_8= "UTF-8"; //$NON-NLS-1$

	/** The bucket capacity */
	protected static final int BUCKET_CAPACITY= 4;

	/** The word buffer capacity */
	protected static final int BUFFER_CAPACITY= 32;

	/** The distance threshold */
	protected static final int DISTANCE_THRESHOLD= 160;

	/**
	 * The hash load factor
	 * @since 3.6
	 */
	protected static final float LOAD_FACTOR= 0.85f;

	/** The phonetic distance algorithm */
	private IPhoneticDistanceAlgorithm fDistanceAlgorithm= new DefaultPhoneticDistanceAlgorithm();

	/** The mapping from phonetic hashes to word lists */
	private final Map<ByteArrayWrapper, Object> fHashBuckets= new HashMap<>(getInitialSize(), LOAD_FACTOR);

	/** The phonetic hash provider */
	private IPhoneticHashProvider fHashProvider= new DefaultPhoneticHashProvider();

	/** Is the dictionary already loaded? */
	private boolean fLoaded= false;
	/**
	 * Must the dictionary be loaded?
	 * @since 3.2
	 */
	private boolean fMustLoad= true;

	/**
	 * Tells whether to strip non-letters at word boundaries.
	 * @since 3.3
	 */
	boolean fIsStrippingNonLetters= true;

	/**
	 * Returns the initial size of dictionary.
	 *
	 * @return The initial size of dictionary.
	 * @since 3.6
	 */
	protected int getInitialSize() {
		return 32;
	}

	/**
	 * Returns all candidates with the same phonetic hash.
	 *
	 * @param hash
	 *                   The hash to retrieve the candidates of
	 * @return Array of candidates for the phonetic hash
	 */
	protected final Object getCandidates(final String hash) {
		ByteArrayWrapper hashBytes;
		try {
			hashBytes= new ByteArrayWrapper(hash.getBytes(UTF_8));
		} catch (UnsupportedEncodingException e) {
			JavaPlugin.log(e);
			return null;
		}
		return fHashBuckets.get(hashBytes);
	}

	/**
	 * Returns all candidates that have a phonetic hash within a bounded
	 * distance to the specified word.
	 *
	 * @param word
	 *                   The word to find the nearest matches for
	 * @param sentence
	 *                   <code>true</code> iff the proposals start a new sentence,
	 *                   <code>false</code> otherwise
	 * @param hashs
	 *                   Array of close hashes to find the matches
	 * @return Set of ranked words with bounded distance to the specified word
	 */
	protected final Set<RankedWordProposal> getCandidates(final String word, final boolean sentence, final ArrayList<String> hashs) {

		int distance= 0;
		String hash= null;

		final StringBuilder buffer= new StringBuilder(BUFFER_CAPACITY);
		final HashSet<RankedWordProposal> result= new HashSet<>(BUCKET_CAPACITY * hashs.size());

		for (String hash2 : hashs) {

			hash= hash2;

			final Object candidates= getCandidates(hash);
			if (candidates == null)
				continue;
			else if (candidates instanceof byte[]) {
				String candidate;
				try {
					candidate= new String((byte[])candidates, UTF_8);
				} catch (UnsupportedEncodingException e) {
					JavaPlugin.log(e);
					return result;
				}
				distance= fDistanceAlgorithm.getDistance(word, candidate);
				if (distance < DISTANCE_THRESHOLD) {
					buffer.setLength(0);
					buffer.append(candidate);
					if (sentence)
						buffer.setCharAt(0, Character.toUpperCase(buffer.charAt(0)));
					result.add(new RankedWordProposal(buffer.toString(), -distance));
				}
				continue;
			}

			@SuppressWarnings("unchecked")
			final ArrayList<byte[]> candidateList= (ArrayList<byte[]>)candidates;
			int candidateSize= Math.min(500, candidateList.size()); // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=195357
			for (int offset= 0; offset < candidateSize; offset++) {

				String candidate;
				try {
					candidate= new String(candidateList.get(offset), UTF_8);
				} catch (UnsupportedEncodingException e) {
					JavaPlugin.log(e);
					return result;
				}
				distance= fDistanceAlgorithm.getDistance(word, candidate);

				if (distance < DISTANCE_THRESHOLD) {

					buffer.setLength(0);
					buffer.append(candidate);

					if (sentence)
						buffer.setCharAt(0, Character.toUpperCase(buffer.charAt(0)));

					result.add(new RankedWordProposal(buffer.toString(), -distance));
				}
			}
		}
		return result;
	}

	/**
	 * Returns all approximations that have a phonetic hash with smallest
	 * possible distance to the specified word.
	 *
	 * @param word
	 *                   The word to find the nearest matches for
	 * @param sentence
	 *                   <code>true</code> iff the proposals start a new sentence,
	 *                   <code>false</code> otherwise
	 * @param result
	 *                   Set of ranked words with smallest possible distance to the
	 *                   specified word
	 */
	protected final void getCandidates(final String word, final boolean sentence, final Set<RankedWordProposal> result) {

		int distance= 0;
		int minimum= Integer.MAX_VALUE;

		StringBuilder buffer= new StringBuilder(BUFFER_CAPACITY);

		final Object candidates= getCandidates(fHashProvider.getHash(word));
		if (candidates == null)
			return;
		else if (candidates instanceof byte[]) {
			String candidate;
			try {
				candidate= new String((byte[])candidates, UTF_8);
			} catch (UnsupportedEncodingException e) {
				JavaPlugin.log(e);
				return;
			}
			distance= fDistanceAlgorithm.getDistance(word, candidate);
			buffer.append(candidate);
			if (sentence)
				buffer.setCharAt(0, Character.toUpperCase(buffer.charAt(0)));
			result.add(new RankedWordProposal(buffer.toString(), -distance));
			return;
		}

		@SuppressWarnings("unchecked")
		final ArrayList<byte[]> candidateList= (ArrayList<byte[]>)candidates;
		final ArrayList<RankedWordProposal> matches= new ArrayList<>(candidateList.size());

		for (byte[] element : candidateList) {
			String candidate;
			try {
				candidate= new String(element, UTF_8);
			} catch (UnsupportedEncodingException e) {
				JavaPlugin.log(e);
				return;
			}
			distance= fDistanceAlgorithm.getDistance(word, candidate);

			if (distance <= minimum) {

				if (distance < minimum)
					matches.clear();

				buffer.setLength(0);
				buffer.append(candidate);

				if (sentence)
					buffer.setCharAt(0, Character.toUpperCase(buffer.charAt(0)));

				matches.add(new RankedWordProposal(buffer.toString(), -distance));
				minimum= distance;
			}
		}

		result.addAll(matches);
	}

	/**
	 * Tells whether this dictionary is empty.
	 *
	 * @return <code>true</code> if this dictionary is empty
	 * @since 3.3
	 */
	protected boolean isEmpty() {
		return fHashBuckets.isEmpty();
	}

	/**
	 * Returns the used phonetic distance algorithm.
	 *
	 * @return The phonetic distance algorithm
	 */
	protected final IPhoneticDistanceAlgorithm getDistanceAlgorithm() {
		return fDistanceAlgorithm;
	}

	/**
	 * Returns the used phonetic hash provider.
	 *
	 * @return The phonetic hash provider
	 */
	protected final IPhoneticHashProvider getHashProvider() {
		return fHashProvider;
	}

	@Override
	public Set<RankedWordProposal> getProposals(final String word, final boolean sentence) {

		try {

			if (!fLoaded) {
				synchronized (this) {
					fLoaded= load(getURL());
					if (fLoaded)
						compact();
				}
			}

		} catch (MalformedURLException exception) {
			// Do nothing
		}

		final String hash= fHashProvider.getHash(word);
		final char[] mutators= fHashProvider.getMutators();

		final ArrayList<String> neighborhood= new ArrayList<>((word.length() + 1) * (mutators.length + 2));
		neighborhood.add(hash);

		final Set<RankedWordProposal> candidates= getCandidates(word, sentence, neighborhood);
		neighborhood.clear();

		char previous= 0;
		char next= 0;

		char[] characters= word.toCharArray();
		for (int index= 0; index < word.length() - 1; index++) {

			next= characters[index];
			previous= characters[index + 1];

			characters[index]= previous;
			characters[index + 1]= next;

			neighborhood.add(fHashProvider.getHash(new String(characters)));

			characters[index]= next;
			characters[index + 1]= previous;
		}

		final String sentinel= word + " "; //$NON-NLS-1$

		characters= sentinel.toCharArray();
		int offset= characters.length - 1;

		while (true) {

			for (char mutator : mutators) {

				characters[offset]= mutator;
				neighborhood.add(fHashProvider.getHash(new String(characters)));
			}

			if (offset == 0)
				break;

			characters[offset]= characters[offset - 1];
			--offset;
		}

		char mutated= 0;
		characters= word.toCharArray();

		for (int index= 0; index < word.length(); index++) {

			mutated= characters[index];
			for (char mutator2 : mutators) {

				characters[index]= mutator2;
				neighborhood.add(fHashProvider.getHash(new String(characters)));
			}
			characters[index]= mutated;
		}

		characters= word.toCharArray();
		final char[] deleted= new char[characters.length - 1];

		System.arraycopy(characters, 0, deleted, 0, deleted.length);

		next= characters[characters.length - 1];
		offset= deleted.length;

		while (true) {

			neighborhood.add(fHashProvider.getHash(new String(characters)));
			if (offset == 0)
				break;

			previous= next;
			next= deleted[offset - 1];

			deleted[offset - 1]= previous;
			--offset;
		}

		neighborhood.remove(hash);
		final Set<RankedWordProposal> matches= getCandidates(word, sentence, neighborhood);

		if (matches.isEmpty() && candidates.isEmpty())
			getCandidates(word, sentence, candidates);

		candidates.addAll(matches);

		return candidates;
	}

	/**
	 * Returns the URL of the dictionary word list.
	 *
	 * @throws MalformedURLException
	 *                    if the URL could not be retrieved
	 * @return The URL of the dictionary word list
	 */
	protected abstract URL getURL() throws MalformedURLException;

	/**
	 * Hashes the word into the dictionary.
	 *
	 * @param word
	 *                   The word to hash in the dictionary
	 */
	protected final void hashWord(final String word) {

		final String hash= fHashProvider.getHash(word);
		ByteArrayWrapper hashBytes;
		byte[] wordBytes;
		try {
			hashBytes= new ByteArrayWrapper(hash.getBytes(UTF_8));
			wordBytes= word.getBytes(UTF_8);
		} catch (UnsupportedEncodingException e) {
			JavaPlugin.log(e);
			return;
		}

		Object bucket= fHashBuckets.get(hashBytes);

		if (bucket == null) {
			fHashBuckets.put(hashBytes, wordBytes);
		} else if (bucket instanceof ArrayList) {
			@SuppressWarnings("unchecked")
			ArrayList<byte[]> bucketList= (ArrayList<byte[]>)bucket;
			bucketList.add(wordBytes);
		} else {
			ArrayList<Object> list= new ArrayList<>(BUCKET_CAPACITY);
			list.add(bucket);
			list.add(wordBytes);
			fHashBuckets.put(hashBytes, list);
		}
	}

	@Override
	public boolean isCorrect(String word) {
		word= stripNonLetters(word);
		try {

			if (!fLoaded) {
				synchronized (this) {
					fLoaded= load(getURL());
					if (fLoaded)
						compact();
				}
			}

		} catch (MalformedURLException exception) {
			// Do nothing
		}

		final Object candidates= getCandidates(fHashProvider.getHash(word));
		if (candidates == null)
			return false;
		else if (candidates instanceof byte[]) {
			String candidate;
			try {
				candidate= new String((byte[])candidates, UTF_8);
			} catch (UnsupportedEncodingException e) {
				JavaPlugin.log(e);
				return false;
			}
			if (candidate.equals(word) || candidate.equals(word.toLowerCase()))
				return true;
			return false;
		}
		@SuppressWarnings("unchecked")
		final ArrayList<byte[]> candidateList= (ArrayList<byte[]>)candidates;
		byte[] wordBytes;
		byte[] lowercaseWordBytes;
		try {
			wordBytes= word.getBytes(UTF_8);
			lowercaseWordBytes= word.toLowerCase().getBytes(UTF_8);
		} catch (UnsupportedEncodingException e) {
			JavaPlugin.log(e);
			return false;
		}
		for (byte[] candidate : candidateList) {
			if (Arrays.equals(candidate, wordBytes) || Arrays.equals(candidate, lowercaseWordBytes)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void setStripNonLetters(boolean state) {
		fIsStrippingNonLetters= state;
	}

	/**
	 * Strips non-letter characters from the given word.
	 * <p>
	 * This will only happen if the corresponding preference is enabled.
	 * </p>
	 *
	 * @param word the word to strip
	 * @return the stripped word
	 * @since 3.3
	 */
	protected String stripNonLetters(String word) {
		if (!fIsStrippingNonLetters)
			return word;

		int i= 0;
		int j= word.length() - 1;
		while (i <= j && !Character.isLetter(word.charAt(i)))
			i++;
		if (i > j)
			return ""; //$NON-NLS-1$

		while (j > i && !Character.isLetter(word.charAt(j)))
			j--;

		return word.substring(i, j+1);
	}

	@Override
	public synchronized final boolean isLoaded() {
		return fLoaded || fHashBuckets.size() > 0;
	}

	/**
	 * Loads a dictionary word list from disk.
	 *
	 * @param url
	 *                   The URL of the word list to load
	 * @return <code>true</code> iff the word list could be loaded, <code>false</code>
	 *               otherwise
	 */
	protected synchronized boolean load(final URL url) {
		 if (!fMustLoad)
			 return fLoaded;

		if (url != null) {
			InputStream stream= null;
			int line= 0;
			try {
				stream= url.openStream();
				if (stream != null) {
					String word= null;

					// Setup a reader with a decoder in order to read over malformed input if needed.
					CharsetDecoder decoder= Charset.forName(getEncoding()).newDecoder();
					decoder.onMalformedInput(CodingErrorAction.REPORT);
					decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
					try (final BufferedReader reader= new BufferedReader(new InputStreamReader(stream, decoder))) {

						boolean doRead= true;
						while (doRead) {
							try {
								word= reader.readLine();
							} catch (MalformedInputException ex) {
								// Tell the decoder to replace malformed input in order to read the line.
								decoder.onMalformedInput(CodingErrorAction.REPLACE);
								decoder.reset();
								word= reader.readLine();
								decoder.onMalformedInput(CodingErrorAction.REPORT);

								String message= Messages.format(JavaUIMessages.AbstractSpellingDictionary_encodingError,
										new String[] { word, decoder.replacement(), BasicElementLabels.getURLPart(url.toString()) });
								IStatus status= new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.OK, message, ex);
								JavaPlugin.log(status);

								doRead= word != null;
								continue;
							}
							doRead= word != null;
							if (doRead)
								hashWord(word);
						}
					}
					return true;
				}
			} catch (FileNotFoundException ex) {
				String urlString= url.toString();
				String lowercaseUrlString= urlString.toLowerCase();
				if (urlString.equals(lowercaseUrlString))
					JavaPlugin.log(ex);
				else
					try {
						return load(new URL(lowercaseUrlString));
					} catch (MalformedURLException e) {
						JavaPlugin.log(e);
					}
			} catch (IOException exception) {
				if (line > 0) {
					String message= Messages.format(JavaUIMessages.AbstractSpellingDictionary_encodingError, new Object[] { Integer.valueOf(line), BasicElementLabels.getURLPart(url.toString()) });
					IStatus status= new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.OK, message, exception);
					JavaPlugin.log(status);
				} else
					JavaPlugin.log(exception);
			} finally {
				fMustLoad= false;
				try {
					if (stream != null)
						stream.close();
				} catch (IOException x) {
				}
			}
		}
		return false;
	}

	/**
	 * Compacts the dictionary.
	 *
	 * @since 3.3.
	 */
	private void compact() {
		Iterator<Object> iter= fHashBuckets.values().iterator();
		while (iter.hasNext()) {
			Object element= iter.next();
			if (element instanceof ArrayList)
				((ArrayList<?>)element).trimToSize();
		}
	}

	/**
	 * Sets the phonetic distance algorithm to use.
	 *
	 * @param algorithm
	 *                   The phonetic distance algorithm
	 */
	protected final void setDistanceAlgorithm(final IPhoneticDistanceAlgorithm algorithm) {
		fDistanceAlgorithm= algorithm;
	}

	/**
	 * Sets the phonetic hash provider to use.
	 *
	 * @param provider
	 *                   The phonetic hash provider
	 */
	protected final void setHashProvider(final IPhoneticHashProvider provider) {
		fHashProvider= provider;
	}

	@Override
	public synchronized void unload() {
		fLoaded= false;
		fMustLoad= true;
		fHashBuckets.clear();
	}

	@Override
	public boolean acceptsWords() {
		return false;
	}

	@Override
	public void addWord(final String word) {
		// Do nothing
	}

	/**
	 * Returns the encoding of this dictionary.
	 *
	 * @return the encoding of this dictionary
	 * @since 3.3
	 */
	protected String getEncoding() {
		String encoding= JavaPlugin.getDefault().getPreferenceStore().getString(PreferenceConstants.SPELLING_USER_DICTIONARY_ENCODING);
		if (encoding == null || encoding.length() == 0)
			encoding= ResourcesPlugin.getEncoding();
		return encoding;
	}

}
