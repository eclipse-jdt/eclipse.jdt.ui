/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import java.util.List;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.Position;

import org.eclipse.jdt.internal.core.Assert;

/* package */ final class PositionUpdater implements IPositionUpdater {

	private DefaultPositionUpdater fDefaultUpdater;
	private int fOffset, fLength, fEnd, fNewLength, fDelta;
	private TextEdit fActiveTextEdit;
	private TextPosition fActiveTextPosition;
	private int fMoveStep;
	
	/* package */ static final String CATEGORY= PositionUpdater.class.getName();
	
	/* package */ PositionUpdater() {
		fDefaultUpdater= new DefaultPositionUpdater(CATEGORY);
	}
	
	/* package */ void setActiveTextPosition(TextPosition position) {
		fActiveTextPosition= position;
	}
	
	/* package */ void setActiveTextEdit(TextEdit currentTextEdit) {
		fActiveTextEdit= currentTextEdit;
	}
	
	//---- Position updating -----------------------------------------------------------------------
	
	public void update(DocumentEvent event) {
		if (!isTextBufferEditorMode())
			fDefaultUpdater.update(event);
			
		try {
			Position[] temp= event.getDocument().getPositions(CATEGORY);
			if (temp.length == 0)
				return;
			TextPosition[] positions= new TextPosition[temp.length];
			System.arraycopy(temp, 0, positions, 0, temp.length);
			fOffset= event.getOffset();
			fLength= event.getLength();
			fEnd= fOffset + fLength - 1;
			fNewLength= (event.getText() == null ? 0 : event.getText().length());
			fDelta= fNewLength - fLength;
			boolean isMoveDelete= isMoveDelete();
			boolean isMoveInsert= isMoveInsert();
			for (int i= 0; i < positions.length; i++) {
				TextPosition position= positions[i];
				if (!isAffected(position, isMoveInsert) || position.isDeleted())
					continue;
				if (isMoveDelete) {
					handleMoveDelete(position);
				} else if (isMoveInsert) {
					handleMoveInsert(position);
				} else {
					handleDefault(position);
				}
				correctValues(position);
			}
			if (isMoveDelete) {
				fMoveStep++;
			} else if (isMoveInsert) {
				fMoveStep= 0;
			}	
		} catch (BadPositionCategoryException e) {
			// Should not happen.
		}
	}
	
	private void handleMoveDelete(TextPosition position) {
		int positionOffset= position.offset;
		int positionLength= position.length;
		int positionEnd= positionOffset + positionLength - 1;

		// Do we have an insertion point
		if (positionLength == 0) {
			if(	positionOffset == fOffset && position.getAnchor() == TextPosition.ANCHOR_RIGHT ||
				 	positionOffset == fEnd + 1 && position.getAnchor() == TextPosition.ANCHOR_LEFT) {
				position.disable();
				position.setOffset(positionOffset - fOffset);
			} else {
				handleDefault(position);
			}
		// It is an position covered by the range to be moved
		} else if (fOffset <= positionOffset && positionEnd <= fEnd && !isMoveSourcePosition(position)) {
			position.disable();
			position.setOffset(positionOffset - fOffset);
		} else {
			handleDefault(position);
		}
	}

	private void handleMoveInsert(TextPosition position) {
		if (!position.isEnabled()) {
			position.enable();
			position.setOffset(position.getOffset() + fOffset);
		} else {
			handleDefault(position);
		}
	}

	private void handleDefault(TextPosition position) {
		Assert.isTrue(position.isEnabled());
		int positionOffset= position.offset;
		int positionLength= position.length;
		int positionEnd= positionOffset + positionLength;
		
		if (positionOffset == fOffset) {
			boolean isCurrentPosition= isActivePosition(position);
			if (positionLength == 0) {		// The position is an insertion point
				if (isCurrentPosition) {
					// We have an insertion point and the position is one of the currently perform text edit.
					position.length= fDelta;
				} 
				// Keep the insertion point at the current location.
			} else {
				if (isCurrentPosition) {
					// No insertion point but current position. Extend length
					position.length+= fDelta;
				} else if (fLength == 0) {
					// This one is actual an insertion. So more offset to the right
					position.offset+= fDelta;
				} else {
					position.length+= fDelta;	// Overlapping move position found.
				}
			}
		} else if (positionOffset > fEnd) {
			position.offset+= fDelta;
		} else {
			Assert.isTrue(false, "Overlapping text position found: " + position.toString());
		}
	}
	
	private boolean isTextBufferEditorMode() {
		return fActiveTextEdit != null;
	}
	
	private boolean isAffected(TextPosition position, boolean isMoveInsert) {
		return (position.offset >= fOffset) || (isMoveInsert && position.isDisabled());
	}
	
	private boolean isTextInsertion() {
		return fLength == 0;
	}
	
	private boolean isMoveDelete() {
		return fActiveTextEdit instanceof MoveTextEdit && fMoveStep == 0;
	}
	
	private boolean isMoveInsert() {
		return fActiveTextEdit instanceof MoveTextEdit && fMoveStep == 1;
	}
	
	private boolean isMoveSourcePosition(TextPosition position) {
		return ((MoveTextEdit)fActiveTextEdit).getTextPositions()[0] == position;
	}
	
	private void correctValues(TextPosition position) {
		if (!position.isEnabled() || position.isDeleted())
			return;
		if (position.offset < 0)
			position.offset= 0;
		if (position.length < 0)
			position.length= 0;
	}
	private boolean isActivePosition(TextPosition position) {
		return fActiveTextPosition == position;
	}
	
//	private boolean covers(TextPosition position) {
//		if (length == 0) {
//			if (position.length == 0)
//				return offset == position.offset;
//			else
//				return false;
//		} else if (position.length == 0) {
//			int pOffset= position.offset;
//			int exclusiveEnd= offset + length;
//			int inclusiveEnd= exclusiveEnd - 1;
//			if (fAnchor == ANCHOR_RIGHT)
//				return offset <= pOffset && pOffset <= inclusiveEnd;
//			else if (fAnchor == ANCHOR_LEFT)
//				return offset < pOffset && pOffset <= exclusiveEnd;
//			else
//				return offset < pOffset && pOffset <= inclusiveEnd;
//		} else {
//			return offset <= position.offset && position.offset + position.length <= offset + length;
//		}
//	}		
}
