/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.textmanipulation;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.internal.corext.Assert;

public class CopyTargetEdit extends AbstractTransferEdit {

	private CopySourceEdit fSource;

	public CopyTargetEdit(int offset) {
		this(new TextRange(offset, 0));
	}

	public CopyTargetEdit(int offset, CopySourceEdit source) {
		this(new TextRange(offset, 0));
		setSourceEdit(source);
	}

	private CopyTargetEdit(TextRange range) {
		super(range);
	}

	public void setSourceEdit(CopySourceEdit edit) {
		if (fSource != edit) {
			fSource= edit;
			fSource.setTargetEdit(this);
		}
	}
	
	/* non Java-doc
	 * @see TextEdit#copy
	 */	
	protected TextEdit copy0(TextEditCopier copier) {
		Assert.isTrue(CopyTargetEdit.class == getClass(), "Subclasses must reimplement copy0");
		return new CopyTargetEdit(getTextRange().copy());
	}

	/* non Java-doc
	 * @see TextEdit#postProcessCopy
	 */	
	protected void postProcessCopy(TextEditCopier copier) {
		if (fSource != null) {
			((CopyTargetEdit)copier.getCopy(this)).setSourceEdit((CopySourceEdit)copier.getCopy(fSource));
		}
	}
	
	public void perform(TextBuffer buffer) throws CoreException {
		if (++fSource.fCounter == 2 && !getTextRange().isDeleted()) {
			try {
				buffer.replace(getTextRange(), getSourceContent());
			} finally {
				fSource.clearContent();
			}
		}
	}
	
	/**
	 * Gets the content from the source edit.
	 */
	protected String getSourceContent() {
		return fSource.getContent();
	}
	
	
	/* package */ IStatus checkEdit(int bufferLength) {
		IStatus s= super.checkEdit(bufferLength);
		if (!s.isOK())
			return s;
		if (fSource == null || fSource.getTargetEdit() != this || getTextRange().getLength() != 0)
			return createErrorStatus("Incorrect CopyTargetEdit");
		return createOKStatus();
	}
	
	/* package */ CopySourceEdit getSourceEdit() {
		return fSource;
	}	
}
