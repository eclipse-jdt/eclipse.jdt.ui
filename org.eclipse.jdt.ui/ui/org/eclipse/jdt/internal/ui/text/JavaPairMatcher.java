package org.eclipse.jdt.internal.ui.text;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

/**
 *
 */
public class JavaPairMatcher {

	protected char[] fPairs;
	protected IDocument fDocument;
	protected int fPosition;
	
	protected int fStartPos;
	protected int fEndPos;
	
	
	public JavaPairMatcher(char[] pairs) {
		fPairs= pairs;
	}
	
	public IRegion match(IDocument document, int position) {

		fPosition= position;

		if (fPosition < 0)
			return null;

		fDocument= document;

		if (matchBracketsAt() && fStartPos != fEndPos)
			return new Region(fStartPos, fEndPos - fStartPos + 1);
			
		return null;
	}
	
	protected boolean matchBracketsAt() {

		int i;
		int bracketIndex1= fPairs.length;
		int bracketIndex2= fPairs.length;

		fStartPos= -1;
		fEndPos= -1;

		// get the chars preceding and following the start position
		try {

			char prevChar= fDocument.getChar(fPosition - 1);
			char nextChar= fDocument.getChar(fPosition);

			// search for opening peer character next to the activation point
			for (i= 0; i < fPairs.length; i= i + 2) {
				if (prevChar == fPairs[i]) {
					fStartPos= fPosition - 1;
					bracketIndex1= i;
				} else if (nextChar == fPairs[i]) {
					fStartPos= fPosition;
					bracketIndex1= i;
				}
			}
			
			// search for closing peer character next to the activation point
			for (i= 1; i < fPairs.length; i= i + 2) {
				if (nextChar == fPairs[i]) {
					fEndPos= fPosition;
					bracketIndex2= i;
				} else if (prevChar == fPairs[i]) {
					fEndPos= fPosition - 1;
					bracketIndex2= i;
				}
			}

			if (fStartPos > -1 && bracketIndex1 < bracketIndex2) {
				fEndPos= searchForClosingPeer(fStartPos, prevChar, fPairs[bracketIndex1 + 1], fDocument);
				if (fEndPos > -1)
					return true;
				else
					fStartPos= -1;
			} else if (fEndPos > -1) {
				fStartPos= searchForOpeningPeer(fEndPos, fPairs[bracketIndex2 - 1], nextChar, fDocument);
				if (fStartPos > -1)
					return true;
				else
					fEndPos= -1;
			}

		} catch (BadLocationException x) {
		}

		return false;
	}
	
	protected int searchForClosingPeer(int offset, char openingPeer, char closingPeer, IDocument document) throws BadLocationException {
		int stack= 1;
		int closePos= offset + 1;
		int length= document.getLength();
		char nextChar;

		while (closePos < length && stack > 0) {
			nextChar= document.getChar(closePos);
			if (nextChar == openingPeer && nextChar != closingPeer)
				stack++;
			else if (nextChar == closingPeer)
				stack--;
			closePos++;
		}

		if (stack == 0)
			return closePos - 1;
		else
			return -1;

	}
	
	protected int searchForOpeningPeer(int offset, char openingPeer, char closingPeer, IDocument document) throws BadLocationException {
		int stack= 1;
		int openPos= offset - 1;
		char nextChar;

		while (openPos >= 0 && stack > 0) {
			nextChar= document.getChar(openPos);
			if (nextChar == closingPeer && nextChar != openingPeer)
				stack++;
			else if (nextChar == openingPeer)
				stack--;
			openPos--;
		}

		if (stack == 0)
			return openPos + 1;
		else
			return -1;
	}
}
