/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.text;

import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.refactoring.Assert;

/**
 * A simpel text change that replaces a text portion specified by start offset and length with 
 * another text string.
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public class SimpleReplaceTextChange extends SimpleTextChange {
	
	private int fLength;
	private String fText;
	private String fName;
	
	protected static final String EMPTY_STRING= ""; //$NON-NLS-1$
	
	private static final SimpleReplaceTextChange NULL_CHANGE= new SimpleReplaceTextChange("Null Change"); //$NON-NLS-1$

	/**
	 * Creates a replace text change. The offset and length is set to zero.
	 * The new text is the empty string.
	 *
	 * @param name the changes name. The name is mainly used to render the change in the 
	 *  user interface. The name can be <code>null</code>.
	 */
	public SimpleReplaceTextChange(String name) {
		this(name, 0, 0, null);
	}
	/**
	 * Creates a replace text change. The length is set to zero and the new
	 * text is the empty string.
	 *
	 * @param name the changes name. The name is mainly used to render the change in the 
	 *  user interface. The name can be <code>null</code>.
	 * @param offset the starting offset of this change. The offset must not be negative. 
	 * @see SimpleTextChange#setOffset(int)
	 */
	public SimpleReplaceTextChange(String name, int offset) {
		this(name, offset, 0, null);
	}
	
	/**
	 * Creates a replace text change.
	 *
	 * @param name the changes name. The name is mainly used to render the change in the 
	 *  user interface. The name can be <code>null</code>.
	 * @param offset the starting offset of this change. The offset must not be negative. 
	 * @param length the length of the text to be replaced. The length must not be negative.
	 * @param text the new text. The value can be <code>null</code> indicating a text deletion.
	 * @see SimpleTextChange#setOffset(int)
	 */
	public SimpleReplaceTextChange(String name, int offset, int length, String text) {
		super(offset);
		setName(name);
		setLength(length);
		setText(text);
	}

	/**
	 * Creates the undo change for this change.
	 *
	 * @param oldText the old text replaced by this change. <code>OldText</code> can be
	 * <code>null</code> indicating a text deletion.
	 */
	protected SimpleTextChange createUndoChange(String oldText) {
		return new SimpleReplaceTextChange(fName, getOffset(), fText.length(), oldText);
	}
	
	/**
	 * Sets the change's name.
	 *
	 * @param name the change's name. The name can be <code>null</code>
	 */
	public void setName(String name) {
		fName= name;
	}
	 
	/**
	 * Returns the length of the text to be replaced by this text change.
	 *
	 * @return the length of the text to be replaced.
	 */
	protected int getLength() {
		return fLength;
	}
	
	/**
	 * Sets the length of the text to be replaced by this text change.
	 *
	 * @param length the length of the text to be replaced.
	 */
	protected void setLength(int length) {
		fLength= length;
		Assert.isTrue(fLength >= 0);
	}
	
	/**
	 * Returns the new text used by this replace text change.
	 *
	 * @return the new text.
	 */
	protected String getText() {
		return fText;
	}
	 	
	/**
	 * Sets the new text used by this replace text change.
	 *
	 * @param the new text.
	 */
	protected void setText(String text) {
		fText= text;
		if (fText == null)
			fText= EMPTY_STRING;
	}
	 	
	/* (Non-Javadoc)
	 * Method declared in SimpleTextChange.
	 */
	protected final SimpleTextChange perform(ITextBuffer textBuffer) throws JavaModelException {
		if (!isActive() || (fLength == 0 && fText == EMPTY_STRING))
			return NULL_CHANGE;
		
		int offset= getOffset();
		String oldText= textBuffer.getContent(offset, fLength);
		if (oldText == null)
			throw new JavaModelException(null, IJavaModelStatusConstants.INDEX_OUT_OF_BOUNDS);
		try {
			textBuffer.replace(offset, fLength, fText);
			return createUndoChange(oldText);
		} catch (IndexOutOfBoundsException e) {
			throw new JavaModelException(null, IJavaModelStatusConstants.INDEX_OUT_OF_BOUNDS);
		}
	}
	
	/* (Non-Javadoc)
	 * Method declared in IChange.
	 */
	public String getName() {
		return fName;
	}
}