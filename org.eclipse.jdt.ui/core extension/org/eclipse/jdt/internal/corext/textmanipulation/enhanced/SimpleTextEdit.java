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
package org.eclipse.jdt.internal.corext.textmanipulation.enhanced;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.internal.corext.Assert;

public abstract class SimpleTextEdit extends TextEdit {

	private TextRange fRange;
	private String fText;

	public static SimpleTextEdit createReplace(int offset, int length, String text) {
		return new SimpleTextEditImpl(offset, length, text);
	}

	public static SimpleTextEdit createInsert(int offset, String text) {
		return new SimpleTextEditImpl(offset, 0, text);
	}
	
	public static SimpleTextEdit createDelete(int offset, int length) {
		return new SimpleTextEditImpl(offset, length, ""); //$NON-NLS-1$
	}
	
	private final static class SimpleTextEditImpl extends SimpleTextEdit {
		protected SimpleTextEditImpl(TextRange range, String text) {
			super(range, text);
		}
		protected SimpleTextEditImpl(int offset, int length, String text) {
			super(offset, length, text);
		}
		protected TextEdit copy0() {
			return new SimpleTextEditImpl(getTextRange().copy(), getText());
		}	
	}
	
	protected SimpleTextEdit() {
		this(TextRange.UNDEFINED, ""); //$NON-NLS-1$
	}
	
	protected SimpleTextEdit(int offset, int length, String text) {
		this(new TextRange(offset, length), text);
	}
	protected SimpleTextEdit(TextRange range, String text) {
		Assert.isNotNull(range);
		Assert.isNotNull(text);
		fRange= range;
		fText= text;
	}
	
	/**
	 * Returns the text edit's text
	 * 
	 * @return the text edit's text
	 */
	public String getText() {
		return fText;
	}
		
	/**
	 * Sets the text edit's text
	 * <p>
	 * This method should only be called from within the <code>
	 * connect</code> method.
	 * 
	 * @param text the text edit's text
	 */	
	protected final void setText(String text) {
		Assert.isTrue(text != null && !isConnected());
		fText= text;
	}
	
	/**
	 * Sets the text edit's range.
	 * <p>
	 * This method should only be called from within the <code>
	 * connect</code> method.
	 * 
	 * @param range the text edit's range.
	 */	
	protected final void setTextRange(TextRange range) {
		Assert.isTrue(range != null && !isConnected());
		fRange= range;
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
	public final void perform(TextBuffer buffer) throws CoreException {
		String current= buffer.getContent(fRange.getOffset(), fRange.getLength());
		buffer.replace(fRange, fText);
	}
	
	/* package */ void updateTextRange(int delta, List executedEdits) {
		super.updateTextRange(delta, executedEdits);
		markAsDeleted(getChildren());
	}	
}

