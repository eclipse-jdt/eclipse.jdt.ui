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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

public final class CopySourceEdit extends AbstractTransferEdit {

	private String fContent= ""; //$NON-NLS-1$
	private CopyTargetEdit fTarget;
	/* package */ int fCounter;
	private ISourceModifier fModifier;

	public CopySourceEdit(int offset, int length) {
		super(offset, length);
	}

	public CopySourceEdit(int offset, int length, CopyTargetEdit target) {
		this(offset, length);
		setTargetEdit(target);
	}

	protected CopySourceEdit(CopySourceEdit other) {
		super(other);
		if (other.fModifier != null)
			fModifier= other.fModifier.copy();
	}

	public CopyTargetEdit getTargetEdit() {
		return fTarget;
	}
	
	public void setTargetEdit(CopyTargetEdit edit) {
		if (fTarget != edit) {
			fTarget= edit;
			fTarget.setSourceEdit(this);
		}
	}
	
	public void setSourceModifier(ISourceModifier modifier) {
		fModifier= modifier;
	}
	
	public ISourceModifier getSourceModifier() {
		return fModifier;
	}
	
	/* non Java-doc
	 * @see TextEdit#copy
	 */	
	protected TextEdit doCopy() {
		return new CopySourceEdit(this);
	}

	/* non Java-doc
	 * @see TextEdit#postProcessCopy
	 */	
	protected void postProcessCopy(TextEditCopier copier) {
		if (fTarget != null) {
			((CopySourceEdit)copier.getCopy(this)).setTargetEdit((CopyTargetEdit)copier.getCopy(fTarget));
		}
	}
	
	protected void checkIntegrity() throws MalformedTreeException {
		if (fTarget == null)
			throw new MalformedTreeException(getParent(), this, EditMessages.getString("CopySourceEdit.no_target")); //$NON-NLS-1$
		if (fTarget.getSourceEdit() != this)
			throw new MalformedTreeException(getParent(), this, EditMessages.getString("CopySourceEdit.different_source")); //$NON-NLS-1$
	}
	
	/* package */ void perform(IDocument document) throws PerformEditException {
		fContent= getContent(document);
		if (++fCounter == 2 && !fTarget.isDeleted()) {
			try {
				performReplace(document, fTarget.getRegion(), fContent);
			} finally {
				clearContent();
			}
		}
	}

	private String getContent(IDocument document) throws PerformEditException {
		try {
			IRegion range= getRegion();
			String result= document.get(range.getOffset(), range.getLength());
			if (fModifier != null) {
				IDocument newDocument= new Document(result);
				TextEdit newEdit= new MultiTextEdit(0, range.getLength());
				fModifier.addEdits(result, newEdit);
				EditProcessor processor= new EditProcessor(newDocument);
				processor.add(newEdit);
				processor.performEdits();
				result= newDocument.get();
			}
			return result;
		} catch (MalformedTreeException e) {
			throw new PerformEditException(this, e.getMessage(), e);
		} catch (BadLocationException e) {
			throw new PerformEditException(this, e.getMessage(), e);
		}
	}
	
	/* package */ void update(DocumentEvent event, TreeIterationInfo info) {
		// Executing the copy source edit means inserting the text
		// at target position. So we have to update the edits around
		// the target.
		fTarget.update(event);
	}
	
	/* package */ String getContent() {
		return fContent;
	}
	
	/* package */ void clearContent() {
		fContent= null;
	}
}
