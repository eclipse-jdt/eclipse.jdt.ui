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
package org.eclipse.text.edits;

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
			TextEdit parent= getParent();
			while (parent != null) {
				if (parent == fSource)
					throw new MalformedTreeException(parent, this, "Source edit must not be the parent of the target.");
				parent= parent.getParent();
			}
		}
	}
	
	/* non Java-doc
	 * @see TextEdit#copy
	 */	
	protected TextEdit doCopy() {
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
	
	protected void checkIntegrity() throws MalformedTreeException {
		if (fSource == null)
			throw new MalformedTreeException(getParent(), this, EditMessages.getString("CopyTargetEdit.no_source")); //$NON-NLS-1$
		if (fSource.getTargetEdit() != this)
			throw new MalformedTreeException(getParent(), this, EditMessages.getString("CopyTargetEdit.different_target")); //$NON-NLS-1$
	}
	
	/* package */ void perform(IDocument document) throws PerformEditException {
		if (++fSource.fCounter == 2 && !isDeleted()) {
			try {
				performReplace(document, getRegion(), fSource.getContent());
			} finally {
				fSource.clearContent();
			}
		}
	}
}
