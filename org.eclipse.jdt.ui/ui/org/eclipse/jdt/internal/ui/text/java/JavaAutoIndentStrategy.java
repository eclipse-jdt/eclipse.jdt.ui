/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/

package org.eclipse.jdt.internal.ui.text.java;


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

	private final static String COMMENT= "//"; //$NON-NLS-1$
	private int fTabWidth= -1;
	private boolean fUseSpaces;

	public JavaAutoIndentStrategy() {
        fUseSpaces= getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SPACES_FOR_TABS);
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
					buf.append(createIndent(1, useSpaces()));
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


	private static boolean startsWithClosingBrace(String string) {
		final int length= string.length();
		int i= 0;
		while (i != length && Character.isWhitespace(string.charAt(i)))
			++i;
		if (i == length)
			return false;
		return string.charAt(i) == '}';
	}

	protected void smartPaste(IDocument document, DocumentCommand command) {

		String lineDelimiter= getLineDelimiter(document);

		try {
			String pastedText= command.text;
			Assert.isNotNull(pastedText);
			Assert.isTrue(pastedText.length() > 1);
			
			// extend selection begin if only whitespaces
			int selectionStart= command.offset;
			IRegion region= document.getLineInformationOfOffset(selectionStart);
			String notSelected= document.get(region.getOffset(), selectionStart - region.getOffset());
			String selected= document.get(selectionStart, region.getOffset() + region.getLength() - selectionStart);
			if (notSelected.trim().length() == 0 && selected.trim().length() != 0) {
				pastedText= notSelected + pastedText;
				command.length += notSelected.length();
				command.offset= region.getOffset();
			}
			
			// choose smaller indent of block and preceeding non-empty line 
			String blockIndent= getBlockIndent(document, command);
			String insideBlockIndent= blockIndent == null ? "" : blockIndent + createIndent(1, useSpaces()); //$NON-NLS-1$ // add one indent level
			int insideBlockIndentSize= calculateDisplayedWidth(insideBlockIndent, getTabWidth());
			int previousIndentSize= getIndentSize(document, command);
			int newIndentSize= insideBlockIndentSize < previousIndentSize
				? insideBlockIndentSize
				: previousIndentSize;

			// indent is different if block starts with '}'				
			if (startsWithClosingBrace(pastedText)) {
				int outsideBlockIndentSize= blockIndent == null ? 0 : calculateDisplayedWidth(blockIndent, getTabWidth());
				newIndentSize = outsideBlockIndentSize;				
			}

			// check selection
			int offset= command.offset;
			int line= document.getLineOfOffset(offset);
			int lineOffset= document.getLineOffset(line);
			String prefix= document.get(lineOffset, offset - lineOffset);

			boolean formatFirstLine= prefix.trim().length() == 0;

			String formattedParagraph= format(pastedText, newIndentSize, lineDelimiter, formatFirstLine);

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

	private static String getIndentOfLine(String line) {
		int i= 0;
		for (; i < line.length(); i++) {
			if (! Character.isWhitespace(line.charAt(i)))
				break;
		}
		return line.substring(0, i);
	}

	/**
	 * Returns the indent of the first non empty line.
	 * A line is considered empty if it only consists of whitespaces or if it
	 * begins with a single line comment followed by whitespaces only.
	 */
	private static int getIndentSizeOfFirstLine(String paragraph, boolean includeFirstLine, int tabWidth) {
		for (final Iterator iterator= new LineIterator(paragraph); iterator.hasNext();) {
			final String line= (String) iterator.next();
			
			if (!includeFirstLine) {
				includeFirstLine= true;
				continue;
			}			

			String indent= null;
			if (line.startsWith(COMMENT)) {
				String commentedLine= line.substring(2);
				
				// line is empty
				if (commentedLine.trim().length() == 0)
					continue;

				indent= COMMENT + getIndentOfLine(commentedLine);
				 
			} else {
				// line is empty
				if (line.trim().length() == 0)
					continue;

				indent= getIndentOfLine(line);
			}
			
			return calculateDisplayedWidth(indent, tabWidth);
		}

		return 0;		
	}
	
	/**
	 * Returns the minimal indent size of all non empty lines;
	 */
	private static int getMinimalIndentSize(String paragraph, boolean includeFirstLine, int tabWidth) {
		int minIndentSize= Integer.MAX_VALUE;
		
		for (final Iterator iterator= new LineIterator(paragraph); iterator.hasNext();) {
			final String line= (String) iterator.next();

			if (!includeFirstLine) {
				includeFirstLine= true;
				continue;
			}

			String indent= null;
			if (line.startsWith(COMMENT)) {
				String commentedLine= line.substring(2);
				
				// line is empty
				if (commentedLine.trim().length() == 0)
					continue;
				
				indent= COMMENT + getIndentOfLine(commentedLine);				
			} else {
				// line is empty
				if (line.trim().length() == 0)
					continue;

				indent=getIndentOfLine(line);
			}

			final int indentSize= calculateDisplayedWidth(indent, tabWidth);
			if (indentSize < minIndentSize)
				minIndentSize= indentSize;
		}

		return minIndentSize == Integer.MAX_VALUE ? 0 : minIndentSize;
	}

	/**
	 * Returns the displayed width of a string, taking in account the displayed tab width.
	 * The result can be compared against the print margin.
	 */
	private static int calculateDisplayedWidth(String string, int tabWidth) {

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

	private int getIndentSize(IDocument document, DocumentCommand command) {

		StringBuffer buffer= new StringBuffer();

		int docLength= document.getLength();
		if (command.offset == -1 || docLength == 0)
			return 0;

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

				else if (JavaPartitionScanner.JAVA_SINGLE_LINE_COMMENT.equals(typedRegion.getType())) {
					buffer.append(COMMENT);
					start += 2;
				}
			}
			int whiteend= findEndOfWhiteSpace(document, start, command.offset);
			buffer.append(document.get(start, whiteend - start));
			if (getBracketCount(document, start, command.offset, true) > 0) {
				buffer.append(createIndent(1, useSpaces()));
			}

		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
		
		return calculateDisplayedWidth(buffer.toString(), getTabWidth());
	}
	
	private String getBlockIndent(IDocument d, DocumentCommand c) {
		if (c.offset < 0 || d.getLength() == 0)
			return null;

		try {
			int p= (c.offset == d.getLength() ? c.offset - 1 : c.offset);
			int line= d.getLineOfOffset(p);

			// evaluate the line with the opening bracket that matches out closing bracket
			int indLine= findMatchingOpenBracket(d, line, c.offset, 1);
			if (indLine != -1)
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

	private String createIndent(int level, boolean useSpaces) {

		StringBuffer buffer= new StringBuffer();

		if (useSpaces) {
            // Fix for bug 29909 contributed by Nikolay Metchev
			int width= level * getTabWidth();
			for (int i= 0; i != width; ++i)
				buffer.append(' ');

		} else {
			for (int i= 0; i != level; ++i)
				buffer.append('\t');
		}

		return buffer.toString();
	}

	/**
	 * Extends the string to match displayed width.
	 * String is either the empty string or "//" and should not contain whites.
	 */
	private static String changePrefix(String string, int displayedWidth, boolean useSpaces, int tabWidth) {

		// assumption: string contains no whitspaces
		final StringBuffer buffer= new StringBuffer(string);
		int column= calculateDisplayedWidth(buffer.toString(), tabWidth);

		if (column > displayedWidth)
			return string;
		
		if (useSpaces) {
			while (column != displayedWidth) {
				buffer.append(' ');
				++column;
			}
			
		} else {
			
			while (column != displayedWidth) {
				if (column + tabWidth - (column % tabWidth) <= displayedWidth) {
					buffer.append('\t');
					column += tabWidth - (column % tabWidth);
				} else {
					buffer.append(' ');
					++column;
				}
			}			
		}

		return buffer.toString();
	}

	/**
	 * Formats a paragraph such that the first non-empty line of the paragraph
	 * will have an indent of size newIndentSize.
	 */
	private String format(String paragraph, int newIndentSize, String lineDelimiter, boolean indentFirstLine) {

		final int tabWidth= getTabWidth();
		final int firstLineIndentSize= getIndentSizeOfFirstLine(paragraph, indentFirstLine, tabWidth);
		final int minIndentSize= getMinimalIndentSize(paragraph, indentFirstLine, tabWidth);		

		if (newIndentSize < firstLineIndentSize - minIndentSize)
			newIndentSize= firstLineIndentSize - minIndentSize;

		final StringBuffer buffer= new StringBuffer();

		for (final Iterator iterator= new LineIterator(paragraph); iterator.hasNext();) {
			String line= (String) iterator.next();
			if (indentFirstLine) {

				String lineIndent= null;
				if (line.startsWith(COMMENT))
					lineIndent= COMMENT + getIndentOfLine(line.substring(2));
				else
					lineIndent= getIndentOfLine(line);
				String lineContent= line.substring(lineIndent.length());
				
				
				if (lineContent.length() == 0) {
					// line was empty; insert as is
					buffer.append(line);

				} else {
					int indentSize= calculateDisplayedWidth(lineIndent, tabWidth);
					int deltaSize= newIndentSize - firstLineIndentSize;
					lineIndent= changePrefix(lineIndent.trim(), indentSize + deltaSize, useSpaces(), tabWidth);
					buffer.append(lineIndent);
					buffer.append(lineContent);			
				}

			} else {
				indentFirstLine= true;
				buffer.append(line);
			}

			if (iterator.hasNext())
				buffer.append(lineDelimiter);			
		}

		return buffer.toString();
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
			
		clearCachedValues();
	}
	
	private static IPreferenceStore getPreferenceStore() {
		return JavaPlugin.getDefault().getPreferenceStore();
	}
	
	private boolean useSpaces() {
		return fUseSpaces;
	}
	
	private int getTabWidth() {
		if (fTabWidth == -1)
			// Fix for bug 29909 contributed by Nikolay Metchev
			fTabWidth= Integer.parseInt(((String)JavaCore.getOptions().get(JavaCore.FORMATTER_TAB_SIZE))); 
		return fTabWidth;
	}
	
	private void clearCachedValues() {
		fTabWidth= -1;
        fUseSpaces= getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SPACES_FOR_TABS);
	}
}
