/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.refactoring.changes;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.GapTextStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextStore;
import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.core.refactoring.text.ITextBuffer;
import org.eclipse.jdt.core.refactoring.text.ITextRegion;

/**
 * An implementation of ITextBuffer</code> that is based on <code>ITextSelection</code>
 * and <code>IDocument</code>.
 */
public class TextBuffer implements ITextBuffer {

	private IDocument fDocument;
	
	private static class DocumentRegion implements ITextRegion {
		IRegion fRegion;
		public DocumentRegion(IRegion region) {
			fRegion= region;
		}
		public int getOffset() {
			return fRegion.getOffset();
		}
		public int getLength() {
			return fRegion.getLength();
		}
	}

	public TextBuffer(IDocument document) {
		fDocument= document;
		Assert.isNotNull(fDocument);
	}
	
	/* Non-Javadoc
	 * Method declared in ITextBuffer
	 */
	public int getLength() {
		return fDocument.getLength();
	}
	
	/* Non-Javadoc
	 * Method declared in ITextBuffer
	 */
	public char getChar(int offset) {
		try {
			return fDocument.getChar(offset);
		} catch (BadLocationException e) {
			throw new ArrayIndexOutOfBoundsException(e.getMessage());
		}
	}
	
	/* Non-Javadoc
	 * Method declared in ITextBuffer
	 */
	public String getContent() {
		return fDocument.get();
	}
	
	/* Non-Javadoc
	 * Method declared in ITextBuffer
	 */
	public String getContent(int start, int length) {
		try {
			return fDocument.get(start, length);
		} catch (BadLocationException e) {
			return null;
		}
	}
	
	/* Non-Javadoc
	 * Method declared in ITextBuffer
	 */
	public String getLineDelimiter(int line) {
		try {
			return fDocument.getLineDelimiter(line);
		} catch (BadLocationException e) {
			return null;
		}	
	}
	
	/* Non-Javadoc
	 * Method declared in ITextBuffer
	 */
	public String getLineContent(int line) {
		try {
			IRegion region= fDocument.getLineInformation(line);
			return fDocument.get(region.getOffset(), region.getLength());
		} catch (BadLocationException e) {
			return null;
		}
	}
	
	/* Non-Javadoc
	 * Method declared in ITextBuffer
	 */
	public ITextRegion getLineInformation(int line) {
		try {
			return new DocumentRegion(fDocument.getLineInformation(line));
		} catch (BadLocationException e) {
			return null;
		}	
	}
	
	/* Non-Javadoc
	 * Method declared in ITextBuffer
	 */
	public ITextRegion getLineInformationOfOffset(int offset) {
		try {
			return new DocumentRegion(fDocument.getLineInformationOfOffset(offset));
		} catch (BadLocationException e) {
			return null;
		}	
	}
	
	/* Non-Javadoc
	 * Method declared in ITextBuffer
	 */
	public int getLineOfOffset(int offset) {
		try {
			return fDocument.getLineOfOffset(offset);
		} catch (BadLocationException e) {
			return -1;
		}
	}

	/* Non-Javadoc
	 * Method declared in ITextBuffer
	 */
	public String getLineContentOfOffset(int offset) {
		try {
			IRegion region= fDocument.getLineInformationOfOffset(offset);
			return fDocument.get(region.getOffset(), region.getLength());
		} catch (BadLocationException e) {
			return null;
		}
	}

	/* Non-Javadoc
	 * Method declared in ITextBuffer
	 */
	public String[] convertIntoLines(int offset, int length) {
		try {
			String text= fDocument.get(offset, length);
			ITextStore store= new GapTextStore(0, 1);
			store.set(text);
			ILineTracker tracker= new DefaultLineTracker();
			tracker.set(text);
			int size= tracker.getNumberOfLines();
			String result[]= new String[size];
			for (int i= 0; i < size; i++) {
				IRegion region= tracker.getLineInformation(i);
				result[i]= store.get(region.getOffset(), region.getLength());
			}
			return result;
		} catch (BadLocationException e) {
			return null;
		}
	}
	
	/* Non-Javadoc
	 * Method declared in ITextBuffer
	 */
	public void replace(int offset, int length, String text) {
		try {
			fDocument.replace(offset, length, text);
		} catch (BadLocationException e) {
			throw new IndexOutOfBoundsException(e.getMessage());
		}	
	}
}