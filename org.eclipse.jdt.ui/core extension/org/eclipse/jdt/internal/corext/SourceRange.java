/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;

public class SourceRange implements ISourceRange{
	
	private final int fOffset;
	private final int fLength;

	public SourceRange(int offset, int length){
		fLength= length;
		fOffset= offset;
	}
	
	public SourceRange(ASTNode node) {
		this(node.getStartPosition(), node.getLength());
	}

	public SourceRange(IProblem problem) {
		this(problem.getSourceStart(), problem.getSourceEnd() - problem.getSourceStart() + 1);
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
	
	public int getEndExclusive() {
		return getOffset() + getLength();
	}
	
	public int getEndInclusive() {
		return getEndExclusive() - 1;	
	}
	
	/*non java doc
	 * for debugging only
	 */
	public String toString(){
		return "<offset: " + fOffset +" length: " + fLength + "/>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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

    /*
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
    	if (! (obj instanceof ISourceRange))
	        return false;
	    return ((ISourceRange)obj).getOffset() == fOffset && ((ISourceRange)obj).getLength() == fLength;
    }

    /*
     * @see Object#hashCode()
     */
    public int hashCode() {
        return fLength ^ fOffset;
    }
    
    public boolean covers(ASTNode node) {
    	return covers(new SourceRange(node));
    }
    
    public boolean covers(SourceRange range) {
    	return    getOffset() <= range.getOffset()
    	       	&& getEndInclusive() >= range.getEndInclusive();
    }
}

