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
package org.eclipse.text.edits;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * A <tt>CopyRangeMarker</tt> can be used to track positions when executing 
 * text edits. Additionally a copy range marker stores a local copy of the text 
 * it captures when it gets executed.
 * 
 * @since 3.0
 */
public final class CopyRangeMarker extends TextEdit {
	
	private String fText;
	
	/**
	 * Creates a new <tt>CopyRangeMarker</tt> for the given
	 * offset and length.
	 * 
	 * @param offset the marker's offset
	 * @param length the marker's length
	 */
	public CopyRangeMarker(int offset, int length) {
		super(offset, length);
	}
		
	/*
	 * Copy constructor
	 */
	private CopyRangeMarker(CopyRangeMarker other) {
		super(other);
		fText= other.fText;
	}
	
	/* non Java-doc
	 * @see TextEdit#doCopy
	 */	
	protected TextEdit doCopy() {
		return new CopyRangeMarker(this);
	}	

	/* non Java-doc
	 * @see TextEdit#perform
	 */	
	/* package */ final void perform(IDocument document) throws BadLocationException {
		fText= document.get(getOffset(), getLength());
	}
}
