/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.internal.core.Assert;

public abstract class SimpleTextEdit extends TextEdit {

	private TextPosition fPosition;
	private String fText;

	public static TextEdit createReplace(int offset, int length, String text) {
		return new SimpleTextEditImpl(offset, length, text);
	}

	public static TextEdit createInsert(int offset, String text) {
		return new SimpleTextEditImpl(offset, 0, text);
	}
	
	public static TextEdit createDelete(int offset, int length) {
		return new SimpleTextEditImpl(offset, length, "");
	}
	
	private final static class SimpleTextEditImpl extends SimpleTextEdit {
		protected SimpleTextEditImpl(TextPosition position, String text) {
			super(position, text);
		}
		protected SimpleTextEditImpl(int offset, int length, String text) {
			super(offset, length, text);
		}
		public TextEdit copy() {
			return new SimpleTextEditImpl(getTextPosition().copy(), getText());
		}	
	}
	
	protected SimpleTextEdit() {
	}
	
	protected SimpleTextEdit(int offset, int length, String text) {
		this(new TextPosition(offset, length), text);
	}
	protected SimpleTextEdit(TextPosition position, String text) {
		fPosition= position;
		fText= text;
	}
	
	/**
	 * Sets the text edit's text
	 * 
	 * @param text the text edit's text
	 */	
	protected final void setText(String text) {
		fText= text;
		Assert.isNotNull(fText);
	}
	
	/**
	 * Returns the text edit's text
	 * 
	 * @return the text edit's text
	 */
	protected String getText() {
		return fText;
	}
		
	/**
	 * Sets the text edit's position. This method must not be called
	 * after the this edit has been connected to a <code>ITextBuffer</code>.
	 * 
	 * @param length the text edit's position.
	 */	
	protected void setTextPosition(TextPosition position) {
		fPosition= position;
		Assert.isNotNull(fPosition);
	}
	
	/**
	 * Returns the text edit's position.
	 * 
	 * @return the text edit's position
	 */
	protected TextPosition getTextPosition() {
		return fPosition;
	}
	
	/* non Java-doc
	 * @see TextEdit#getTextPositions
	 */
	public final TextPosition[] getTextPositions() {
		if (fPosition == null)
			return new TextPosition[0];
		return new TextPosition[] { fPosition };
	}
	
	/* non Java-doc
	 * @see TextEdit#doPerform
	 */
	public final TextEdit perform(TextBuffer buffer) throws CoreException {
		String current= buffer.getContent(fPosition.getOffset(), fPosition.getLength());
		buffer.replace(fPosition, fText);
		return new SimpleTextEditImpl(fPosition, current);
	}	
}

