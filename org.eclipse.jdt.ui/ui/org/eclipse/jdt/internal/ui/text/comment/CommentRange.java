/*****************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package org.eclipse.jdt.internal.ui.text.comment;

import org.eclipse.jface.text.IRegion;

/**
 * Range in a comment region in comment region coordinates.
 * 
 * <p>
 * Comment ranges are considered reference objects.
 * Its offset and length may change over time.
 * </p>
 * 
 * @since 3.0
 */
public class CommentRange implements IRegion {

	/** Length of the comment range */
	private int fLength= 0;

	/** Offset of the comment range in comment region coordinates */
	private int fOffset= 0;

	/**
	 * Creates a new comment range.
	 * 
	 * @param offset Offset of the range
	 * @param length Length of the range
	 */
	public CommentRange(int offset, int length) {
		fOffset= offset;
		fLength= length;
	}

	/**
	 * Changes the length of this range.
	 * 
	 * @param delta The increment/decrement to change the length of the range
	 */
	public final void changeLength(int delta) {
		fLength += delta;
	}

	/**
	 * Changes the offset of this range.
	 * 
	 * @param delta The increment/decrement to change the offset of the range
	 */
	public final void changeOffset(int delta) {
		fOffset += delta;
	}

	/*
	 * @see org.eclipse.jface.text.IRegion#getLength()
	 */
	public final int getLength() {
		return fLength;
	}

	/*
	 * @see org.eclipse.jface.text.IRegion#getOffset()
	 */
	public final int getOffset() {
		return fOffset;
	}

	/**
	 * Sets the length of this range.
	 * 
	 * @param length The length to set for this range
	 */
	public final void setLength(int length) {
		fLength= length;
	}

	/**
	 * Sets the offset of this range.
	 * 
	 * @param offset The offset to set for this range
	 */
	public final void setOffset(int offset) {
		fOffset= offset;
	}
}
