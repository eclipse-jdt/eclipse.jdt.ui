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

import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

/**
 * Persistent modifiable word-list based dictionary.
 * 
 * @since 3.0
 */
public class PersistentSpellDictionary extends AbstractSpellDictionary {

	/** The word list location */
	private final URL fLocation;

	/**
	 * Creates a new persistent spell dictionary.
	 * 
	 * @param url
	 *                   The URL of the word list for this dictionary
	 */
	public PersistentSpellDictionary(final URL url) {
		fLocation= url;
	}
	
	/*
	 * @see org.eclipse.jdt.ui.text.spelling.engine.AbstractSpellDictionary#acceptsWords()
	 */
	public boolean acceptsWords() {
		return true;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.spelling.engine.ISpellDictionary#addWord(java.lang.String)
	 */
	public void addWord(final String word) {

		if (!isCorrect(word)) {

			hashWord(word);
			try {

				final FileWriter writer= new FileWriter(fLocation.getPath(), true);
				writer.write(word);
				writer.write("\n"); //$NON-NLS-1$
				writer.close();

			} catch (IOException exception) {
				// Do nothing
			}
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.spelling.engine.AbstractSpellDictionary#getURL()
	 */
	protected final URL getURL() {
		return fLocation;
	}
}
