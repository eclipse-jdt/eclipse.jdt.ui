/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.textmanipulation;

import org.eclipse.core.runtime.CoreException;

/**
 * A text edit that does nothing. A <code>NopTextEdit</code> can be used to track
 * positions when executing <code>TextEdits</code> associated with a <code>
 * TextBufferEditor</code>.
 */
public final class NopTextEdit extends TextEdit {
	
	private TextRange fTextRange;
	
	/**
	 * Creates a new <code>NopTextEdit</code> for the given
	 * offset and length.
	 * 
	 * @param offset the starting offset this text edit is "working on"
	 * @param length the length this text edit is "working on"
	 */
	public NopTextEdit(int offset, int length) {
		this(new TextRange(offset, length));
	}
	
	/**
	 * Creates a new <code>NopTextEdit</code> for the given
	 * range.
	 * 
	 * @param range the <code>TextRange</code> this text edit is "working on"
	 */
	public NopTextEdit(TextRange range) {
		fTextRange= range;
	}

	/* non Java-doc
	 * @see TextEdit#getTextRange
	 */	
	public final TextRange getTextRange() {
		return fTextRange;
	}

	/* non Java-doc
	 * @see TextEdit#perform
	 */	
	public final void perform(TextBuffer buffer) throws CoreException {
	}
	
	/* non Java-doc
	 * @see TextEdit#copy
	 */	
	protected TextEdit copy0(TextEditCopier copier) {
		return new NopTextEdit(fTextRange.copy());
	}	
}