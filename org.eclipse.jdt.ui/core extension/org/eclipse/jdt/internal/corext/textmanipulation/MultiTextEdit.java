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

import org.eclipse.jdt.internal.corext.Assert;

public final class MultiTextEdit extends TextEdit {

	private TextRange fRange;
	
	private static final TextRange MAX_RANGE= new TextRange(0, Integer.MAX_VALUE);

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
		fRange= new TextRange(offset, length);
	}
	
	/**
	 * Copy constructor.
	 */
	private MultiTextEdit(MultiTextEdit other) {
		super(other);
		if (other.fRange != null) {
			fRange= new TextRange(other.fRange);
		}
	}

	/* non Java-doc
	 * @see TextEdit#getTextRange
	 */	
	public final TextRange getTextRange() {
		if (fRange != null)
			return fRange;
		if (getParent() == null) // not added yet
			return MAX_RANGE;
		return getChildrenTextRange();
	}

	/* (non-Javadoc)
	 * @see TextEdit#matches(java.lang.Object)
	 */
	public boolean matches(Object obj) {
		if (!(obj instanceof MultiTextEdit))
			return false;
		Assert.isTrue(MultiTextEdit.class == getClass(), "Subclasses must reimplement matches"); //$NON-NLS-1$
		MultiTextEdit other= (MultiTextEdit)obj;
		if (fRange != null)
			return fRange.equals(other.fRange);
		if (other.fRange != null)
			return false;
		return true;
	}
	
	/* non Java-doc
	 * @see TextEdit#perform
	 */	
	public final void perform(TextBuffer buffer) throws CoreException {
		// do nothing.
	}

	/* non Java-doc
	 * @see TextEdit#copy
	 */	
	protected TextEdit copy0() {
		Assert.isTrue(MultiTextEdit.class == getClass(), "Subclasses must reimplement copy0"); //$NON-NLS-1$
		return new MultiTextEdit(this);
	}

	/* non Java-doc
	 * @see TextEdit#createPlaceholder
	 */	
	protected TextEdit createPlaceholder() {
		if (fRange == null)
			return new MultiTextEdit();
		return new MultiTextEdit(fRange.getOffset(), fRange.getLength());
	}
	
	/* non Java-doc
	 * @see TextEdit#adjustOffset
	 */	
	public void adjustOffset(int delta) {
		if (fRange != null)
			fRange.addToOffset(delta);
	}
	
	/* non Java-doc
	 * @see TextEdit#adjustLength
	 */	
	public void adjustLength(int delta) {
		if (fRange != null)
			fRange.addToLength(delta);
	}
	
	/* package */ void aboutToBeAdded(TextEdit parent) {
		if (fRange == null) {
			if (hasChildren())
				fRange= getChildrenTextRange();
			else
				fRange= new TextRange(parent.getTextRange().getOffset(), 0);
		}
	}	
}
