/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

// This is a layer breaker
import org.eclipse.jface.text.Position;

import org.eclipse.jdt.internal.core.Assert;

// Purpose of this class is to avoid contamination of clients with wrong imports.

public final class TextPosition extends Position {

	public static class Anchor {
		private String fValue;
		// no outside instance
		private Anchor(String value) {
			fValue= value;
		}
		public String toString() {
			return fValue;
		}
	}
	
	public static final Anchor NO_ANCHOR=		new Anchor("no anchor");
	public static final Anchor ANCHOR_LEFT=		new Anchor("anchor left");
	public static final Anchor ANCHOR_RIGHT= 	new Anchor("anchor right");

	private Anchor fAnchor;
	private boolean fIsDisabled;
	

	/**
	 * Creates a new insert position with the given offset and anchor
	 *
	 * @param offset the position offset, must be >= 0
	 * @param anchor the anchor mode of this insert mark
	 */
	public TextPosition(int offset, Anchor anchor) {
		super(offset);
		fAnchor= anchor;
	}
	
	/**
	 * Creates a new position with the given offset and length.
	 *
	 * @param offset the position offset, must be >= 0
	 * @param length the position length, must be >= 0
	 */
	public TextPosition(int offset, int length) {
		super(offset, length);
		fAnchor= NO_ANCHOR;
		Assert.isNotNull(fAnchor);
	}
	
	/**
	 * Creates a copy of this <code>TextPosition</code>.
	 * 
	 * @return a copy of this <code>TextPosition</code>
	 */
	public TextPosition copy() {
		return new TextPosition(getOffset(), getLength());
	}
	
	/**
	 * Returns the anchor type of the text position.
	 * 
	 * @return the anchor type
	 */
	public Anchor getAnchor() {
		return fAnchor;
	}
	 
	/* package */ void disable() {
		fIsDisabled= true;
	}
	
	/* package */ void enable() {
		fIsDisabled= false;
	}
	
	/* package */ boolean isEnabled() {
		return !fIsDisabled;
	}
	
	/* pacvkage */ boolean isDisabled() {
		return fIsDisabled;
	}
	
	/* package */ void setAnchor(Anchor anchor) {
		fAnchor= anchor;
	}
	
	/* package */ boolean isInsertionPoint() {
		return length == 0;
	}
	
	/* package */ boolean equals(TextPosition position) {
		return offset == position.offset && length == position.length;
	}

	/* package */ boolean isEqualInsertionPoint(TextPosition position)	{
		return length == 0 && position.length == 0 && offset == position.offset;
	}

	/* package */ boolean liesBehind(TextPosition position) {
		return offset >= position.offset + position.length;
	}

	/* package */ boolean isInsertionPointAt(int o) {
		return offset == o && length == 0;
	}
	
	/* non Java-doc
	 * @see Object#toString()
	 */
	public String toString() {
		StringBuffer buffer= new StringBuffer();
		if (isDeleted())
			buffer.append("<deleted> ");
		if (!isEnabled())
			buffer.append("<disabled> ");
		buffer.append("Offset: ");
		buffer.append(offset);
		if (length > 0) {
			buffer.append("Length: ");
			buffer.append(length);
		} else {
			buffer.append("Anchor: ");
			buffer.append(fAnchor.toString());
		}
		return buffer.toString();
	}
}

