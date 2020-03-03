/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
 *     Red Hat Inc. - copied from SemanticHighlightingManager and modified
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.jface.text.Position;

/**
 * Highlighted Positions.
 *
 * @since 1.11
 */
public class HighlightedPositionCore extends Position {

	/** Lock object */
	private Object fLock;

	/** Highlighting object */
	private Object fHighlighting;

	/**
	 * Initialize the styled positions with the given offset, length and foreground color.
	 *
	 * @param offset The position offset
	 * @param length The position length
	 * @param highlighting The highlighting object
	 * @param lock The lock object
	 */
	public HighlightedPositionCore(int offset, int length, Object highlighting, Object lock) {
		super(offset, length);
		fHighlighting = highlighting;
		fLock= lock;
	}


	/**
	 * Uses reference equality for the highlighting.
	 *
	 * @param off The offset
	 * @param len The length
	 * @param highlighting The highlighting
	 * @return <code>true</code> iff the given offset, length and highlighting are equal to the internal ones.
	 */
	public boolean isEqual(int off, int len, Object highlighting) {
		synchronized (fLock) {
			return !isDeleted() && getOffset() == off && getLength() == len && fHighlighting == highlighting;
		}
	}

	/**
	 * Is this position contained in the given range (inclusive)? Synchronizes on position updater.
	 *
	 * @param off The range offset
	 * @param len The range length
	 * @return <code>true</code> iff this position is not delete and contained in the given range.
	 */
	public boolean isContained(int off, int len) {
		synchronized (fLock) {
			return !isDeleted() && off <= getOffset() && off + len >= getOffset() + getLength();
		}
	}

	public void update(int off, int len) {
		synchronized (fLock) {
			super.setOffset(off);
			super.setLength(len);
		}
	}

	/*
	 * @see org.eclipse.jface.text.Position#setLength(int)
	 */
	@Override
	public void setLength(int length) {
		synchronized (fLock) {
			super.setLength(length);
		}
	}

	/*
	 * @see org.eclipse.jface.text.Position#setOffset(int)
	 */
	@Override
	public void setOffset(int offset) {
		synchronized (fLock) {
			super.setOffset(offset);
		}
	}

	/*
	 * @see org.eclipse.jface.text.Position#delete()
	 */
	@Override
	public void delete() {
		synchronized (fLock) {
			super.delete();
		}
	}

	/*
	 * @see org.eclipse.jface.text.Position#undelete()
	 */
	@Override
	public void undelete() {
		synchronized (fLock) {
			super.undelete();
		}
	}

	/**
	 * @return Returns the highlighting.
	 */
	public Object getHighlighting() {
		return fHighlighting;
	}
}