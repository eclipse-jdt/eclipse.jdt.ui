/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring;

import org.eclipse.jdt.core.ISourceRange;

public class SourceRange implements ISourceRange{
	
	private int fOffset;
	private int fLength;

	public SourceRange(int offset, int length){
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
}

