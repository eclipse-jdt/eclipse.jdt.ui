/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.sef;

import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.Reference;
import org.eclipse.jdt.internal.corext.textmanipulation.MultiTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;

final class EncapsulateWriteAccess extends MultiTextEdit {

	public EncapsulateWriteAccess(String setter, Reference lhs, Expression expression) {
		this(setter + "(", lhs.sourceStart, expression.sourceStart - lhs.sourceStart, expression.sourceEnd + 1);
	}
	
	public EncapsulateWriteAccess(String setter, int offset, int length, Expression expression) {
		this(setter + "(", offset, length, expression.sourceEnd + 1);
	}
	
	private EncapsulateWriteAccess(String text, int offset, int lhsLength, int closingBracket) {
		add(SimpleTextEdit.createReplace(offset, lhsLength, text));
		add(SimpleTextEdit.createInsert(closingBracket, ")"));
	}	
}

