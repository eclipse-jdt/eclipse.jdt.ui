/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.sef;

import org.eclipse.jdt.core.dom.SimpleName;

import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEditCopier;
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
	 * @see TextEdit#copy0
	 */
	protected TextEdit copy0(TextEditCopier copier) {
		return new EncapsulateReadAccess(getTextRange().copy(), getText());
	}	
}

