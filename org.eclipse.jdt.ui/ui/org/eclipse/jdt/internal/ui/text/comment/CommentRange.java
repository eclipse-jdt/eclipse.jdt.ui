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

import org.eclipse.jface.text.Position;

/**
 * Range in a comment region in comment region coordinates.
 * 
 * @since 3.0
 */
public class CommentRange extends Position {

	/**
	 * Creates a new comment range.
	 * 
	 * @param offset Offset of the range
	 * @param length Length of the range
	 */
	public CommentRange(final int offset, final int length) {
		super(offset, length);
	}

	/**
	 * Moves this comment range.
	 * 
	 * @param delta The delta to move the range
	 */
	public final void move(final int delta) {
		offset += delta;
	}

	/**
	 * Trims this comment range at the beginning.
	 * 
	 * @param delta Amount to trim the range
	 */
	public final void trimBegin(final int delta) {
		offset += delta;
		length -= delta;
	}

	/**
	 * Trims this comment range at the end.
	 * 
	 * @param delta Amount to trim the range
	 */
	public final void trimEnd(final int delta) {
		length += delta;
	}
}
