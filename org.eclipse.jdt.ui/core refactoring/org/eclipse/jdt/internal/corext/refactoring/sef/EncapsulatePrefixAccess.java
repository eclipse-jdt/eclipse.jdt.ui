/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.sef;

import org.eclipse.jdt.core.dom.PrefixExpression;

import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;

final class EncapsulatePrefixAccess extends SimpleTextEdit {
	
	public EncapsulatePrefixAccess(String getter, String setter, PrefixExpression  prefix) {
		super(prefix.getStartPosition(), prefix.getLength(), setter + "(" + getter + "() " + prefix.getOperator().toString().substring(0, 1) + " 1)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}	
	
	private EncapsulatePrefixAccess(TextRange range, String text) {
		super(range, text);
	}
	
	/* non Java-doc
	 * @see TextEdit#getCopy
	 */
	public TextEdit copy() {
		return new EncapsulatePrefixAccess(getTextRange().copy(), getText());
	}	
}