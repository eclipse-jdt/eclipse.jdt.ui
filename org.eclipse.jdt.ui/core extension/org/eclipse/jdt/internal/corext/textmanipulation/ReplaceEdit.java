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

/**
 * Text edit to replace a range in a document with a different
 * string
 */
public class ReplaceEdit extends SimpleTextEdit {
	
	private String fText;
	
	/**
	 * Constructs a new replace edit.
	 * 
	 * @param offset the offset of the range to replace
	 * @param length the length of the range to replace
	 * @param text the new text
	 */
	public ReplaceEdit(int offset, int length, String text) {
		super(offset, length);
		fText= text;
	}
	
	/**
	 * Copy constructor
	 * 
	 * @param other the edit to copy from
	 */
	protected ReplaceEdit(ReplaceEdit other) {
		super(other);
		fText= other.fText;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit#getText()
	 */
	public String getText() {
		return fText;
	}
	
	protected TextEdit doCopy() {
		return new ReplaceEdit(this);
	}		
}
