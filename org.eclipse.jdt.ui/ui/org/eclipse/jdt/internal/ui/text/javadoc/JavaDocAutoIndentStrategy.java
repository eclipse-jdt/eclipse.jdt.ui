package org.eclipse.jdt.internal.ui.text.javadoc;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultAutoIndentStrategy;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;

/**
 * Auto indent strategy for java doc comments
 */
public class JavaDocAutoIndentStrategy extends DefaultAutoIndentStrategy {

	public JavaDocAutoIndentStrategy() {
	}

	/**
	 * Returns whether the text ends with one of the given search strings.
	 */
	private boolean endsWithDelimiter(IDocument d, String txt) {
		String[] delimiters= d.getLegalLineDelimiters();
		
		for (int i= 0; i < delimiters.length; i++) {
			if (txt.endsWith(delimiters[i]))
				return true;
		}
		
		return false;
	}

	/**
	 * Copies the indentation of the previous line and add a star
	 * If the javadoc just started on tis line add also a blank 
	 *
	 * @param d the document to work on
	 * @param c the command to deal with
	 */
	private void jdocIndentAfterNewLine(IDocument d, DocumentCommand c) {
		
		if (c.offset == -1 || d.getLength() == 0)
			return;
			
		try {
			// find start of line
			int p= (c.offset == d.getLength() ? c.offset  - 1 : c.offset);
			IRegion info= d.getLineInformationOfOffset(p);
			int start= info.getOffset();
				
			// find white spaces
			int end= findEndOfWhiteSpace(d, start, c.offset);
							
			StringBuffer buf= new StringBuffer(c.text);
			if (end >= start) {	// 1GEYL1R: ITPJUI:ALL - java doc edit smartness not work for class comments
				// append to input
				String indentation= d.get(start, end - start);
				buf.append(indentation);				
				if (end < c.offset) {
					if (d.getChar(end) == '/') {
						// javadoc started on this line
						buf.append(" * ");	 //$NON-NLS-1$

						if (isNewComment(d, c.offset)) {
							String[] lineDelimiters= d.getLegalLineDelimiters();
							c.doit= false;
							d.replace(c.offset, 0, lineDelimiters[0] + indentation + " */"); //$NON-NLS-1$
						}

					} else if (d.getChar(end) == '*') {
						buf.append("* "); //$NON-NLS-1$
					}
				}			
			}
						
			c.text= buf.toString();
				
		} catch (BadLocationException excp) {
			// stop work
		}	
	}	
	
	protected void jdocIndentForCommentEnd(IDocument d, DocumentCommand c) {
		if (c.offset < 2 || d.getLength() == 0) {
			return;
		}
		try {
			if ("* ".equals(d.get(c.offset - 2, 2))) { //$NON-NLS-1$
				// modify document command
				c.length++;
				c.offset--;
			}					
		} catch (BadLocationException excp) {
			// stop work
		}
	}

	private static boolean isNewComment(IDocument document, int commandOffset) {

		try {
			int lineIndex= document.getLineOfOffset(commandOffset) + 1;
			if (lineIndex >= document.getNumberOfLines())
				return true;

			IRegion line= document.getLineInformation(lineIndex);
			ITypedRegion partition= document.getPartition(commandOffset);
			if (document.getLineOffset(lineIndex) >= partition.getOffset() + partition.getLength())
				return true;

			String string= document.get(line.getOffset(), line.getLength());				
			if (!string.trim().startsWith("*")) //$NON-NLS-1$
				return true;
			
			return false;
			
		} catch (BadLocationException e) {
			return false;
		}			
	}

	/*
	 * @see IAutoIndentStrategy#customizeDocumentCommand
	 */
	public void customizeDocumentCommand(IDocument d, DocumentCommand c) {
		if (c.length == 0 && c.text != null && endsWithDelimiter(d, c.text))
			jdocIndentAfterNewLine(d, c);
		else if ("/".equals(c.text)) { //$NON-NLS-1$
			jdocIndentForCommentEnd(d, c);			
		}
	}
}
