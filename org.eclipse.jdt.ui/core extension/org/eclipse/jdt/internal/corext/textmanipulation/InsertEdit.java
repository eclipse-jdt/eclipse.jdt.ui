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
 * Text edit to insert a text at a given position in a 
 * document.
 */
public final class InsertEdit extends SimpleTextEdit {
	
	private String fText;
	
	/**
	 * Constructs a new insert edit.
	 * 
	 * @param offset the offset of the range to replace
	 * @param text the text to insert
	 */
	public InsertEdit(int offset, String text) {
		super(offset, 0);
		fText= text;
	}
	
	/**
	 * Copy constructor
	 * 
	 * @param other the edit to copy from
	 */
	private InsertEdit(InsertEdit other) {
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
		return new InsertEdit(this);
	}
}
