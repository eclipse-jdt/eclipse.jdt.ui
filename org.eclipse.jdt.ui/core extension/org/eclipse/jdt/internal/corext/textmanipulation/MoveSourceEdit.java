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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;

public final class MoveSourceEdit extends AbstractTransferEdit {

	/* package */ int fCounter;
	private MoveTargetEdit fTarget;
	private ISourceModifier fModifier;
	
	private String fContent;
	private int fContentOffset;
	private List fContentChildren;
	
	public MoveSourceEdit(int offset, int length) {
		super(offset, length);
	}

	public MoveSourceEdit(int offset, int length, MoveTargetEdit target) {
		this(offset, length);
		setTargetEdit(target);
	}

	/**
	 * Copy constructor
	 */
	protected MoveSourceEdit(MoveSourceEdit other) {
		super(other);
		if (other.fModifier != null)
			fModifier= other.fModifier.copy();
	}

	/**
	 * Sets the target edit.
	 * 
	 * @param edit the target edit. The target edit must
	 *  not be <code>null</code>
	 */	
	public void setTargetEdit(MoveTargetEdit edit) {
		fTarget= edit;
		fTarget.setSourceEdit(this);
	}
	
	public MoveTargetEdit getTargetEdit() {
		return fTarget;
	}
	
	public void setSourceModifier(ISourceModifier modifier) {
		fModifier= modifier;
	}
	
	public ISourceModifier getSourceModifier() {
		return fModifier;
	}
	
	protected void checkIntegrity() throws MalformedTreeException {
		if (fTarget == null)
			throw new MalformedTreeException(getParent(), this, TextManipulationMessages.getString("MoveSourceEdit.no_target")); //$NON-NLS-1$
		if (fTarget.getSourceEdit() != this)
			throw new MalformedTreeException(getParent(), this, TextManipulationMessages.getString("MoveSourceEdit.different_source"));  //$NON-NLS-1$
	}
	
	/* non Java-doc
	 * @see TextEdit#perform
	 */	
	public void perform(IDocument document) throws PerformEditException {
		fCounter++;
		switch(fCounter) {
			// Position of move source > position of move target.
			// Hence MoveTarget does the actual move. Move Source
			// only deletes the content.
			case 1:
				fContent= getContent(document);
				fContentOffset= getOffset();
				fContentChildren= internalGetChildren();
				fMode= DELETE;
				performReplace(document, ""); //$NON-NLS-1$
				// do this after executing the replace to be able to
				// compute the number of children.
				internalSetChildren(null);
				break;
				
			// Position of move source < position of move target.
			// Hence move source handles the delete and the 
			// insert at the target position.	
			case 2:
				fContent= getContent(document);
				fMode= DELETE;
				performReplace(document, ""); //$NON-NLS-1$
				if (!fTarget.isDeleted()) {
					// Insert target
					TextRange targetRange= fTarget.getTextRange();
					fMode= INSERT;
					performReplace(document, targetRange, fContent);
				}
				clearContent();
				break;
			default:
				Assert.isTrue(false, "Should never happen"); //$NON-NLS-1$
		}
	}

	/* non Java-doc
	 * @see TextEdit#copy
	 */	
	protected TextEdit doCopy() {
		return new MoveSourceEdit(this);
	}

	/* non Java-doc
	 * @see TextEdit#postProcessCopy
	 */	
	protected void postProcessCopy(TextEditCopier copier) {
		if (fTarget != null) {
			((MoveSourceEdit)copier.getCopy(this)).setTargetEdit((MoveTargetEdit)copier.getCopy(fTarget));
		}
	}
	
	/* package */ String getContent() {
		return fContent;
	}
	
	/* package */ List getContentChildren() {
		return fContentChildren;
	}
	
	/* package */ int getContentOffset() {
		return fContentOffset;
	}
	
	/* package */ void clearContent() {
		fContent= null;
		fContentChildren= null;
		fContentOffset= -1;
	}
	
