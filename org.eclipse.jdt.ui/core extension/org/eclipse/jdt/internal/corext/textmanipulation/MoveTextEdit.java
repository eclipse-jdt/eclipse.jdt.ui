/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.textmanipulation;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.internal.corext.Assert;

/**
 * A text edit that moves text inside a text buffer.
 */
public class MoveTextEdit extends TextEdit {
	
	/* package */ static class TargetMark extends NopTextEdit {
		private MoveTextEdit fMoveTextEdit;
		public TargetMark(TextRange range, MoveTextEdit edit) {
			super(range);
			fMoveTextEdit= edit;
		}
		/* package */ MoveTextEdit getMoveTextEdit() {
			return fMoveTextEdit;
		}
		public TextEdit perform(TextBuffer buffer) throws CoreException {
			fMoveTextEdit.internalPerform(buffer);
			return super.perform(buffer);
		}
		public TextEdit copy() {
			Assert.isTrue(false, "This should never happen"); //$NON-NLS-1$
			return super.copy();
		}
	}

	private TextRange fTarget;
	private TextRange fSource;
	private int fPerformCounter;

	/**
	 * Creates a new <code>MoveTextEdit</code>. The text edit doesn't support
	 * overlapping moves. So for a <code>MoveTextEdit</code> <code>destination &lt;= offset && 
	 * offset + length - 1 &lt;= destination</code> must be <code>true</code>.
	 * 
	 * @param offset the offset of the text to be moved
	 * @param length the text length to be moved
	 * @param destination the destination offset
	 */
	public MoveTextEdit(int offset, int length, int destination) {
		Assert.isTrue(destination <= offset || offset + length <= destination);
		fSource= new TextRange(offset, length);
		fTarget= new TextRange(destination);
	}

	/**
	 * Creates a new <code>MoveTextEdit</code> with the given source and target range.
	 * 
	 * @param source the source
	 * @param target the target
	 */
	private MoveTextEdit(TextRange source,TextRange target) {
		fSource= source;
		fTarget= target;
	}
	
	/**
	 * Returns the move text edit's source range. This method returns the same range
	 * as <code>TextEdit#getTextRange()</code>
	 * 
	 * @return the edit's source range
	 */
	public TextRange getSourceRange() {
		return fSource;
	}
	
	/**
	 * Returns the move text edit's target range.
	 * 
	 * @return the edit's target range
	 */
	public TextRange getTargetRange() {
		return fTarget;
	}
	
	/* non Java-doc
	 * @see TextEdit#getTextRange()
	 */
	public TextRange getTextRange() {
		return fSource;
	}

	/* non Java-doc
	 * @see TextEdit#connect(TextBufferEditor)
	 */
	public void connect(TextBufferEditor editor) throws CoreException {
		editor.add(new TargetMark(fTarget, this));
	}
	
	/* non Java-doc
	 * @see TextEdit#perform(TextBuffer)
	 */
	public TextEdit perform(TextBuffer buffer) throws CoreException {
		internalPerform(buffer);
		return new MoveTextEdit(fTarget, fSource);
	}

	/* non Java-doc
	 * @see TextEdit#copy()
	 */
	public TextEdit copy() {
		TextRange source= getSourceRange();
		TextRange target= getTargetRange();
		return new MoveTextEdit(source.fOffset, source.fLength, target.fOffset);
	}
	
	//---- Helper method ---------------------------------------------------------------------------------
	
	private void internalPerform(TextBuffer buffer) throws CoreException {
		Assert.isTrue(fPerformCounter < 2);
		if (++fPerformCounter == 2) {
			String current= getContent(buffer);
			buffer.replace(getSourceRange(), ""); //$NON-NLS-1$
			buffer.replace(getTargetRange(), current);
		}
	}
	
	protected String getContent(TextBuffer buffer) {
		TextRange source= getSourceRange();
		return buffer.getContent(source.fOffset, source.fLength);
	}
	
	/* package */ boolean isUpMove() {
		return fSource.fOffset < fTarget.fOffset;
	}
	
	/* package */ boolean isDownMove() {
		return fSource.fOffset > fTarget.fOffset;
	}
	
	/* package */ TextRange getChildRange() {
		int offset= fSource.fOffset;
		int length= fSource.fLength;
		int destination= fTarget.fOffset;
		if (destination <= offset)
			return new TextRange(destination, offset + length - destination);
		else
			return new TextRange(offset, destination - offset);
	}	
}

