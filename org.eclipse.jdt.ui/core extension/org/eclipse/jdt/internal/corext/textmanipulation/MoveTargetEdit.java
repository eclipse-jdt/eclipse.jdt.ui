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

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;

public final class MoveTargetEdit extends AbstractTransferEdit {

	private MoveSourceEdit fSource;

	public MoveTargetEdit(int offset) {
		super(offset, 0);
	}

	public MoveTargetEdit(int offset, MoveSourceEdit source) {
		this(offset);
		setSourceEdit(source);
	}

	/**
	 * Copy constructor
	 */
	private MoveTargetEdit(MoveTargetEdit other) {
		super(other);
	}

	public MoveSourceEdit getSourceEdit() {
		return fSource;
	}
					
	public void setSourceEdit(MoveSourceEdit edit) {
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
	
	protected void checkIntegrity() {
		if (fSource == null)
			throw new MalformedTreeException(getParent(), this, TextManipulationMessages.getString("MoveTargetEdit.no_source")); //$NON-NLS-1$
		if (fSource.getTargetEdit() != this)
			throw new MalformedTreeException(getParent(), this, TextManipulationMessages.getString("MoveTargetEdit.different_target")); //$NON-NLS-1$
	}
	
	/* non Java-doc
	 * @see TextEdit#perform
	 */	
	public void perform(IDocument document) throws PerformEditException {
		if (++fSource.fCounter == 2) {
			String source= getSourceContent();
			fMode= INSERT;
			performReplace(document, getTextRange(), source);
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
	public TextEdit doCopy() {
		return new MoveTargetEdit(this);
	}
	
	/* non Java-doc
	 * @see TextEdit#postProcessCopy
	 */	
	protected void postProcessCopy(TextEditCopier copier) {
		if (fSource != null) {
			((MoveTargetEdit)copier.getCopy(this)).setSourceEdit((MoveSourceEdit)copier.getCopy(fSource));
		}
	}
	
	/* package */ void update(DocumentEvent event, TreeIterationInfo info) {
		if (fMode == INSERT) {
			// we have to substract the delta since <code>super.updateTextRange</code>
			// add the delta to the move source's children.
			int moveDelta= getOffset() - fSource.getContentOffset();
			
			super.update(event, info);			

			List sourceChildren= fSource.getContentChildren();
			move(sourceChildren, moveDelta); 
			internalSetChildren(sourceChildren);
			
		} else {
			Assert.isTrue(false);
		}
		
	}
	
//	protected void updateTextRange(int delta, List executedEdits) {
//		if (fMode == INSERT) {
//			// we have to substract the delta since <code>super.updateTextRange</code>
//			// add the delta to the move source's children.
//			int moveDelta= getTextRange().getOffset() - fSource.getContentRange().getOffset() - delta;
//			
//			markChildrenAsDeleted();
//
//			super.updateTextRange(delta, executedEdits);			
//
//			List sourceChildren= fSource.getContentChildren();
//			move(sourceChildren, moveDelta); 
//			internalSetChildren(sourceChildren);
//			
//		} else {
//			Assert.isTrue(false);
//		}
//	}	
}
