/*******************************************************************************
 * Copyright (c) 2000, 2001 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.sef;

import org.eclipse.jdt.core.dom.PostfixExpression;

import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEditCopier;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;

/**
 * Encapsulates a postfix expression into setter/getter calls
 * 
 * Contributors:
 * 
 * jens.lukowski@gmx.de: contributed code to convert prefix and postfix expressions
 *   into a combination of setter and getter calls.
 */
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