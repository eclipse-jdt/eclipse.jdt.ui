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

public final class CopySourceEdit extends AbstractTransferEdit {

	private String fContent;
	private CopyTargetEdit fTarget;
	/* package */ int fCounter;

	public CopySourceEdit(int offset, int length) {
		this(new TextRange(offset, length));
	}

	public CopySourceEdit(int offset, int length, CopyTargetEdit target) {
		this(offset, length);
		setTargetEdit(target);
	}

	private CopySourceEdit(TextRange range) {
		super(range);
	}

	public void setTargetEdit(CopyTargetEdit edit) {
		if (fTarget != edit) {
			fTarget= edit;
			fTarget.setSourceEdit(this);
		}
	}
	
	/* non Java-doc
	 * @see TextEdit#copy
	 */	
	protected TextEdit copy0() throws CoreException {
		return new CopySourceEdit(getTextRange().copy());
	}

	public void perform(TextBuffer buffer) throws CoreException {
		TextRange range= getTextRange();
		fContent= buffer.getContent(range.getOffset(), range.getLength());
		fTarget.perform(buffer);
	}
	
	/* package */ void updateTextRange(int delta, List executedEdits) {
		predecessorExecuted(fTarget.getSuccessorIterator(), delta);
		fTarget.getTextRange().adjustLength(delta);
		fTarget.updateParents(delta);
	}
		
	/* package */ String getContent() {
		return fContent;
	}
	
	/* package */ void clearContent() {
		fContent= null;
	}
	
	/* package */ void checkRange(DocumentEvent event) {
		fTarget.checkRange(event);
	}
	
	/* package */ boolean checkEdit(int bufferLength) {
		return fTarget != null && fTarget.getSourceEdit() == this && super.checkEdit(bufferLength);
	}
	
	/* package */ CopyTargetEdit getTargetEdit() {
		return fTarget;
	}		
}
