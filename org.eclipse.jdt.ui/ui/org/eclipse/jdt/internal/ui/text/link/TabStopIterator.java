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
package org.eclipse.jdt.internal.ui.text.link;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.Position;


/**
 * Iterator that leaps over the double occurrence of an element when switching from forward
 * to backward iteration that is shown by <code>ListIterator</code>.
 */
class TabStopIterator {
	/**
	 * Comparator for <code>LinkedPosition</code>s. If the sequence nr. of two positions is equal, the
	 * offset is used.
	 */
	private static class SequenceComparator implements Comparator {

		/*
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(Object o1, Object o2) {
			LinkedPosition p1= (LinkedPosition)o1;
			LinkedPosition p2= (LinkedPosition)o2;
			int i= p1.getSequenceNumber() - p2.getSequenceNumber();
			if (i != 0)
				return i;
			else
				return p1.getOffset() - p2.getOffset();
		}
		
	}
	
	/** The comparator to sort the list of positions. */
	private static final Comparator fComparator= new SequenceComparator();
	/** The iteration sequence. */
	private final ArrayList fList;
	/** The size of <code>fList</code>. */
	private int fSize;
	/** Index of the current element, to the first one initially. */
	private int fIndex;
	/** Cycling property. */
	private boolean fIsCycling= false;

	public TabStopIterator(List positionSequence) {
		Assert.isNotNull(positionSequence);
		fList= new ArrayList(positionSequence);
		Collections.sort(fList, fComparator);
		fSize= fList.size();
		fIndex= -1;
		Assert.isTrue(fSize > 0);
	}

	public boolean hasNext(LinkedPosition current) {
		return getNextIndex(current) != fSize;				
	}
	
	private int getNextIndex(LinkedPosition current) {
		if (current != null && fList.get(fIndex) != current)
			return findNext(current);
		else if (fIsCycling && fIndex == fSize - 1)
			return 0;
		else
			// default: increase
			return fIndex + 1;
	}
	
	/**
	 * Finds the closest position in the iteration set that follows after
	 * <code>current</code> and sets <code>fIndex</code> accordingly. If <code>current</code>
	 * is in the iteration set, the next in turn is chosen.
	 * 
	 * @param current the current position
	 * @return <code>true</code> if there is a next position, <code>false</code> otherwise
	 */
	private int findNext(LinkedPosition current) {
		Assert.isNotNull(current);
		// if the position is in the iteration set, jump to the next one
		int index= fList.indexOf(current);
		if (index != -1) {
			if (fIsCycling && index == fSize - 1)
				return 0;
			else
				return index + 1;
		} else {
			// find the position that follows closest to the current position
			LinkedPosition found= null;
			for (Iterator it= fList.iterator(); it.hasNext(); ) {
				LinkedPosition p= (LinkedPosition) it.next();
				if (p.offset > current.offset)
					if (found == null || found.offset > p.offset)
						found= p;
			}
			if (found != null) {
				return fList.indexOf(found);
			} else if (fIsCycling) {
				return 0;
			} else
				return fSize;
		}
	}

	public boolean hasPrevious(LinkedPosition current) {
		return getPreviousIndex(current) != -1;				
	}
	
	private int getPreviousIndex(LinkedPosition current) {
		if (current != null && fList.get(fIndex) != current)
			return findPrevious(current);
		else if (fIsCycling && fIndex == 0)
			return fSize - 1;
		else
			return fIndex - 1;
	}

	/**
	 * Finds the closest position in the iteration set that precedes
	 * <code>current</code>. If <code>current</code>
	 * is in the iteration set, the previous in turn is chosen.
	 * 
	 * @param current the current position
	 * @return the index of the previous position
	 */
	private int findPrevious(LinkedPosition current) {
		Assert.isNotNull(current);
		// if the position is in the iteration set, jump to the next one
		int index= fList.indexOf(current);
		if (index != -1) {
			if (fIsCycling && index == 0)
				return fSize - 1;
			else
				return index - 1;
		} else {
			// find the position that follows closest to the current position
			LinkedPosition found= null;
			for (Iterator it= fList.iterator(); it.hasNext(); ) {
				LinkedPosition p= (LinkedPosition) it.next();
				if (p.offset < current.offset)
					if (found == null || found.offset < p.offset)
						found= p;
			}
			if (found != null) {
				return fList.indexOf(found);
			} else if (fIsCycling) {
				return fSize - 1;
			} else
				return -1;
		}
	}

	public LinkedPosition next(LinkedPosition current) {
		if (!hasNext(current))
			throw new NoSuchElementException();
		else 
			return (LinkedPosition) fList.get(fIndex= getNextIndex(current));
	}

	public LinkedPosition previous(LinkedPosition current) {
		if (!hasPrevious(current))
			throw new NoSuchElementException();
		else
			return (LinkedPosition) fList.get(fIndex= getPreviousIndex(current));
	}

	public void setCycling(boolean mode) {
		fIsCycling= mode;
	}

	public void addPosition(Position position) {
		fList.add(fSize++, position);
	}
	
	public void removePosition(Position position) {
		if (fList.remove(position))
			fSize--;
	}

	/**
	 * @return Returns the isCycling.
	 */
	public boolean isCycling() {
		return fIsCycling;
	}
}
