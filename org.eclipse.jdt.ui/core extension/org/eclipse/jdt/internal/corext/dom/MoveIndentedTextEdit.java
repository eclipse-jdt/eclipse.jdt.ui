package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jdt.internal.corext.textmanipulation.MoveTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.util.Strings;

/**
  */
public class MoveIndentedTextEdit extends MoveTextEdit {

	private String fDestinationIndent;
	private int fTabWidth;

	/**
	 * Constructor for MoveIndentedTextEdit.
	 * @param offset
	 * @param length
	 * @param destination
	 */
	public MoveIndentedTextEdit(int offset, int length, int destination, String destinationIndent, int tabWidth) {
		super(offset, length, destination);
		
		fDestinationIndent= destinationIndent;
		fTabWidth= tabWidth;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.textmanipulation.MoveTextEdit#getContent(TextBuffer)
	 */
	public String getContent(TextBuffer buffer) {
		TextRange sourceRange= getSourceRange();
		
		int nodeStartLine= buffer.getLineOfOffset(sourceRange.getOffset());
		int nodeIndentLevel= buffer.getLineIndent(nodeStartLine, fTabWidth);
			
		int destIndentLevel= Strings.computeIndent(fDestinationIndent, fTabWidth);
			
		if (nodeIndentLevel != destIndentLevel) {
			String[] lines= buffer.convertIntoLines(sourceRange.getOffset(), sourceRange.getLength(), true);
			StringBuffer buf= new StringBuffer();
			for (int k= 0; k < lines.length; k++) {
				if (k > 0) { // no indent for first line (contained in the formatted string), no new line after last line
					buf.append(buffer.getLineDelimiter());
					buf.append(fDestinationIndent); 
					buf.append(Strings.trimIndent(lines[k], nodeIndentLevel, fTabWidth));
				} else {
					buf.append(lines[k]);
				}
			}
			return buf.toString();
		}
		return super.getContent(buffer); // return unmodified
		
	}
	
}
