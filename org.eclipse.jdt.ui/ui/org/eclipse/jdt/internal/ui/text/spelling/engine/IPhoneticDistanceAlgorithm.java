/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

/**
 * Interface of algorithms to compute the phonetic distance between two words.
 *
 * @since 3.0
 */
public interface IPhoneticDistanceAlgorithm {

	/**
	 * Returns the non-negative phonetic distance between two words
	 *
	 * @param from
	 *                  The first word
	 * @param to
	 *                  The second word
	 * @return The non-negative phonetic distance between the words.
	 */
	int getDistance(String from, String to);

	/**
	 * Returns the non-negative phonetic distance between two words, aborting
	 * early and returning a value &gt;= <code>threshold</code> as soon as it is
	 * determined that the distance cannot be below the threshold.
	 *
	 * @param from
	 *                  The first word
	 * @param to
	 *                  The second word
	 * @param threshold
	 *                  The distance threshold above which computation can be aborted
	 * @return The non-negative phonetic distance between the words, or a value
	 *         &gt;= <code>threshold</code> if the distance meets or exceeds it.
	 * @since 3.20
	 */
	default int getDistance(String from, String to, int threshold) {
		return getDistance(from, to);
	}
}
