package org.eclipse.jdt.internal.ui.text.java;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultAutoIndentStrategy;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.JavaPartitionScanner;

/**
 * Auto indent strategy sensitive to brackets.
 */
public class JavaAutoIndentStrategy extends DefaultAutoIndentStrategy {

	public JavaAutoIndentStrategy() {
	}

	// evaluate the line with the opening bracket that matches the closing bracket on the given line
	protected int findMatchingOpenBracket(IDocument d, int line, int end, int closingBracketIncrease) throws BadLocationException {

		int start= d.getLineOffset(line);
		int brackcount= getBracketCount(d, start, end, false) - closingBracketIncrease;

		// sum up the brackets counts of each line (closing brackets count negative,
		// opening positive) until we find a line the brings the count to zero
		while (brackcount < 0) {
			line--;
			if (line < 0) {
				return -1;
			}
			start= d.getLineOffset(line);
			end= start + d.getLineLength(line) - 1;
			brackcount += getBracketCount(d, start, end, false);
		}
		return line;
	}

	private int getBracketCount(IDocument d, int start, int end, boolean ignoreCloseBrackets) throws BadLocationException {

		int bracketcount= 0;
		while (start < end) {
			char curr= d.getChar(start);
			start++;
			switch (curr) {
				case '/' :
					if (start < end) {
						char next= d.getChar(start);
						if (next == '*') {
							// a comment starts, advance to the comment end
							start= getCommentEnd(d, start + 1, end);
						} else if (next == '/') {
							// '//'-comment: nothing to do anymore on this line
							start= end;
						}
					}
					break;
				case '*' :
					if (start < end) {
						char next= d.getChar(start);
						if (next == '/') {
							// we have been in a comment: forget what we read before
							bracketcount= 0;
							start++;
						}
					}
					break;
				case '{' :
					bracketcount++;
					ignoreCloseBrackets= false;
					break;
				case '}' :
					if (!ignoreCloseBrackets) {
						bracketcount--;
					}
					break;
				case '"' :
				case '\'' :
					start= getStringEnd(d, start, end, curr);
					break;
				default :
					}
		}
		return bracketcount;
	}

	// ----------- bracket counting ------------------------------------------------------

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

