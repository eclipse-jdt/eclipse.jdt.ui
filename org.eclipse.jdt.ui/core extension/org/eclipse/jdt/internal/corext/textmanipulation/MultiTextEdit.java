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

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.IDocument;

public class MultiTextEdit extends TextEdit {

	private boolean fDefined;

	/**
	 * Creates a new <code>MultiTextEdit</code>. The range
	 * of the edit is determined by the range of its children.
	 * 
	 * Adding this edit to a parent edit sets its range to the
	 * range covered by its children. If the edit doesn't have 
	 * any children its offset is set to the parent's offset 
	 * and its length is set to 0.
	 */
	public MultiTextEdit() {
		super(0, Integer.MAX_VALUE);
		fDefined= false;
	}
	
	/**
	 * Creates a new </code>MultiTextEdit</code> for the given
	 * range. Adding a child to this edit which isn't covered 
	 * by the given range will result in an exception.
	 * 
	 * @param offset the edit's offset
	 * @param length the edit's length.
	 * @see TextEdit#add(TextEdit)
	 */
	public MultiTextEdit(int offset, int length) {
		super(offset, length);
		fDefined= true;
	}
	
	/**
	 * Copy constructor.
	 */
	private MultiTextEdit(MultiTextEdit other) {
		super(other);
	}

	/* non Java-doc
	 * @see TextEdit#perform
	 */	
	public final void perform(IDocument document) {
		// do nothing.
	}

	/* non Java-doc
	 * @see TextEdit#copy
	 */	
	protected TextEdit doCopy() {
		Assert.isTrue(MultiTextEdit.class == getClass(), "Subclasses must reimplement copy0"); //$NON-NLS-1$
		return new MultiTextEdit(this);
	}
	
	/* package */ void aboutToBeAdded(TextEdit parent) {
		if (!fDefined) {
			if (hasChildren()) {
				TextRange region= getTextRange(getChildren());
				setOffset(region.getOffset());
				setLength(region.getLength());
			} else {
				setOffset(parent.getOffset());
				setLength(0);
			}
		}
	}	
}
