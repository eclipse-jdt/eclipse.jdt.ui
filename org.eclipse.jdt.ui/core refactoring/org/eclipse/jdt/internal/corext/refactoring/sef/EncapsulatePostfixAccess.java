/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.sef;

import org.eclipse.jdt.core.dom.PostfixExpression;

import org.eclipse.jdt.internal.corext.textmanipulation.TextEditCopier;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;

final class EncapsulatePostfixAccess extends SimpleTextEdit {
	
	public EncapsulatePostfixAccess(String getter, String setter, PostfixExpression  postfix) {
		super(postfix.getStartPosition(), postfix.getLength(), setter + "(" + getter + "() " + postfix.getOperator().toString().substring(0, 1) + " 1)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}	
	
	private EncapsulatePostfixAccess(TextRange range, String text) {
		super(range, text);
	}
	
	/* non Java-doc
	 * @see TextEdit#copy0
	 */
	protected TextEdit copy0(TextEditCopier copier) {
		return new EncapsulatePostfixAccess(getTextRange().copy(), getText());
	}	
}