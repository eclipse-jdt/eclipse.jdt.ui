/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/* package */ class PositionChecker {

	private class OffsetComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			TextPosition pos1= (TextPosition)o1;
			TextPosition pos2= (TextPosition)o2;
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
			boolean p1IsMove= isMove(pos1);
			boolean p2IsMove= isMove(pos2);
			if (p1IsMove && !p2IsMove)
				return -1;
			if (!p1IsMove && p2IsMove)
				return 1;
			return 0;	
		}
	}

	private int fBufferLength;	
	private TextPosition[] fPositions;
	private TextPosition[] fMoves;
	private TextPosition fRootParentMove;
	
	public PositionChecker(List edits, int bufferLength) {
		fBufferLength= bufferLength;
		int length= edits.size();
		List positions= new ArrayList(length);
		List moves= new ArrayList(5);
		fRootParentMove= new TextPosition(0, fBufferLength);
		moves.add(fRootParentMove);
		for (int i= 0; i < length; i++) {
			TextEdit edit= (TextEdit)edits.get(i);
			boolean isMove= edit instanceof MoveTextEdit;
			TextPosition[] tps= edit.getTextPositions();
			for (int j= 0; j < tps.length; j++) {
				TextPosition tp= tps[j];
				positions.add(tp);
				if (isMove && tp.length > 0)
					moves.add(tp);
			}
		}
		fPositions= (TextPosition[]) positions.toArray(new TextPosition[positions.size()]);
		fMoves= (TextPosition[]) moves.toArray(new TextPosition[moves.size()]);
		Arrays.sort(fPositions, new OffsetComparator());
	}
	
	public boolean perform() {
		int index= checkEnclosedPositions(fRootParentMove, 0);
		return index != -1;
	}
	
	private int checkEnclosedPositions(TextPosition parent, int index) {
		if (index >= fPositions.length)
			return index;
		boolean isMove= isMove(parent);
		for (int i= index; i < fPositions.length; ) {
			TextPosition current= fPositions[i];
			if (parent.isInsertionPoint()) {
				if (parent.isEqualInsertionPoint(current)) {
					i++;
					continue;
				} else {
					return i;		// since the position are sorted any other position lies behind the parent
				}
			} else {
				if (current.liesBehind(parent)) {
					return i;
				} else if (current.isInsertionPointAt(parent.offset)) {
					i++;
					continue;
				} else if (covers(parent, current)) {
					if (isMove) {
						i= checkEnclosedPositions(current, i + 1);
						if (i == -1)
							return -1;
						continue;
					} else {
						return -1;
					}
				} else {
					return -1;
				}
			}
		}
		return fPositions.length;
	}
	
	private static boolean covers(TextPosition p1, TextPosition other) {
		if (p1.length == 0) {
			if (other.length == 0)
				return p1.offset == other.offset;
			else
				return false;
		} else if (other.length == 0) {
			int offset= p1.offset;
			int otherOffset= other.offset;
			return offset < otherOffset && otherOffset < offset + p1.length;
		} else {
			int offset= p1.offset;
			int otherOffset= other.offset;
			return offset <= otherOffset && otherOffset + other.length <= offset + p1.length;
		}
	}
	private boolean isMove(TextPosition position) {
		for (int i= 0; i < fMoves.length; i++) {
			if (position == fMoves[i])
				return true;
		}
		return false;
	}	
}

