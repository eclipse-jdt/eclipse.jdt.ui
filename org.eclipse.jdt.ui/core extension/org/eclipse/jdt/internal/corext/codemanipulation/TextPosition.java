/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

// This is a layer breaker
import org.eclipse.jface.text.Position;


// Purpose of this class is to avoid contamination of clients with wrong imports.

public final class TextPosition extends Position {

	/**
	 * Creates a new position with the given offset and length 0.
	 *
	 * @param offset the position offset, must be >= 0
	 */
	public TextPosition(int offset) {
		super(offset);
	}
	
	/**
	 * Creates a new position with the given offset and length.
	 *
	 * @param offset the position offset, must be >= 0
	 * @param length the position length, must be >= 0
	 */
	public TextPosition(int offset, int length) {
		super(offset, length);
	}
	
	/**
	 * Creates a copy of this <code>TextPosition</code>.
	 * 
	 * @return a copy of this <code>TextPosition</code>
	 */
	public TextPosition copy() {
		return new TextPosition(getOffset(), getLength());
	}
}

