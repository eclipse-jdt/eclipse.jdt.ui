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

package org.eclipse.jdt.internal.ui.text.spelling;

import java.text.MessageFormat;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.ui.texteditor.spelling.SpellingProblem;

import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.text.spelling.engine.ISpellEvent;

/**
 * A {@link SpellingProblem} that adapts a {@link ISpellEvent}.
 * <p>
 * TODO: remove {@link ISpellEvent} notification mechanism
 * </p>
 */
public class JavaSpellingProblem extends SpellingProblem {
	
	/** Spell event */
	private ISpellEvent fSpellEvent;
	
	/**
	 * Initialize with the given spell event.
	 * 
	 * @param spellEvent the spell event
	 */
	public JavaSpellingProblem(ISpellEvent spellEvent) {
		super();
		fSpellEvent= spellEvent;
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.spelling.SpellingProblem#getOffset()
	 */
	public int getOffset() {
		return fSpellEvent.getBegin();
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.spelling.SpellingProblem#getLength()
	 */
	public int getLength() {
		return fSpellEvent.getEnd() - fSpellEvent.getBegin() + 1;
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.spelling.SpellingProblem#getMessage()
	 */
	public String getMessage() {
		if (isSentenceStart() && isDictionaryMatch())
			return MessageFormat.format(JavaUIMessages.getString("Spelling.error.case.label"), new String[] { fSpellEvent.getWord() }); //$NON-NLS-1$

		return MessageFormat.format(JavaUIMessages.getString("Spelling.error.label"), new String[] { fSpellEvent.getWord() }); //$NON-NLS-1$
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.spelling.SpellingProblem#getProposals()
	 */
	public ICompletionProposal[] getProposals() {
		/*
		 * TODO: implement, see WordQuickFixProcessor
		 * isDictionaryMatch() and isSentenceStart() are workarounds
		 * that could be removed once getProposals() is implemented
		 */
		return new ICompletionProposal[0];
	}
	
	/**
	 * Returns <code>true</code> iff the corresponding word was found in the dictionary.
	 * <p>
	 * NOTE: to be removed, see {@link #getProposals()}
	 * </p>
	 * 
	 * @return <code>true</code> iff the corresponding word was found in the dictionary
	 */
	public boolean isDictionaryMatch() {
		return fSpellEvent.isMatch();
	}
	
	/**
	 * Returns <code>true</code> iff the corresponding word starts a sentence.
	 * <p>
	 * NOTE: to be removed, see {@link #getProposals()}
	 * </p>
	 * 
	 * @return <code>true</code> iff the corresponding word starts a sentence
	 */
	public boolean isSentenceStart() {
		return fSpellEvent.isStart();
	}
}