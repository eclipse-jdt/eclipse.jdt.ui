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

import java.util.List;

import org.eclipse.jface.text.DocumentEvent;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.internal.corext.Assert;

public final class MoveTargetEdit extends AbstractTransferEdit {

	private MoveSourceEdit fSource;

	public MoveTargetEdit(int offset) {
		super(new TextRange(offset, 0));
	}

	public MoveTargetEdit(int offset, MoveSourceEdit source) {
		this(offset);
		setSourceEdit(source);
	}

	private MoveTargetEdit(TextRange range) {
		super(range);
	}

	public void setSourceEdit(MoveSourceEdit edit) {
		if (fSource != edit) {
			fSource= edit;
			fSource.setTargetEdit(this);
		}
	}
	
	/* non Java-doc
	 * @see TextEdit#perform
	 */	
	public void perform(TextBuffer buffer) throws CoreException {
		if (++fSource.fCounter == 2) {
			String source= getSourceContent();

			fMode= INSERT;
			buffer.replace(getTextRange(), source);
		}
	}
	
	/**
	 * Gets the content from the source edit.
	 */
	protected String getSourceContent() {
		return fSource.getContent();
	}	

	/* non Java-doc
	 * @see TextEdit#copy0
	 */	
	public TextEdit copy0(TextEditCopier copier) {
		return new MoveTargetEdit(getTextRange().copy());
	}
	
	/* non Java-doc
	 * @see TextEdit#postProcessCopy
	 */	
	protected void postProcessCopy(TextEditCopier copier) {
		if (fSource != null) {
			((MoveTargetEdit)copier.getCopy(this)).setSourceEdit((MoveSourceEdit)copier.getCopy(fSource));
		}
	}
	
	protected void updateTextRange(int delta, List executedEdits) {
		if (fMode == INSERT) {
			// we have to substract the delta since <code>super.updateTextRange</code>
			// add the delta to the move source's children.
			int moveDelta= getTextRange().getOffset() - fSource.getContentRange().getOffset() - delta;
			
			markChildrenAsDeleted();

			super.updateTextRange(delta, executedEdits);			

			List sourceChildren= fSource.getContentChildren();
			move(sourceChildren, moveDelta); 
			setChildren(sourceChildren);
			
		} else {
			Assert.isTrue(false);
		}
	}
	
	/* package */ void checkRange(DocumentEvent event) {
		if (fMode == DELETE) {
			fSource.checkRange(event);
		} else {
			super.checkRange(event);
		}
	}
	
	/* package */ IStatus checkEdit(int bufferLength) {
		IStatus s= super.checkEdit(bufferLength);
		if (!s.isOK())
			return s;
		if (fSource == null)
			return createErrorStatus(TextManipulationMessages.getString("MoveTargetEdit.no_source")); //$NON-NLS-1$
		if (fSource.getTargetEdit() != this)
			return createErrorStatus(TextManipulationMessages.getString("MoveTargetEdit.different_target")); //$NON-NLS-1$
		if (getTextRange().getLength() != 0)
			return createErrorStatus(TextManipulationMessages.getString("MoveTargetEdit.length")); //$NON-NLS-1$
		return createOKStatus();
	}
	
	/* package */ MoveSourceEdit getSourceEdit() {
		return fSource;
	}				
}
