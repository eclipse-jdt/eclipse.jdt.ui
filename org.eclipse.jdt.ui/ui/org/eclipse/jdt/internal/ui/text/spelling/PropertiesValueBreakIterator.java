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

import java.text.BreakIterator;
import java.text.CharacterIterator;
import java.util.Locale;

/**
 * {@link BreakIterator} implementation for properties values.
 * 
 * @since 3.1
 */
public class PropertiesValueBreakIterator extends BreakIterator {

	/** Parent break iterator from which breaks will be changed and filtered */
	private BreakIterator fParent;
	
	/** Underlying character iterator */
	private CharacterIterator fText;

	/**
	 * Initialize with the given locale.
	 * 
	 * @param locale the locale
	 */
	public PropertiesValueBreakIterator(Locale locale) {
		fParent= BreakIterator.getWordInstance(locale);
	}
	
	/*
	 * @see java.text.BreakIterator#current()
	 */
	public int current() {
		return correct(fParent.current());
	}

	/*
	 * @see java.text.BreakIterator#first()
	 */
	public int first() {
		return fParent.first();
	}

	/*
	 * @see java.text.BreakIterator#last()
	 */
	public int last() {
		return fParent.last();
	}

	/*
	 * @see java.text.BreakIterator#next()
	 */
	public int next() {
		int next= fParent.next();
		while (!stopAt(next))
			next= fParent.next();
		return correct(next);
	}

	/*
	 * @see java.text.BreakIterator#previous()
	 */
	public int previous() {
		int previous= fParent.previous();
		while (!stopAt(previous))
			previous= fParent.previous();
		return correct(previous);
	}

	/*
	 * @see java.text.BreakIterator#following(int)
	 */
	public int following(int offset) {
		int following= fParent.following(offset);
		while (!stopAt(following))
			following= fParent.following(offset);
		return correct(following);
	}

	/*
	 * @see java.text.BreakIterator#next(int)
	 */
	public int next(int n) {
		int next= fParent.next(n);
		while (!stopAt(next))
			next= fParent.next(n);
		return correct(next);
	}

	/*
	 * @see java.text.BreakIterator#getText()
	 */
	public CharacterIterator getText() {
		return fParent.getText();
	}

	/*
	 * @see java.text.BreakIterator#setText(java.text.CharacterIterator)
	 */
	public void setText(CharacterIterator newText) {
		fText= newText;
		fParent.setText(newText);
	}

	/**
	 * Should stop at the given index?
	 * 
	 * @param index the index
	 * @return <code>true</code> iff the iterator should stop at the given index
	 */
	private boolean stopAt(int index) {
		if (index == BreakIterator.DONE)
			return true;
		if (index > getBeginIndex() && index < getEndIndex() - 1 && !Character.isWhitespace(charAt(index - 1)) && !Character.isWhitespace(charAt(index)) && (charAt(index - 1) == '&' || charAt(index) == '&'))
			return false;
		return true;
	}

	/**
	 * Returns the corrected break index of the given index.
	 * 
	 * @param index the index
	 * @return the corrected index
	 */
	private int correct(int index) {
		if (index == BreakIterator.DONE)
			return index;
		if (index > getBeginIndex() && index < getEndIndex() - 1 && !Character.isWhitespace(charAt(index - 1))  && charAt(index - 1) == '\\')
			return index - 1;
		return index;
	}

	/**
	 * Returns the underlying character at the given index.
	 * 
	 * @param index the index
	 * @return the character
	 */
	private char charAt(int index) {
		int oldIndex= fText.getIndex();
		char ch= fText.setIndex(index);
		fText.setIndex(oldIndex);
		return ch;
	}

	/**
	 * Returns the exclusive end index of the underlying character iterator.
	 * 
	 * @return the exclusive end index of the underlying character iterator
	 */
	private int getEndIndex() {
		return fText.getEndIndex();
	}

	/**
	 * Returns the inclusive begin index of the underlying character iterator.
	 * 
	 * @return the inclusive begin index of the underlying character iterator
	 */
	private int getBeginIndex() {
		return fText.getBeginIndex();
	}
}
