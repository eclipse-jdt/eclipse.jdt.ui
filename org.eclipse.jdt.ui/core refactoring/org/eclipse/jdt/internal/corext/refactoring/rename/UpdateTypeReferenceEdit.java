/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.rename;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEditCopier;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;

public final class UpdateTypeReferenceEdit extends SimpleTextEdit {

	private String fOldName;
	
	public UpdateTypeReferenceEdit(int offset, int length, String newName, String oldName) {
		super(offset, length, newName);
		Assert.isNotNull(oldName);
		fOldName= oldName;			
	}
	
	private UpdateTypeReferenceEdit(TextRange range, String newName, String oldName) {
		super(range, newName);
		Assert.isNotNull(oldName);
		fOldName= oldName;			
	}

	/* non Java-doc
	 * @see TextEdit#copy0
	 */
	protected TextEdit copy0(TextEditCopier copier) {
		return new UpdateTypeReferenceEdit(getTextRange().copy(), getText(), fOldName);
	}

	/* non Java-doc
	 * @see TextEdit#connect(TextBuffer)
	 */
	public void connect(TextBuffer buffer) throws CoreException {
		int offset= getTextRange().getOffset() + getTextRange().getLength() - fOldName.length();
		setTextRange(new TextRange(offset, fOldName.length()));
	}
}
