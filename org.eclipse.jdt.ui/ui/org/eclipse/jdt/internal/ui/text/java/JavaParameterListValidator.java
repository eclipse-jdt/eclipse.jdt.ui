package org.eclipse.jdt.internal.ui.text.java;



import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;



public class JavaParameterListValidator implements IContextInformationValidator{
	
	private int fPosition;
	private ITextViewer fViewer;
	
	
	
	public JavaParameterListValidator() {
	}
	
	/**
	 * @see IContextInformationValidator#install(IContextInformation, ITextViewer, int)
	 */
	public void install(IContextInformation info, ITextViewer viewer, int documentPosition) {
		fPosition= documentPosition;
		fViewer= viewer;
	}
	
	private int getCommentEnd(IDocument d, int pos, int end) throws BadLocationException {
		while (pos < end) {
			char curr= d.getChar(pos);
			pos++;
			if (curr == '*') {
				if (pos < end && d.getChar(pos) == '/') {
					return pos + 1;
				}
			}
		}
		return end;
	}

	private int getStringEnd(IDocument d, int pos, int end, char ch) throws BadLocationException {
		while (pos < end) {
			char curr= d.getChar(pos);
			pos++;
			if (curr == '\\') {
				// ignore escaped characters
				pos++;
			} else if (curr == ch) {
				return pos;
			}
		}
		return end;
	}
	
	/**
	 * @see IContextInformationValidator#isContextInformationValid(int)
	 */
	public boolean isContextInformationValid(int position) {		
		
		try {
			if (position < fPosition)
				return false;
			
			IDocument document= fViewer.getDocument();
			IRegion line= document.getLineInformationOfOffset(fPosition);
			
			if (position > line.getOffset() + line.getLength())
				return false;
	
			int start= fPosition;
			int end= position;
			
			int bracketcount= 0;
			while (start < end) {
				char curr= document.getChar(start);
				start++;
				switch (curr) {
					case '/' :
						if (start < end) {
							char next= document.getChar(start);
							if (next == '*') {
								// a comment starts, advance to the comment end
								start= getCommentEnd(document, start + 1, end);
							} else if (next == '/') {
								// '//'-comment: nothing to do anymore on this line 
								start= end;
							}
						}
						break;
					case '*' :
						if (start < end) {
							char next= document.getChar(start);
							if (next == '/') {
								// we have been in a comment: forget what we read before
								bracketcount= 0;
								start++;
							}
						}
						break;
					case '(' :
						bracketcount++;
						break;
					case ')' :
						bracketcount--;
						break;
					case '"' :
					case '\'' :
						start= getStringEnd(document, start, end, curr);
						break;
					default :
				}
			}
			
			return (bracketcount >= 0);
			
		} catch (BadLocationException x) {
			return false;
		}
	}	
}

