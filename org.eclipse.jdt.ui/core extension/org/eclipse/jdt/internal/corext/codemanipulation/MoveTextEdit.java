/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.core.Assert;

/**
 * A text edit that moves text inside a text buffer. All text positions added by text edits which are enclosed by 
 * the text to be moved are moved as well.
 */
public class MoveTextEdit extends TextEdit {

	private TextPosition fSource;
	private TextPosition fTarget;

	/**
	 * Creates a new <code>MoveTextEdit</code>. The text edit doesn't support
	 * overlapping moves. So <code>target &lt;= offset && offset + length - 1 &lt;= target</code>
	 * must be <code>true</code>
	 * 
	 * @param offset the offset of the text to be moved
	 * @param length the text length to be moved
	 * @param target the destination offset
	 */
	public MoveTextEdit(int offset, int length, int target) {
		fSource= new TextPosition(offset, length);
		fTarget= new TextPosition(target, TextPosition.ANCHOR_RIGHT);
		Assert.isTrue(target <= offset || offset + length - 1 <= target);
	}

	protected MoveTextEdit(TextPosition source, TextPosition target) {
		fSource= source;
		Assert.isNotNull(fSource);
		fTarget= target;
		Assert.isNotNull(fTarget);
	}
	
	/*
	 * @see TextEdit#getTextPositions()
	 */
	public TextPosition[] getTextPositions() {
		return new TextPosition[] {fSource, fTarget};
	}

	/*
	 * @see TextEdit#perform(TextBuffer)
	 */
	public TextEdit perform(TextBuffer buffer) throws CoreException {
		String current= buffer.getContent(fSource.getOffset(), fSource.getLength());
		buffer.replace(fSource, "");
		buffer.replace(fTarget, current);
		fSource.setAnchor(TextPosition.ANCHOR_RIGHT);
		fTarget.setAnchor(TextPosition.NO_ANCHOR);
		return new MoveTextEdit(fTarget, fSource);
	}

	/*
	 * @see TextEdit#copy()
	 */
	public TextEdit copy() {
		return new MoveTextEdit(fSource.getOffset(), fSource.getLength(), fTarget.getOffset());
	}	
}

