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
package org.eclipse.jdt.internal.corext.textmanipulation.enhanced;

import org.eclipse.core.runtime.CoreException;

public final class CopyTargetEdit extends AbstractTransferEdit {

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
	protected TextEdit copy0() throws CoreException {
		return new CopyTargetEdit(getTextRange().copy());
	}

	public void perform(TextBuffer buffer) throws CoreException {
		if (++fSource.fCounter == 2 && !getTextRange().isDeleted()) {
			try {
				buffer.replace(getTextRange(), fSource.getContent());
			} finally {
				fSource.clearContent();
			}
		}
	}
	
	/* package */ boolean checkEdit(int bufferLength) {
		return fSource != null && fSource.getTargetEdit() == this && getTextRange().getLength() == 0 && super.checkEdit(bufferLength);
	}
	
	/* package */ CopySourceEdit getSourceEdit() {
		return fSource;
	}	
}
