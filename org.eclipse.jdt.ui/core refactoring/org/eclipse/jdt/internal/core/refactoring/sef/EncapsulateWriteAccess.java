/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.sef;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.Reference;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBuffer;
import org.eclipse.jdt.internal.core.refactoring.text.SimpleTextChange;
import sun.awt.OrientableFlowLayout;

public class EncapsulateWriteAccess extends SimpleTextChange {

	private static final String WRITE_ACCESS= "Encapsulate write access";
	
	static class UndoEncapsulateWriteAccess extends SimpleTextChange {
		
		private int fLength;
		private String fLhs;
		private int fClosingBracket;
		public UndoEncapsulateWriteAccess(int offset, int length, String lhs, int closingBracket) {
			super(offset);
			fLength= length;
			fLhs= lhs;
			fClosingBracket= closingBracket;
		}	
		protected SimpleTextChange perform(ITextBuffer buffer) throws JavaModelException {
			int offset= getOffset();
			String original= buffer.getContent(offset, fLength);
			buffer.replace(offset, fLength, fLhs);
			buffer.replace(fClosingBracket, 1, "");
			return new EncapsulateWriteAccess(offset, fLength, original, fClosingBracket);
		}
		
		public String getName() {
			return WRITE_ACCESS;
		}
	}

	private int fLength;	
	private String fSetter;
	private int fClosingBracket;
	
	public EncapsulateWriteAccess(String setter, Reference lhs, Expression expression) {
		super(lhs.sourceStart);
		fSetter= setter + "(";
		fLength= expression.sourceStart - getOffset();
		fClosingBracket= expression.sourceEnd + 1;
	}
	
	private EncapsulateWriteAccess(int offset, int length, String setter, int closingBracket) {
		super(offset);
		fLength= length;
		fSetter= setter;
		fClosingBracket= closingBracket;
	}
	
	protected SimpleTextChange perform(ITextBuffer buffer) throws JavaModelException {
		int offset= getOffset();
		String original= buffer.getContent(offset, fLength);
		buffer.replace(fClosingBracket, 0, ")");
		buffer.replace(offset, fLength, fSetter);
		return new UndoEncapsulateWriteAccess(offset, fSetter.length(), original, fClosingBracket);
	}
	
	public String getName() {
		return WRITE_ACCESS;
	}
}

