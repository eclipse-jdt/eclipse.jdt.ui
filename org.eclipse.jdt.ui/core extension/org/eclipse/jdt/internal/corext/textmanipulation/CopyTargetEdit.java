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
package org.eclipse.jdt.internal.corext.textmanipulation;

import org.eclipse.jface.text.IDocument;

public final class CopyTargetEdit extends AbstractTransferEdit {

	private CopySourceEdit fSource;

	public CopyTargetEdit(int offset) {
		super(offset, 0);
	}

	public CopyTargetEdit(int offset, CopySourceEdit source) {
		this(offset);
		setSourceEdit(source);
	}

	private CopyTargetEdit(CopyTargetEdit other) {
		super(other);
	}

	public CopySourceEdit getSourceEdit() {
		return fSource;
	}
		
	public void setSourceEdit(CopySourceEdit edit) {
		if (fSource != edit) {
			fSource= edit;
			fSource.setTargetEdit(this);
		}
	}
	
	/* (non-Javadoc)
	 * @see TextEdit#matches(java.lang.Object)
	 */
	public boolean matches(Object obj) {
		if (!(obj instanceof CopyTargetEdit))
			return false;
		CopyTargetEdit other= (CopyTargetEdit)obj;
		if (!internalMatches(other))
			return false;
		if (fSource != null)
			return fSource.internalMatches(other.fSource);
		if (other.fSource != null)
			return false;
		return true;
	}
	
	/* package */ boolean internalMatches(CopyTargetEdit other) {
		return fRange.equals(other.fRange);
	}
	
	/* non Java-doc
	 * @see TextEdit#copy
	 */	
	protected TextEdit copy0() {
		return new CopyTargetEdit(this);
	}

	/* non Java-doc
	 * @see TextEdit#postProcessCopy
	 */	
	protected void postProcessCopy(TextEditCopier copier) {
		if (fSource != null) {
			((CopyTargetEdit)copier.getCopy(this)).setSourceEdit((CopySourceEdit)copier.getCopy(fSource));
		}
	}
	
	protected void connect(IDocument buffer) throws IllegalEditException {
		if (fSource == null)
			throw new IllegalEditException(getParent(), this, TextManipulationMessages.getString("CopyTargetEdit.no_source")); //$NON-NLS-1$
		if (fSource.getTargetEdit() != this)
			throw new IllegalEditException(getParent(), this, TextManipulationMessages.getString("CopyTargetEdit.different_target")); //$NON-NLS-1$
	}
	
	public void perform(IDocument document) throws PerformEditException {
		if (++fSource.fCounter == 2 && !getTextRange().isDeleted()) {
			try {
				performReplace(document, getTextRange(), getSourceContent());
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
}
