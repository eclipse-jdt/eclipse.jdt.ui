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
package org.eclipse.jdt.internal.corext.textmanipulation;

import org.eclipse.jdt.internal.corext.Assert;

public final class TextRange {

	private int fOffset;
	private int fLength;

	/**
	 * Creates a insert position with the given offset.
	 *
	 * @param offset the position offset, must be >= 0
	 */
	public TextRange(int offset) {
		this(offset, 0);
	}
	
	/**
	 * Creates a new range with the given offset and length.
	 *
	 * @param offset the position offset, must be >= 0
	 * @param length the position length, must be >= 0
	 */
	public TextRange(int offset, int length) {
		fOffset= offset;
		Assert.isTrue(fOffset >= 0);
		fLength= length;
		Assert.isTrue(fLength >= 0);
	}
	
	/**
	 * Copy constrcutor
	 */
	public TextRange(TextRange other) {
		fOffset= other.fOffset;
		fLength= other.fLength;
	}
	
	/**
	 * Returns the offset of this range.
	 *
	 * @return the length of this range
	 */
	public int getOffset() {
		return fOffset;
	}
	
	/**
	 * Returns the length of this range.
	 *
	 * @return the length of this range
	 */
	public int getLength() {
		return fLength;
	}
	
	/* non Java-doc
	 * @see Object#toString()
	 */
	public String toString() {
		StringBuffer buffer= new StringBuffer();
		buffer.append("["); //$NON-NLS-1$
		buffer.append(fOffset);
		buffer.append(","); //$NON-NLS-1$
		buffer.append(fLength);
		buffer.append("]"); //$NON-NLS-1$
		return buffer.toString();
	}

	/* non Java-doc
	 * @see Object#equals()
	 */
	public boolean equals(Object obj) {
		if (! (obj instanceof TextRange))
			return false;
		TextRange other= (TextRange)obj;	
		return fOffset == other.fOffset && fLength == other.fLength;
	}

	/* non Java-doc
	 * @see Object#hashCode()
	 */
	public int hashCode() {
		return fOffset * fLength;
	}
	
	/* package */ boolean equals(TextRange range) {
		return fOffset == range.fOffset && fLength == range.fLength;
	}
}

