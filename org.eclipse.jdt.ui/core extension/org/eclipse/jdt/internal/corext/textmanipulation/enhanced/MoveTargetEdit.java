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
			try {
				String source= fSource.getContent();
	
				if (!getTextRange().isDeleted()) {
					// Insert target
					fMode= INSERT;
					buffer.replace(getTextRange(), source);
				}
				
				// Delete source
				if (!fSource.getTextRange().isDeleted()) {
					fMode= DELETE;
					buffer.replace(fSource.getTextRange(), "");
				}
			} finally {
				fMode= UNDEFINED;
				fSource.clearContent();
			}
		}
	}

	/* non Java-doc
	 * @see TextEdit#copy
	 */	
	public TextEdit copy0() {
		return new MoveTargetEdit(getTextRange().copy());
	}
	
	/* package */ void updateTextRange(int delta, List executedEdits) {
		if (fMode == INSERT) {
			predecessorExecuted(getSuccessorIterator(), delta);
			getTextRange().adjustLength(delta);
			updateParents(delta);

			markAsDeleted(getChildren());
			
			List sourceChildren= fSource.getChildren();
			fSource.setChildren(null);
			int moveDelta= getTextRange().getOffset() - fSource.getTextRange().getOffset();
			move(sourceChildren, moveDelta); 
			setChildren(sourceChildren);
		} else if (fMode == DELETE) {
			predecessorExecuted(fSource.getSuccessorIterator(), delta);
			fSource.getTextRange().adjustLength(delta);
			fSource.updateParents(delta);
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
		if (fSource == null || fSource.getTargetEdit() != this || getTextRange().getLength() != 0)
			createErrorStatus("Incorrect MoveTargetEdit");
		return createOKStatus();
	}
	
	/* package */ MoveSourceEdit getSourceEdit() {
		return fSource;
	}				
}
