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

import org.eclipse.jface.text.IDocument;

/**
 * A <tt>RangeMarker</tt> can be used to track positions when executing 
 * text edits.
 */
public final class RangeMarker extends TextEdit {
	
	private TextRange fRange;
	
	/**
	 * Creates a new <tt>RangeMarker</tt> for the given
	 * offset and length.
	 * 
	 * @param offset the starting offset this text edit is "working on"
	 * @param length the length this text edit is "working on"
	 */
	public RangeMarker(int offset, int length) {
		fRange= new TextRange(offset, length);
	}
	
	/**
	 * Creates a new <tt>RangeMarker</tt> for the given range.
	 * 
	 * @param range the <code>TextRange</code> this text edit is "working on"
	 */
	public RangeMarker(TextRange range) {
		fRange= new TextRange(range);
	}
	
	/**
	 * Copy constructor
	 */
	private RangeMarker(RangeMarker other) {
		super(other);
		fRange= new TextRange(other.fRange);
	}

	/* non Java-doc
	 * @see TextEdit#getTextRange
	 */	
	public final TextRange getTextRange() {
		return fRange;
	}

	/* (non-Javadoc)
	 * @see TextEdit#matches(java.lang.Object)
	 */
	public boolean matches(Object obj) {
		if (!(obj instanceof RangeMarker))
			return false;
		RangeMarker other= (RangeMarker)obj;
		return fRange.equals(other.getTextRange());
	}
	
	/* non Java-doc
	 * @see TextEdit#perform
	 */	
	public final void perform(IDocument document)  {
	}
	
	/* non Java-doc
	 * @see TextEdit#copy
	 */	
	protected TextEdit copy0() {
		return new RangeMarker(this);
	}	
}
