/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.sef;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.Reference;
import org.eclipse.jdt.internal.corext.codemanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.codemanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.TextPosition;
import org.eclipse.jdt.internal.corext.refactoring.Assert;

final class EncapsulateWriteAccess extends TextEdit {

	private String fSetter;
	private TextPosition fWriteAccess;
	private TextPosition fClosingBracket;
	
	private static final class UndoEncapsulateWriteAccess extends TextEdit {
		private String fSetter;
		private TextPosition fWriteAccess;
		private TextPosition fClosingBracket;
		
		public UndoEncapsulateWriteAccess(String setter, TextPosition write, TextPosition bracket) {
			fSetter= setter;
			fWriteAccess= write;
			fClosingBracket= bracket;
		}
		public TextEdit copy() {
			return new UndoEncapsulateWriteAccess(fSetter, fWriteAccess.copy(), fClosingBracket.copy()); 
		}
		public TextPosition[] getTextPositions() {
			return new TextPosition[] { fWriteAccess, fClosingBracket };
		}
		public TextEdit perform(TextBuffer buffer) throws CoreException {
			buffer.replace(fClosingBracket, "");
			String old= buffer.getContent(fWriteAccess.getOffset(), fWriteAccess.getLength());
			buffer.replace(fWriteAccess, fSetter);
			return new EncapsulateWriteAccess(old, fWriteAccess, fClosingBracket);
		}	
	}
	
	public EncapsulateWriteAccess(String setter, Reference lhs, Expression expression) {
		this(setter + "(", lhs.sourceStart, expression.sourceStart - lhs.sourceStart, expression.sourceEnd + 1);
	}
	
	public EncapsulateWriteAccess(String setter, int offset, int length, Expression expression) {
		this(setter + "(", offset, length, expression.sourceEnd + 1);
	}
	
	private EncapsulateWriteAccess(String text, int offset, int lhsLength, int closingBracket) {
		fSetter= text;
		fWriteAccess= new TextPosition(offset, lhsLength);
		fClosingBracket= new TextPosition(closingBracket, 0);
	}
	
	private EncapsulateWriteAccess(String text, TextPosition write, TextPosition bracket) {
		fSetter= text;
		fWriteAccess= write;
		fClosingBracket= bracket;
	}
	
	/* non Java-doc
	 * @see TextEdit#getCopy
	 */
	public TextEdit copy() {
		return new EncapsulateWriteAccess(fSetter, fWriteAccess.copy(), fClosingBracket.copy());
	}
	
	/* non Java-doc
	 * @see TextEdit#getTextPositions
	 */
	public TextPosition[] getTextPositions() {
		return new TextPosition[] { fWriteAccess, fClosingBracket };
	}
	
	/* non Java-doc
	 * @see TextEdit#doPerform
	 */
	public TextEdit perform(TextBuffer buffer) throws CoreException {
		String old= buffer.getContent(fWriteAccess.getOffset(), fWriteAccess.getLength());
		buffer.replace(fWriteAccess, fSetter);
		buffer.replace(fClosingBracket, ")");
		return new UndoEncapsulateWriteAccess(old, fWriteAccess, fClosingBracket);
	}		
}

