/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.jdt.core.ISourceRange;

public class SourceRange implements ISourceRange{
	
	private int fOffset;
	private int fLength;

	public SourceRange(int offset, int length){
		Assert.isTrue(offset >= 0);
		Assert.isTrue(length >= 0);
		fLength= length;
		fOffset= offset;
	}
	
	/*
	 * @see ISourceRange#getLength()
	 */
	public int getLength() {
		return fLength;
	}

	/*
	 * @see ISourceRange#getOffset()
	 */
	public int getOffset() {
		return fOffset;
	}
	
	/*non java doc
	 * for debugging only
	 */
	public String toString(){
		return "<offset: " + fOffset +" length: " + fLength + "/>";
	}
	
	/**
	 * Sorts the given ranges by offset (backwards).
	 * Note: modifies the parameter.
	 */
	public static ISourceRange[] reverseSortByOffset(ISourceRange[] ranges){
		Comparator comparator= new Comparator(){
			public int compare(Object o1, Object o2){
				return ((ISourceRange)o2).getOffset() - ((ISourceRange)o1).getOffset();
			}
		};
		Arrays.sort(ranges, comparator);
		return ranges;
	}
}

