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
import org.eclipse.jface.text.IRegion;

/**
 * A <tt>CopyRangeMarker</tt> can be used to track positions when executing 
 * text edits. Additionally a copy range marker stores a local copy of the text 
 * it captures when it gets executed.
 */
public final class CopyRangeMarker extends TextEdit {
	
	private String fText;
	
	/**
	 * Creates a new <tt>CopyRangeMarker</tt> for the given
	 * offset and length.
	 * 
	 * @param offset the starting offset this text edit is "working on"
	 * @param length the length this text edit is "working on"
	 */
	public CopyRangeMarker(int offset, int length) {
		super(offset, length);
	}
	
	/**
	 * Creates a new <tt>CopyRangeMarker</tt> for the given range.
	 * 
	 * @param range the <code>TextRange</code> this text edit is "working on"
	 */
	public CopyRangeMarker(IRegion range) {
		super(range.getOffset(), range.getLength());
	}
	
	/**
	 * Copy constructor
	 */
	private CopyRangeMarker(CopyRangeMarker other) {
		super(other);
		fText= other.fText;
	}

	/* non Java-doc
	 * @see TextEdit#perform
	 */	
	/* package */ final void perform(IDocument document) throws PerformEditException {
		try {
			fText= document.get(getOffset(), getLength());
		} catch (BadLocationException e) {
			new PerformEditException(this, e.getMessage(), e);
		}
	}
	
	/* non Java-doc
	 * @see TextEdit#copy
	 */	
	protected TextEdit doCopy() {
		return new CopyRangeMarker(this);
	}	
}
