/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.sef;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.Reference;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBuffer;
import org.eclipse.jdt.internal.core.refactoring.text.SimpleReplaceTextChange;
import org.eclipse.jdt.internal.core.refactoring.text.SimpleTextChange;
import sun.awt.OrientableFlowLayout;

public class EncapsulateWriteAccess extends SimpleReplaceTextChange {

	private static final String WRITE_ACCESS= "Encapsulate write access";
	
	private int fLength;	
	private String fSetter;
	private int fClosingBracket;
	
	public EncapsulateWriteAccess(String setter, Reference lhs, Expression expression) {
		this(setter, lhs.sourceStart, expression.sourceStart - lhs.sourceStart, expression.sourceEnd + 1);
	}
	
	public EncapsulateWriteAccess(String setter, int offset, int length, Expression expression) {
		this(setter, offset, length, expression.sourceEnd + 1);
	}
	
	public EncapsulateWriteAccess(String setter, int offset, int length, int closingBracket) {
		super(WRITE_ACCESS, offset, length, setter + "(");
		fClosingBracket= closingBracket;
	}
	
	protected SimpleTextChange[] adjust(ITextBuffer buffer) throws JavaModelException {
		return new SimpleTextChange[] {
				new SimpleReplaceTextChange("Add closing bracket", fClosingBracket, 0, ")")
			};
	}
}