	protected String getIndentOfLine(IDocument d, int line) throws BadLocationException {
		if (line > -1) {
			int start= d.getLineOffset(line);
			int end= start + d.getLineLength(line) - 1;
			int whiteend= findEndOfWhiteSpace(d, start, end);
			return d.get(start, whiteend - start);
		} else {
			return ""; //$NON-NLS-1$
		}
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

	protected void smartInsertAfterBracket(IDocument d, DocumentCommand c) {
		if (c.offset == -1 || d.getLength() == 0)
			return;

		try {
			int p= (c.offset == d.getLength() ? c.offset - 1 : c.offset);
			int line= d.getLineOfOffset(p);
			int start= d.getLineOffset(line);
			int whiteend= findEndOfWhiteSpace(d, start, c.offset);

			// shift only when line does not contain any text up to the closing bracket
			if (whiteend == c.offset) {
				// evaluate the line with the opening bracket that matches out closing bracket
				int indLine= findMatchingOpenBracket(d, line, c.offset, 1);
				if (indLine != -1 && indLine != line) {
					// take the indent of the found line
					StringBuffer replaceText= new StringBuffer(getIndentOfLine(d, indLine));
					// add the rest of the current line including the just added close bracket
					replaceText.append(d.get(whiteend, c.offset - whiteend));
					replaceText.append(c.text);
					// modify document command
					c.length += c.offset - start;
					c.offset= start;
					c.text= replaceText.toString();
				}
			}
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
	}

	protected void smartIndentAfterNewLine(IDocument d, DocumentCommand c) {

		int docLength= d.getLength();
		if (c.offset == -1 || docLength == 0)
			return;

		try {
			int p= (c.offset == docLength ? c.offset - 1 : c.offset);
			int line= d.getLineOfOffset(p);

			StringBuffer buf= new StringBuffer(c.text);
			if (c.offset < docLength && d.getChar(c.offset) == '}') {
				int indLine= findMatchingOpenBracket(d, line, c.offset, 0);
				if (indLine == -1) {
					indLine= line;
				}
				buf.append(getIndentOfLine(d, indLine));
			} else {
				int start= d.getLineOffset(line);
				// if line just ended a javadoc comment, take the indent from the comment's begin line
				IDocumentPartitioner partitioner= d.getDocumentPartitioner();
				if (partitioner != null) {
					ITypedRegion region= partitioner.getPartition(start);
					if (JavaPartitionScanner.JAVA_DOC.equals(region.getType()))
						start= d.getLineInformationOfOffset(region.getOffset()).getOffset();
				}
				int whiteend= findEndOfWhiteSpace(d, start, c.offset);
				buf.append(d.get(start, whiteend - start));
				if (getBracketCount(d, start, c.offset, true) > 0) {
					buf.append(getOneIndentLevel());
				}
			}
			c.text= buf.toString();

		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
	}

	private static String getLineDelimiter(IDocument document) {
		try {
			if (document.getNumberOfLines() > 1)
				return document.getLineDelimiter(0);
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}

		return System.getProperty("line.separator"); //$NON-NLS-1$
	}

	protected void smartPaste(IDocument document, DocumentCommand command) {

		String lineDelimiter= getLineDelimiter(document);

		try {
			String pastedText= command.text;
			Assert.isNotNull(pastedText);
			Assert.isTrue(pastedText.length() > 1);
			
			// #27512
//			int selectionEnd= command.offset + command.length;
//			IRegion region= document.getLineInformationOfOffset(selectionEnd);
//			String selected= document.get(region.getOffset(), selectionEnd - region.getOffset());
//			String notSelected= document.get(selectionEnd, region.getOffset() + region.getLength() - selectionEnd);
//			if (selected.trim().length() == 0 && notSelected.trim().length() != 0) {
//				pastedText += notSelected;
//				command.length += notSelected.length();
//			}
			
			String strippedParagraph= stripIndent(pastedText, lineDelimiter);

			// check selection
			int offset= command.offset;
			int line= document.getLineOfOffset(offset);
			int lineOffset= document.getLineOffset(line);

			// format
			String prefix= document.get(lineOffset, offset - lineOffset);

			String blockIndent= getBlockIndent(document, command);
			String insideBlockIndent= blockIndent == null ? "" : blockIndent + createIndent(1); //$NON-NLS-1$ // add one indent level
			String previousIndent= getIndent(document, command);
			String indent= calculateDisplayedWidth(insideBlockIndent) < calculateDisplayedWidth(previousIndent)
				? insideBlockIndent
				: previousIndent;

			boolean formatFirstLine= prefix.trim().length() == 0;
			String formattedParagraph= format(strippedParagraph, indent, lineDelimiter, formatFirstLine);

			// paste
			if (formatFirstLine) {
				int end= command.offset + command.length;
				command.offset= lineOffset;
				command.length= end - command.offset;
			}
			command.text= formattedParagraph;

		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
	}

	/**
	 * Returns the displayed width of a string, taking in account the displayed tab width.
	 * The result can be compared against the print margin.
	 */
	private static int calculateDisplayedWidth(String string) {

		final int tabWidth= JavaPlugin.getDefault().getPreferenceStore().getInt(PreferenceConstants.EDITOR_TAB_WIDTH);

		int column= 0;
		for (int i= 0; i < string.length(); i++)
			if ('\t' == string.charAt(i))
				column += tabWidth - (column % tabWidth);
			else
				column++;

		return column;
	}

	private static boolean isLineEmpty(IDocument document, int line) throws BadLocationException {
		IRegion region= document.getLineInformation(line);
		String string= document.get(region.getOffset(), region.getLength());
		return string.trim().length() == 0;
	}

	private String getIndent(IDocument document, DocumentCommand command) {

		StringBuffer buf= new StringBuffer();

		int docLength= document.getLength();
		if (command.offset == -1 || docLength == 0)
			return buf.toString();

		try {
			
			int p= (command.offset == docLength ? command.offset - 1 : command.offset);
			int line= document.getLineOfOffset(p);

			IRegion region= document.getLineInformation(line);
			String string= document.get(region.getOffset(), command.offset - region.getOffset());
			if (line != 0 && string.trim().length() == 0)
				--line;
			
			while (line != 0 && isLineEmpty(document, line))
				--line;

			int start= document.getLineOffset(line);
			
			// if line is at end of a javadoc comment, take the indent from the comment's begin line
			IDocumentPartitioner partitioner= document.getDocumentPartitioner();
			if (partitioner != null) {
				ITypedRegion typedRegion= partitioner.getPartition(start);
				if (JavaPartitionScanner.JAVA_DOC.equals(typedRegion.getType()))
					start= document.getLineInformationOfOffset(typedRegion.getOffset()).getOffset();
			}
			int whiteend= findEndOfWhiteSpace(document, start, command.offset);
			buf.append(document.get(start, whiteend - start));
			if (getBracketCount(document, start, command.offset, true) > 0) {
				buf.append(getOneIndentLevel());
			}

		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
		
		return buf.toString();
	}
	
	private String getBlockIndent(IDocument d, DocumentCommand c) {
		if (c.offset < 0 || d.getLength() == 0)
			return null;

		try {
			int p= (c.offset == d.getLength() ? c.offset - 1 : c.offset);
			int line= d.getLineOfOffset(p);

			// evaluate the line with the opening bracket that matches out closing bracket
			int indLine= findMatchingOpenBracket(d, line, c.offset, 1);
			if (indLine != -1 && indLine != line)
				// take the indent of the found line
				return getIndentOfLine(d, indLine);

		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
		return null;
	}

	private static final class LineIterator implements Iterator {
		/** The document to iterator over. */
		private final IDocument fDocument;
		/** The line index. */
		private int fLineIndex;

		/**
		 * Creates a line iterator.
		 */
		public LineIterator(String string) {
			fDocument= new Document(string);
		}

		/*
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext() {
			return fLineIndex != fDocument.getNumberOfLines();
		}

		/*
		 * @see java.util.Iterator#next()
		 */
		public Object next() {
			try {
				IRegion region= fDocument.getLineInformation(fLineIndex++);
				return fDocument.get(region.getOffset(), region.getLength());
			} catch (BadLocationException e) {
				JavaPlugin.log(e);
				throw new NoSuchElementException();
			}
		}

		/*
		 * @see java.util.Iterator#remove()
		 */
		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	private static IPreferenceStore getPreferenceStore() {
		return JavaPlugin.getDefault().getPreferenceStore();
	}

	private static String createIndent(int level) {

		StringBuffer buffer= new StringBuffer();

		if (useSpaces()) {
			int tabWidth= getPreferenceStore().getInt(PreferenceConstants.EDITOR_TAB_WIDTH);
			int width= level * tabWidth;
			for (int i= 0; i != width; ++i)
				buffer.append(' ');

		} else {
			buffer.append('\t');
		}

		return buffer.toString();
	}

	private static String createPrefix(int displayedWidth) {

		StringBuffer buffer= new StringBuffer();

		if (useSpaces()) {
			for (int i= 0; i != displayedWidth; ++i)
				buffer.append(' ');

		} else {
			int tabWidth= getPreferenceStore().getInt(PreferenceConstants.EDITOR_TAB_WIDTH);
			int div= displayedWidth / tabWidth;
			int mod= displayedWidth % tabWidth;

			for (int i= 0; i != div; ++i)
				buffer.append('\t');

			for (int i= 0; i != mod; ++i)
				buffer.append(' ');

		}

		return buffer.toString();
	}

	private String stripIndent(String paragraph, String lineDelimiter) {

		final StringBuffer buffer= new StringBuffer();

		// determine minimum indent width
		int minIndentWidth= Integer.MAX_VALUE;
		for (final Iterator iterator= new LineIterator(paragraph); iterator.hasNext();) {
			String line= (String) iterator.next();
			String trimmedLine= line.trim();

			if (trimmedLine.length() == 0)
				continue;

			int index= line.indexOf(trimmedLine);
			String indent= line.substring(0, index);
			int width= calculateDisplayedWidth(indent);
			minIndentWidth= Math.min(minIndentWidth, width);
		}

		// strip prefixes
		for (final Iterator iterator= new LineIterator(paragraph); iterator.hasNext();) {
			String line= (String) iterator.next();
			String trimmedLine= line.trim();

			if (trimmedLine.length() != 0) {
				int index= line.indexOf(trimmedLine);
				String indent= line.substring(0, index);
				int width= calculateDisplayedWidth(indent);
				int strippedWidth= width - minIndentWidth;
				String prefix= createPrefix(strippedWidth);

				buffer.append(prefix);
				buffer.append(trimmedLine);
			}

			if (iterator.hasNext())
				buffer.append(lineDelimiter);
		}

		return buffer.toString();
	}

	private String format(String paragraph, String indent, String lineDelimiter, boolean indentFirstLine) {

		final StringBuffer buffer= new StringBuffer();

		for (final Iterator iterator= new LineIterator(paragraph); iterator.hasNext();) {
			String line= (String) iterator.next();
			if (indentFirstLine && line.trim().length() != 0)
				buffer.append(indent);
			else
				indentFirstLine= true;
			buffer.append(line);
			if (iterator.hasNext())
				buffer.append(lineDelimiter);
		}

		return buffer.toString();
	}

	private String getOneIndentLevel() {
		return String.valueOf('\t');
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

	private boolean equalsDelimiter(IDocument d, String txt) {

		String[] delimiters= d.getLegalLineDelimiters();

		for (int i= 0; i < delimiters.length; i++) {
			if (txt.equals(delimiters[i]))
				return true;
		}

		return false;
	}


	private void smartIndentAfterBlockDelimiter(IDocument document, DocumentCommand command) {
		if (command.text.charAt(0) == '}')
			smartInsertAfterBracket(document, command);
	}

	/*
	 * @see org.eclipse.jface.text.IAutoIndentStrategy#customizeDocumentCommand(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.DocumentCommand)
	 */
	public void customizeDocumentCommand(IDocument d, DocumentCommand c) {
		if (c.length == 0 && c.text != null && equalsDelimiter(d, c.text))
			smartIndentAfterNewLine(d, c);
		else if (c.text.length() == 1)
			smartIndentAfterBlockDelimiter(d, c);
		else if (c.text.length() > 1 && getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SMART_PASTE))
			smartPaste(d, c);
	}

	private static boolean useSpaces() {
		return JavaCore.SPACE.equals(JavaCore.getOptions().get(JavaCore.FORMATTER_TAB_CHAR));
	}

}
