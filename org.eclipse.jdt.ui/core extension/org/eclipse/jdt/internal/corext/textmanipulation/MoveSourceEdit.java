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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.text.DocumentEvent;

import org.eclipse.jdt.internal.corext.Assert;

public final class MoveSourceEdit extends AbstractTransferEdit {

	/* package */ int fCounter;
	private MoveTargetEdit fTarget;
	private ISourceModifier fModifier;
	
	private String fContent;
	private TextRange fContentRange;
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
	
	protected void connect(TextBuffer buffer) throws TextEditException {
		if (fTarget == null)
			throw new TextEditException(getParent(), this, TextManipulationMessages.getString("MoveSourceEdit.no_target")); //$NON-NLS-1$
		if (fTarget.getSourceEdit() != this)
			throw new TextEditException(getParent(), this, TextManipulationMessages.getString("MoveSourceEdit.different_source"));  //$NON-NLS-1$
	}
	
	/* non Java-doc
	 * @see TextEdit#perform
	 */	
	public void perform(TextBuffer buffer) throws CoreException {
		fCounter++;
		switch(fCounter) {
			// Position of move source > position of move target.
			// Hence MoveTarget does the actual move. Move Source
			// only deletes the content.
			case 1:
				fContent= getContent(buffer);
				fContentRange= getTextRange().copy();
				fContentChildren= internalGetChildren();
				fMode= DELETE;
				buffer.replace(fContentRange, ""); //$NON-NLS-1$
				// do this after executing the replace to be able to
				// compute the number of children.
				internalSetChildren(null);
				break;
			// Position of move source < position of move target.
			// Hence move source handles the delete and the 
			// insert at the target position.	
			case 2:
				fContent= getContent(buffer);
				fMode= DELETE;
				buffer.replace(getTextRange(), ""); //$NON-NLS-1$
				TextRange targetRange= fTarget.getTextRange();
				if (!targetRange.isDeleted()) {
					// Insert target
					fMode= INSERT;
					buffer.replace(targetRange, fContent);
				}
				clearContent();
				break;
			default:
				Assert.isTrue(false, "Should never happen"); //$NON-NLS-1$
		}
	}

	/* non Java-doc
	 * @see TextEdit#adjustOffset
	 */	
	public void adjustOffset(int delta) {
		if (fContentRange != null)
			fContentRange.addToOffset(delta);
		super.adjustOffset(delta);
	}
	
	/* non Java-doc
	 * @see TextEdit#copy
	 */	
	protected TextEdit copy0() {
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
	
	/* package */ TextRange getContentRange() {
		return fContentRange;
	}
	
	/* package */ void clearContent() {
		fContent= null;
		fContentChildren= null;
		fContentRange= null;
	}
	
	protected void updateTextRange(int delta, List executedEdits) {
		if (fMode == DELETE) {
			adjustLength(delta);
			updateParents(delta);
			if (fCounter == 1) {
				predecessorExecuted(executedEdits, getNumberOfChildren(), delta);
			} else {
				// only update the edits which are executed between the move source
				// and the move target. For all other edits nothing will change.
				// The children of the move source will be updte when moving them 
				// under the target edit
				for (int i= executedEdits.size() - 1 - getNumberOfChildren(); i >= 0; i--) {
					TextEdit edit= (TextEdit)executedEdits.get(i);
					edit.predecessorExecuted(delta);
					if (edit == fTarget)
						break;
				}
			}
		} else if (fMode == INSERT) {
			fTarget.adjustLength(delta);
			fTarget.updateParents(delta);
			
			fTarget.markChildrenAsDeleted();
			
			List children= internalGetChildren();
			internalSetChildren(null);
			int moveDelta= fTarget.getTextRange().getOffset() - getTextRange().getOffset();
			move(children, moveDelta);
			fTarget.internalSetChildren(children);
		} else {
			Assert.isTrue(false);
		}
	}
	
	/* package */ void checkRange(DocumentEvent event) {
		if (fMode == INSERT) {
			fTarget.checkRange(event);
		} else  {
			super.checkRange(event);
		}
	}
	
	//---- content management ------------------------------------------
	
	private String getContent(TextBuffer buffer) {
		TextRange range= getTextRange();
		String result= buffer.getContent(range.getOffset(), range.getLength());
		if (fModifier != null) {
			TextBuffer newBuffer= TextBuffer.create(result);
			Map editMap= new HashMap();
			TextEdit newEdit= createEdit(editMap);
			fModifier.addEdits(result, newEdit);
			TextBufferEditor editor= new TextBufferEditor(newBuffer);
			try {
				editor.add(newEdit);
				editor.performEdits(new NullProgressMonitor());
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			restorePositions(editMap, range.getOffset());
			result= newBuffer.getContent();
		}
		return result;
	}
	
	private TextEdit createEdit(Map editMap) {
		TextRange range= getTextRange();
		int delta= range.getOffset();
		MultiTextEdit result= new MultiTextEdit(0, range.getLength());
		// don't but the root edit into the edit map. The sourc edit
		// will be updated by the perform method.
		createEdit(this, result, editMap, delta);
		return result;
	}
	
	private static void createEdit(TextEdit source, TextEdit target, Map editMap, int delta) {
		TextEdit[] children= source.getChildren();
		for (int i= 0; i < children.length; i++) {
			TextEdit child= children[i];
			TextRange range= child.getTextRange();
			RangeMarker marker= new RangeMarker(range.getOffset() - delta, range.getLength());
			target.add(marker);
			editMap.put(marker, child);
			createEdit(child, marker, editMap, delta);
		}
	}
	
	private static void restorePositions(Map editMap, int delta) {
		for (Iterator iter= editMap.keySet().iterator(); iter.hasNext();) {
			TextEdit marker= (TextEdit)iter.next();
			TextEdit edit= (TextEdit)editMap.get(marker);
			TextRange markerRange= marker.getTextRange();
			TextRange editRange= edit.getTextRange();
			if (markerRange.isDeleted()) {
				editRange.markAsDeleted();
			} else {
				edit.adjustOffset(markerRange.getOffset() - editRange.getOffset() + delta);
				edit.adjustLength(markerRange.getLength() - editRange.getLength());
			}
		}
	}		
}
