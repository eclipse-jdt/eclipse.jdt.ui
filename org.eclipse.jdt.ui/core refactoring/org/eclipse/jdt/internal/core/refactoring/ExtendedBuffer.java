/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IBufferChangedListener;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.refactoring.Assert;

public class ExtendedBuffer {
	private IBuffer fBuffer;
	
	public ExtendedBuffer(IBuffer buffer) {
		fBuffer= buffer;
		Assert.isNotNull(fBuffer);
	}

	public char getChar(int position) {
		return fBuffer.getChar(position);
	}
	public char[] getCharacters() {
		return fBuffer.getCharacters();
	}
	public String getContents() {
		return fBuffer.getContents();
	}
	public int getLength() {
		return fBuffer.getLength();
	}
	public String getText(int offset, int length) {
		return fBuffer.getText(offset, length);
	}
	public boolean isReadOnly() {
		return fBuffer.isReadOnly();
	}
	
	public boolean isLineDelimiter(int position) {
		char c= getChar(position);
		return c == '\n' || c == '\r';
	}
	
	/**
	 * Returns the index of the given character inside the provided buffer
	 * starting at position <code>start</code>. The method overreads comments,
	 * meaning that the character isn't found inside a comment.
	 */
	public int indexOf(char search, int start) {
		return indexOf(search, start, getLength() - start);
	}
	
	public int indexOf(char search, int start, int length) {
		int last= start + length;
		for (int i= start; i < last && i != -1; i++) {
			char c= getChar(i);
			if (c == search)
				return i;
				
			switch (c) {
				case '/':
					int j= i + 1;
					if (j < length) {
						char nextChar= getChar(j);
						if (nextChar == '*') {
							i= getCommentEnd(j + 1);
						} else if (nextChar == '/') {
							i= getLineEnd(j + 1);
						}
					}	
					break;
			}
		}
		return -1;
	}
	
	public int indexOfStatementCharacter(int start) {
		int length= getLength();
		for (int i= start; i < length && i != -1; i++) {
			char c= getChar(i);
			switch (c) {
				case ';':
				case ' ':
				case '\t':
				case '\r':
				case '\n':
					break;
				case '/':
					int j= i + 1;
					if (j < length) {
						char nextChar= getChar(j);
						if (nextChar == '*') {
							i= getCommentEnd(j + 1);
						} else if (nextChar == '/') {
							i= getLineEnd(j + 1);
						}
					}	
					break;
				default:
					return i;
			}
		}
		return -1;
	}
	
	public int indexOfLastCharacterBeforeLineBreak(int start) {
		int length= getLength();
		int i= start;
		loop: for (; i < length; i++) {
			char c= getChar(i);
			switch (c) {
				case '\n':
					break loop;
				case '\r':
					break loop;
			}
		}
		if (i == start)
			return -1;
			
		return i - 1;
	}
	
	private int getCommentEnd(int start) {
		int length= getLength();
		for (int i= start; i < length; i++) {
			char c= getChar(i);
			if (c == '*') {
				int j= i + 1;
				if (j < length && getChar(j) == '/')
					return j;
			} 
		}
		return -1;
	}
	
	private int getLineEnd(int start) {
		int length= getLength();
		for (int i= start; i < length; i++) {
			char c= getChar(i);
			switch (c) {
				case '\n':
					return i;
				case '\r':
					int j= i + 1;
					if (j < length && getChar(j) == '\n')
						return j;
					return i;	
			}
		}
		return length - 1;
	}	
}