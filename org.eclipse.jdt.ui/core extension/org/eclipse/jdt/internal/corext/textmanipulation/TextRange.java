/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.textmanipulation;

import org.eclipse.jdt.core.ISourceRange;

import org.eclipse.jdt.internal.corext.Assert;

public final class TextRange {

	private int fOffset;
	private int fLength;

	private static final int UNDEFINED_VALUE= -1;
	private static final int DELETED_VALUE= -2;
	
	public static final TextRange UNDEFINED= new TextRange(null, UNDEFINED_VALUE);
	public static final TextRange DELETED= new TextRange(null, DELETED_VALUE);

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
	 * Constructor for the undefined text range.
	 */
	private TextRange(TextRange dummy, int value) {
		fOffset= value;
		fLength= value;
	}
	
	public static TextRange createFromStartAndLength(int start, int length) {
		return new TextRange(start, length);
	}
	
	public static TextRange createFromStartAndInclusiveEnd(int start, int end) {
		return new TextRange(start, end - start + 1);
	}
	
	public static TextRange createFromStartAndExclusiveEnd(int start, int end) {
		return new TextRange(start, end - start);
	}
	
	/**
	 * Creates a new range from the given source range.
	 * 
	 * @range the source range denoting offset and length
	 */
	public TextRange(ISourceRange range) {
		this(range.getOffset(), range.getLength());
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
	
	/**
	 * Returns the inclusive end position of this range. That means that the end position
	 * denotes the last character of this range.
	 * 
	 * @return the inclusive end position
	 */
	public int getInclusiveEnd() {
		return fOffset + fLength - 1;
	}
	
	/**
	 * Returns the exclusive end position of this range. That means that the end position
	 * denotes the first character after this range.
	 * 
	 * @return the exclusive end position
	 */
	public int getExclusiveEnd() {
		return fOffset + fLength;
	}
	
	/**
	 * Creates a copy of this <code>TextRange</code>.
	 * 
	 * @return a copy of this <code>TextRange</code>
	 */
	public TextRange copy() {
		if (isUndefined())
			return this;
		if (isDeleted())
			return new TextRange(null, DELETED_VALUE);
		return new TextRange(fOffset, fLength);
	}
	
	/**
	 * Returns <code>true</code> if this text range is the <code>UNDEFINED</code>
	 * text range. Otherwise <code>false</code> is returned.
	 */
	public boolean isUndefined() {
		return UNDEFINED == this;
	}
	
	/**
	 * Returns <code>true</code> if the text represented by this <code>TextRange</code>
	 * got deleted. Otherwise <code>false</code> is returned.
	 */
	public boolean isDeleted() {
		return fOffset == DELETED_VALUE && fLength == DELETED_VALUE;
	}
	
	/**
	 * Checks if this <code>TextRange</code> is valid. For valid text range the following
	 * expression evaluates to <code>true</code>:
	 * <pre>
	 * 	getOffset() >= 0 && getLength() >= 0
	 * </pre>
	 * 
	 * @return <code>true</code> if this text range is a valid range. Otherwise <code>
	 * 	false</code>
	 */
	public boolean isValid() {
		return fOffset >= 0 && fLength >= 0;
	}
	
	/* non Java-doc
	 * @see Object#toString()
	 */
	public String toString() {
		if (isDeleted())
			return "[deleted]"; //$NON-NLS-1$
		if (isUndefined())
			return "[undefined]"; //$NON-NLS-1$
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
	
	/* package */ boolean isInsertionPoint() {
		return fLength == 0;
	}
	
	/* package */ boolean equals(TextRange range) {
		return fOffset == range.fOffset && fLength == range.fLength;
	}

	/* package */ boolean isEqualInsertionPoint(TextRange range)	{
		return fLength == 0 && range.fLength == 0 && fOffset == range.fOffset;
	}

	/* package */ boolean liesBehind(TextRange range) {
		return fOffset >= range.fOffset + range.fLength;
	}

	/* package */ boolean isInsertionPointAt(int o) {
		return fOffset == o && fLength == 0;
	}
	
	/* package */ boolean covers(TextRange other) {
		if (fLength == 0) {	// an insertion point can't cover anything
			return false;
		} else if (other.fLength == 0) {
			int otherOffset= other.fOffset;
			return fOffset < otherOffset && otherOffset < fOffset + fLength;
		} else {
			int otherOffset= other.fOffset;
			return fOffset <= otherOffset && otherOffset + other.fLength <= fOffset + fLength;
		}
	}
	
	/* package */ void markAsDeleted() {
		fOffset= DELETED_VALUE;
		fLength= DELETED_VALUE;
	}
	
	/* package */ void addToOffset(int delta) {
		if (isUndefined() || isDeleted())
			return;
		fOffset+= delta;
		Assert.isTrue(fOffset >= 0);
	}
	
	/* package */ void addToLength(int delta) {
		if (isUndefined() || isDeleted())
			return;
		fLength+= delta;
		Assert.isTrue(fLength >= 0);
	}
}

