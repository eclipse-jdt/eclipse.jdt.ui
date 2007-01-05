/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.spelling.engine;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;

import org.eclipse.jdt.internal.ui.JavaPlugin;

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

			OutputStreamWriter writer= null;
			try {
				FileOutputStream fileStream= new FileOutputStream(fLocation.getPath(), true);
				writer= new OutputStreamWriter(fileStream, getEncoding());
				writer.write(word);
				writer.write("\n"); //$NON-NLS-1$
			} catch (IOException exception) {
				JavaPlugin.log(exception);
			} finally {
				try {
					if (writer != null)
						writer.close();
				} catch (IOException e) {
				}
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
