/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.sef;

import org.eclipse.jdt.core.dom.SimpleName;

import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;

final class EncapsulateReadAccess extends SimpleTextEdit {

	public EncapsulateReadAccess(String getter, SimpleName node) {
		this(getter, node.getStartPosition(), node.getLength());
	}
	
	public EncapsulateReadAccess(String getter, int offset, int length) {
		super(offset, length, getter + "()"); //$NON-NLS-1$
	}
	
	private EncapsulateReadAccess(TextRange range, String text) {
		super(range, text);
	}
	
	/* non Java-doc
	 * @see TextEdit#getCopy
	 */
	public TextEdit copy() {
		return new EncapsulateReadAccess(getTextRange().copy(), getText());
	}	
}

