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

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.internal.corext.Assert;

public class SimpleTextEdit extends TextEdit {

	private TextRange fRange;
	private String fText;

	public static SimpleTextEdit createReplace(int offset, int length, String text) {
		return new SimpleTextEdit(offset, length, text);
	}

	public static SimpleTextEdit createInsert(int offset, String text) {
		return new SimpleTextEdit(offset, 0, text);
	}
	
	public static SimpleTextEdit createDelete(int offset, int length) {
		return new SimpleTextEdit(offset, length, ""); //$NON-NLS-1$
	}
	
	public SimpleTextEdit(int offset, int length, String text) {
		fRange= new TextRange(offset, length);
		fText= text;
	}
	
	/**
	 * Copy constructor
	 */
	protected SimpleTextEdit(SimpleTextEdit other) {
		super(other);
		fText= other.fText;
		fRange= new TextRange(other.fRange);
	}
	
	/**
	 * Returns the text edit's text
	 * 
	 * @return the text edit's text
	 */
	public String getText() {
		return fText;
	}
	
	/* (non-Javadoc)
	 * @see TextEdit#matches(java.lang.Object)
	 */
	public boolean matches(Object obj) {
		if (!(obj instanceof SimpleTextEdit))
			return false;
		SimpleTextEdit other= (SimpleTextEdit)obj;
		return fText.equals(other.getText()) && fRange.equals(other.getTextRange());
	}
		
	/* non Java-doc
	 * @see TextEdit#getTextRange
	 */
	public final TextRange getTextRange() {
		return fRange;
	}
	
	/* non Java-doc
	 * @see TextEdit#doPerform
	 */
	public void perform(TextBuffer buffer) throws CoreException {
		buffer.replace(fRange, fText);
	}
	
	/* non Java-doc
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return super.toString() + " <<" + fText; //$NON-NLS-1$
	}
	
	protected TextEdit copy0() {
		Assert.isTrue(SimpleTextEdit.class == getClass(), "Subclasses must reimplement copy0"); //$NON-NLS-1$
		return new SimpleTextEdit(this);
	}		
	
	protected void updateTextRange(int delta, List executedEdits) {
		markChildrenAsDeleted();
		super.updateTextRange(delta, executedEdits);
	}	
}

