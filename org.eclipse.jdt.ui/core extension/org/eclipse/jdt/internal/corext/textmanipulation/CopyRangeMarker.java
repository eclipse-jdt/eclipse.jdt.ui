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

import org.eclipse.core.runtime.CoreException;

/**
 * A <tt>CopyRangeMarker</tt> can be used to track positions when executing 
 * text edits. Additionally a copy range marker stores a local copy of the text 
 * it captures when it gets executed.
 */
public final class CopyRangeMarker extends TextEdit {
	
	private TextRange fRange;
	private String fText;
	
	/**
	 * Creates a new <tt>CopyRangeMarker</tt> for the given
	 * offset and length.
	 * 
	 * @param offset the starting offset this text edit is "working on"
	 * @param length the length this text edit is "working on"
	 */
	public CopyRangeMarker(int offset, int length) {
		fRange= new TextRange(offset, length);
	}
	
	/**
	 * Creates a new <tt>CopyRangeMarker</tt> for the given range.
	 * 
	 * @param range the <code>TextRange</code> this text edit is "working on"
	 */
	public CopyRangeMarker(TextRange range) {
		fRange= new TextRange(range);
	}
	
	/**
	 * Copy constructor
	 */
	private CopyRangeMarker(CopyRangeMarker other) {
		super(other);
		fRange= new TextRange(other.fRange);
		fText= other.fText;
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
		if (!(obj instanceof CopyRangeMarker))
			return false;
		CopyRangeMarker other= (CopyRangeMarker)obj;
		if (!fRange.equals(other.getTextRange()))
			return false;
		if (fText != null)
			return fText.equals(other.fText);
		if (other.fText != null)
			return false;
		return true;
	}
	
	/* non Java-doc
	 * @see TextEdit#perform
	 */	
	public final void perform(TextBuffer buffer) throws CoreException {
		fText= buffer.getContent(fRange.getOffset(), fRange.getLength());
	}
	
	/* non Java-doc
	 * @see TextEdit#copy
	 */	
	protected TextEdit copy0() {
		return new CopyRangeMarker(this);
	}	
}