	/* package */ void update(DocumentEvent event, TreeIterationInfo info) {
		if (fMode == DELETE) {			// source got deleted
			super.update(event, info); 
		} else if (fMode == INSERT) {	// text got inserted at target position
			fTarget.update(event);
			List children= internalGetChildren();
			if (children != null) {
				internalSetChildren(null);
				int moveDelta= fTarget.getOffset() - getOffset();
				move(children, moveDelta);
			}
			fTarget.internalSetChildren(children);
		} else {
			Assert.isTrue(false);
		}
	}
	
//	protected void updateTextRange(int delta, List executedEdits) {
//		if (fMode == DELETE) {
//			adjustLength(delta);
//			updateParents(delta);
//			if (fCounter == 1) {
//				predecessorExecuted(executedEdits, getNumberOfChildren(), delta);
//			} else {
//				// only update the edits which are executed between the move source
//				// and the move target. For all other edits nothing will change.
//				// The children of the move source will be updte when moving them 
//				// under the target edit
//				for (int i= executedEdits.size() - 1 - getNumberOfChildren(); i >= 0; i--) {
//					TextEdit edit= (TextEdit)executedEdits.get(i);
//					edit.predecessorExecuted(delta);
//					if (edit == fTarget)
//						break;
//				}
//			}
//		} else if (fMode == INSERT) {
//			fTarget.adjustLength(delta);
//			fTarget.updateParents(delta);
//			
//			fTarget.markChildrenAsDeleted();
//			
//			List children= internalGetChildren();
//			internalSetChildren(null);
//			int moveDelta= fTarget.getTextRange().getOffset() - getTextRange().getOffset();
//			move(children, moveDelta);
//			fTarget.internalSetChildren(children);
//		} else {
//			Assert.isTrue(false);
//		}
//	}
	
	//---- content management ------------------------------------------
	
	private String getContent(IDocument document) throws PerformEditException {
		try {
			TextRange range= getTextRange();
			String result= document.get(range.getOffset(), range.getLength());
			if (fModifier != null) {
				IDocument newDocument= new Document(result);
				Map editMap= new HashMap();
				TextEdit newEdit= createEdit(editMap);
				fModifier.addEdits(result, newEdit);
				EditProcessor processor= new EditProcessor(newDocument);
				processor.add(newEdit);
				processor.performEdits();
				restorePositions(editMap, range.getOffset());
				result= newDocument.get();
			}
			return result;
		} catch (MalformedTreeException e) {
			throw new PerformEditException(this, e.getMessage(), e);
		} catch (BadLocationException e) {
			throw new PerformEditException(this, e.getMessage(), e);
		}
	}
	
	private TextEdit createEdit(Map editMap) {
		int delta= getOffset();
		MultiTextEdit result= new MultiTextEdit(0, getLength());
		// don't but the root edit into the edit map. The sourc edit
		// will be updated by the perform method.
		createEdit(this, result, editMap, delta);
		return result;
	}
	
	private static void createEdit(TextEdit source, TextEdit target, Map editMap, int delta) {
		TextEdit[] children= source.getChildren();
		for (int i= 0; i < children.length; i++) {
			TextEdit child= children[i];
			RangeMarker marker= new RangeMarker(child.getOffset() - delta, child.getLength());
			target.add(marker);
			editMap.put(marker, child);
			createEdit(child, marker, editMap, delta);
		}
	}
	
	private static void restorePositions(Map editMap, int delta) {
		for (Iterator iter= editMap.keySet().iterator(); iter.hasNext();) {
			TextEdit marker= (TextEdit)iter.next();
			TextEdit edit= (TextEdit)editMap.get(marker);
			if (marker.isDeleted()) {
				edit.markAsDeleted();
			} else {
				edit.adjustOffset(marker.getOffset() - edit.getOffset() + delta);
				edit.adjustLength(marker.getLength() - edit.getLength());
			}
		}
	}		
}
