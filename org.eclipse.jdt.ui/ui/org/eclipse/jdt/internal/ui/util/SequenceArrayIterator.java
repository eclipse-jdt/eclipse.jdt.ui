package org.eclipse.jdt.internal.ui.util;


import java.util.Iterator;
import java.util.NoSuchElementException;

public class SequenceArrayIterator implements Iterator {
	
	private Object[] fArray1;
	private Object[] fArray2;
	private int fIndex;
	
	/**
	 * Creates a new enumeration for an arry of objects.
	 */
	public SequenceArrayIterator(Object[] array1, Object[] array2) {
		fArray1= array1;
		fArray2= array2;
		fIndex= 0;
	}
	
	/**
	 * @see Enumeration#hasMoreElements()
	 */
	public boolean hasNext() {
		if (fArray1 == null && fArray2 == null)
			return false;
		if (fArray1 == null)
			return fIndex < fArray2.length;
		if (fArray2 == null)
			return fIndex < fArray1.length;
		return fIndex < fArray1.length + fArray2.length;
	}
	
	/**
	 * @see Enumeration#nextElement()
	 */
	public Object next() {
		try {
			if (fArray1 == null)
				return fArray2[fIndex++];
			if (fIndex < fArray1.length)
				return fArray1[fIndex++];
			return fArray2[fIndex++ - fArray1.length];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new NoSuchElementException();
		} catch (NullPointerException e) {
			throw new NoSuchElementException();
		}
	}
	public void remove() {
		throw new UnsupportedOperationException();
	}
}