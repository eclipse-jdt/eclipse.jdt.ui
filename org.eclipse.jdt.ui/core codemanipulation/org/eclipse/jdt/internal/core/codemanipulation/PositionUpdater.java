/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.codemanipulation;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.Position;

import org.eclipse.core.internal.runtime.Assert;

/* package */ final class PositionUpdater implements IPositionUpdater {

	private static class OffsetComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			Position pos1= (Position)o1;
			Position pos2= (Position)o2;
			int p1= pos1.offset;
			int p2= pos2.offset;
			if (p1 < p2)
				return -1;
			if (p1 > p2)
				return 1;
			// same offset
			int l1= pos1.length;
			int l2= pos2.length;
			if (l1 <  l2)
				return 1;
			if (l1 > l2)
				return -1;
			return 0;	
		}
	}

	private DefaultPositionUpdater fDefaultUpdater;
	private int fOffset, fLength, fEnd, fNewLength, fDelta;
	private Position fPosition;
	private TextPosition[] fCurrentPositions;
	
	public static final String CATEGORY= PositionUpdater.class.getClass().getName();
	
	public PositionUpdater() {
		fDefaultUpdater= new DefaultPositionUpdater(CATEGORY);
	}
	
	public void update(DocumentEvent event) {
		if (!isTextBufferEditorMode())
			fDefaultUpdater.update(event);
			
		try {
			Position[] positions= event.getDocument().getPositions(CATEGORY);
			fOffset= event.getOffset();
			fLength= event.getLength();
			fEnd= fOffset + fLength - 1;
			fNewLength= (event.getText() == null ? 0 : event.getText().length());
			fDelta= fNewLength - fLength;
			for (int i= 0; i < positions.length; i++) {
				fPosition= positions[i];
				if (!isAffected())
					continue;
				processPosition();
				correctValues();
			}
			
		} catch (BadPositionCategoryException e) {
		}
	}
	
	private boolean isTextBufferEditorMode() {
		return fCurrentPositions != null;
	}
	
	private void processPosition() {
		int positionOffset= fPosition.offset;
		int positionLength= fPosition.length;
		int positionEnd= positionOffset + positionLength - 1;
		
		if (positionOffset == fOffset) {
			boolean isCurrentPosition= isCurrentPosition(fPosition);
			if (positionLength == 0) {		// The position is an insertion point
				if (isCurrentPosition) {
					// We have an insertion point and the position is one of the currently perform text edit.
					fPosition.length= fDelta;
				} 
				// Keep the insertion point at the current location.
			} else {
				if (isCurrentPosition) {
					// No insertion point but current position. Extend length
					fPosition.length+= fDelta;
				} else {
					// No insertion point and not current position. Shift the offset to the right.
					fPosition.offset+= fDelta;
				}
			}
		} else if (positionOffset > fEnd) {
			fPosition.offset+= fDelta;
		} else if (positionOffset < fOffset && (isTextInsertion() ? fOffset <= positionEnd : fEnd <= positionEnd)) {
			fPosition.length+= fDelta;
		} else {
			Assert.isTrue(false, "Should never happen");
		}
	}
	
	private boolean isAffected() {
		return fPosition.offset >= fOffset;
	}
	
	private boolean isTextInsertion() {
		return fLength == 0;
	}
	
	private void correctValues() {
		if (fPosition.offset < 0)
			fPosition.offset= 0;
		if (fPosition.length < 0)
			fPosition.length= 0;
	}

	public boolean canUpdate(Position[] positions, int bufferLength) {	
		Arrays.sort(positions, new OffsetComparator());
		outer: for (int i= 0; i < positions.length;) {
			Position position= positions[i];
			int offset= position.offset;
			int length= position.length;
			int end= offset + length - 1;
			if (end >= bufferLength)
				return false;
			boolean isInsertion= length == 0;
			int x= i + 1;
			for (; x < positions.length; x++) {
				Position t= positions[x];
				int tOffset= t.offset;
				int tLength= t.length;
				int tEnd= tOffset + tLength - 1;
				if (offset == tOffset && tLength == 0) // Same insertion points are ok.
					continue;
				if ((offset == tOffset && tEnd <= end) || (offset < tOffset && tOffset <= end))
					return false;
				if (tOffset > end)
					break;
			}
			i= x;
		}
		return true;
	}

	public void setCurrentPositions(TextPosition[] currentPositions) {
		fCurrentPositions= currentPositions;
	}
	
	private boolean isCurrentPosition(Position position) {
		if (fCurrentPositions == null)
			return false;
		for (int i= 0; i < fCurrentPositions.length; i++) {
			if (fCurrentPositions[i] == position)
				return true;
		}
		return false;
	}	
}
