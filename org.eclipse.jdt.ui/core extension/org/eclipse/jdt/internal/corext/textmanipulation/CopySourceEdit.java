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

import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;

public final class CopySourceEdit extends AbstractTransferEdit {

	private String fContent;
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
	
	/* (non-Javadoc)
	 * @see TextEdit#matches(java.lang.Object)
	 */
	public boolean matches(Object obj) {
		if (!(obj instanceof CopySourceEdit))
			return false;
		CopySourceEdit other= (CopySourceEdit)obj;
		if (!internalMatches(other))
			return false;
		if (fTarget != null)
			return fTarget.internalMatches(other.fTarget);
		if (other.fTarget != null)
			return false;
		return true;
	}
	
	/* package */ boolean internalMatches(CopySourceEdit other) {
		return fRange.equals(other.fRange);
	}
	
	/* non Java-doc
	 * @see TextEdit#copy
	 */	
	protected TextEdit copy0() {
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
	
	protected void connect(IDocument buffer) throws IllegalEditException {
		if (fTarget == null)
			throw new IllegalEditException(getParent(), this, TextManipulationMessages.getString("CopySourceEdit.no_target")); //$NON-NLS-1$
		if (fTarget.getSourceEdit() != this)
			throw new IllegalEditException(getParent(), this, TextManipulationMessages.getString("CopySourceEdit.different_source")); //$NON-NLS-1$
	}
	
	public void perform(IDocument document) throws PerformEditException {
		fContent= getContent(document);
		TextRange targetRange= fTarget.getTextRange();
		if (++fCounter == 2 && !targetRange.isDeleted()) {
			try {
				performReplace(document, targetRange, fContent);
			} finally {
				clearContent();
			}
		}
	}

	private String getContent(IDocument document) throws PerformEditException {
		try {
			TextRange range= getTextRange();
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
		} catch (IllegalEditException e) {
			throw new PerformEditException(this, e.getMessage(), e);
		} catch (BadLocationException e) {
			throw new PerformEditException(this, e.getMessage(), e);
		}
	}
	
	protected void updateTextRange(int delta, List executedEdits) {
		boolean doIt= true;
		for (Iterator iter= executedEdits.iterator(); iter.hasNext() && doIt;) {
			TextEdit edit= (TextEdit)iter.next();
			if (edit == fTarget)
				doIt= false;
			if (doIt)
				edit.adjustOffset(delta);
		}
		fTarget.adjustLength(delta);
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
}
