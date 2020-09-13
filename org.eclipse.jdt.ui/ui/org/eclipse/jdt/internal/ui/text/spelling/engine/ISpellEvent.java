/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

import java.util.Set;

/**
 * Event fired by spell checkers.
 *
 * @since 3.0
 */
public interface ISpellEvent {

	/**
	 * Returns the begin index of the incorrectly spelled word.
	 *
	 * @return The begin index of the word
	 */
	int getBegin();

	/**
	 * Returns the end index of the incorrectly spelled word.
	 *
	 * @return The end index of the word
	 */
	int getEnd();

	/**
	 * Returns the proposals for the incorrectly spelled word.
	 *
	 * @return Array of proposals for the word
	 */
	Set<RankedWordProposal> getProposals();

	/**
	 * Returns the incorrectly spelled word.
	 *
	 * @return The incorrect word
	 */
	String getWord();

	/**
	 * Was the incorrectly spelled word found in the dictionary?
	 *
	 * @return <code>true</code> iff the word was found, <code>false</code> otherwise
	 */
	boolean isMatch();

	/**
	 * Does the incorrectly spelled word start a new sentence?
	 *
	 * @return <code>true<code> iff the word starts a new sentence, <code>false</code> otherwise
	 */
	boolean isStart();
}
