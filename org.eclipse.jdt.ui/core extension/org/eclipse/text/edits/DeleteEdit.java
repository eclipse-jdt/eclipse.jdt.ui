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


/**
 * Text edit to delete a range in a document.
 */
public final class DeleteEdit extends SimpleTextEdit {
	
	/**
	 * Constructs a new delete edit.
	 * 
	 * @param offset the offset of the range to replace
	 * @param length the length of the range to replace
	 */
	public DeleteEdit(int offset, int length) {
		super(offset, length);
	}
	
	/**
	 * Copy constructor
	 * 
	 * @param other the edit to copy from
	 */
	private DeleteEdit(DeleteEdit other) {
		super(other);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit#getText()
	 */
	public String getText() {
		return ""; //$NON-NLS-1$
	}
	
	protected TextEdit doCopy() {
		return new DeleteEdit(this);
	}
}
