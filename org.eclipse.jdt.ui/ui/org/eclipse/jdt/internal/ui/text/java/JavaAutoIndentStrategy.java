/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Nikolay Metchev - Fixed bug 29909
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;


import java.util.Iterator;
import java.util.NoSuchElementException;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultAutoIndentStrategy;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.texteditor.ITextEditorExtension3;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.WhileStatement;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.IJavaPartitions;
import org.eclipse.jdt.internal.ui.text.JavaHeuristicScanner;
import org.eclipse.jdt.internal.ui.text.JavaIndenter;
import org.eclipse.jdt.internal.ui.text.Symbols;

/**
 * Auto indent strategy sensitive to brackets.
 */
public class JavaAutoIndentStrategy extends DefaultAutoIndentStrategy {
		
		/**
		 * Internal line interator working on <code>IDocument</code>.
		 */
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
		
		private static class CompilationUnitInfo {
			
			public char[] buffer;
			public int delta;
			
			public CompilationUnitInfo(char[] buffer, int delta) {
				this.buffer= buffer;
				this.delta= delta;
			}
		}


	private final static String COMMENT= "//"; //$NON-NLS-1$
	
	private int fTabWidth;
	private boolean fUseSpaces;
	private boolean fCloseBrace;
	private boolean fIsSmartMode;

	private String fPartitioning;
	
	/**
	 * Creates a new Java auto indent strategy for the given document partitioning.
	 * 
	 * @param partitioning the document partitioning
	 */
	public JavaAutoIndentStrategy(String partitioning) {
		fPartitioning= partitioning;
 	}

	/**
	 * Evaluates the given line for the opening bracket that matches the closing bracket on the given line.
	 */
	private int findMatchingOpenBracket(IDocument d, int lineNumber, int endOffset, int closingBracketIncrease) throws BadLocationException {

		int startOffset= d.getLineOffset(lineNumber);
		int bracketCount= getBracketCount(d, startOffset, endOffset, false) - closingBracketIncrease;

		// sum up the brackets counts of each line (closing brackets count negative,
		// opening positive) until we find a line the brings the count to zero
		while (bracketCount < 0) {
			--lineNumber;
			if (lineNumber < 0)
				return -1;
			startOffset= d.getLineOffset(lineNumber);
			endOffset= startOffset + d.getLineLength(lineNumber) - 1;
			bracketCount += getBracketCount(d, startOffset, endOffset, false);
		}
		return lineNumber;
	}

	private int getBracketCount(IDocument d, int startOffset, int endOffset, boolean ignoreCloseBrackets) throws BadLocationException {

		int bracketCount= 0;
		while (startOffset < endOffset) {
			char curr= d.getChar(startOffset);
			startOffset++;
			switch (curr) {
				case '/' :
					if (startOffset < endOffset) {
						char next= d.getChar(startOffset);
						if (next == '*') {
							// a comment starts, advance to the comment end
							startOffset= getCommentEnd(d, startOffset + 1, endOffset);
						} else if (next == '/') {
							// '//'-comment: nothing to do anymore on this line
							startOffset= endOffset;
						}
					}
					break;
				case '*' :
					if (startOffset < endOffset) {
						char next= d.getChar(startOffset);
						if (next == '/') {
							// we have been in a comment: forget what we read before
							bracketCount= 0;
							startOffset++;
						}
					}
					break;
				case '{' :
					bracketCount++;
					ignoreCloseBrackets= false;
					break;
				case '}' :
					if (!ignoreCloseBrackets) {
						bracketCount--;
					}
					break;
				case '"' :
				case '\'' :
					startOffset= getStringEnd(d, startOffset, endOffset, curr);
					break;
				default :
					}
		}
		return bracketCount;
	}

	// ----------- bracket counting ------------------------------------------------------

	private int getCommentEnd(IDocument d, int offset, int endOffset) throws BadLocationException {
		while (offset < endOffset) {
			char curr= d.getChar(offset);
			offset++;
			if (curr == '*') {
				if (offset < endOffset && d.getChar(offset) == '/') {
					return offset + 1;
				}
			}
		}
		return endOffset;
	}

	private String getIndentOfLine(IDocument d, int line) throws BadLocationException {
		if (line > -1) {
			int start= d.getLineOffset(line);
			int end= start + d.getLineLength(line) - 1;
			int whiteEnd= findEndOfWhiteSpace(d, start, end);
			return d.get(start, whiteEnd - start);
		} else {
			return ""; //$NON-NLS-1$
		}
	}

	private int getStringEnd(IDocument d, int offset, int endOffset, char ch) throws BadLocationException {
		while (offset < endOffset) {
			char curr= d.getChar(offset);
			offset++;
			if (curr == '\\') {
				// ignore escaped characters
				offset++;
			} else if (curr == ch) {
				return offset;
			}
		}
		return endOffset;
	}

	private void smartIndentAfterClosingBracket(IDocument d, DocumentCommand c) {
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
	
	private void smartIndentAfterOpeningBracket(IDocument d, DocumentCommand c) {
		if (c.offset < 1 || d.getLength() == 0)
			return;
			
		JavaHeuristicScanner scanner= new JavaHeuristicScanner(d);

		int p= (c.offset == d.getLength() ? c.offset - 1 : c.offset);
		
		try {
			// current line
			int line= d.getLineOfOffset(p);
			int lineOffset= d.getLineOffset(line);
			
			// line of last javacode
			int pos= scanner.findNonWhitespaceBackward(p, JavaHeuristicScanner.UNBOUND);
			if (pos == -1)
				return;
			int lastLine= d.getLineOfOffset(pos);
			
			// only shift if the last java line is further up and is a braceless block candidate
			if (lastLine < line) {
			
				if (scanner.isBracelessBlockStart(pos + 1, JavaHeuristicScanner.UNBOUND)) {
					// if the last line was a braceless block candidate, we have indented 
					// after the new line. This has to be undone as we *are* starting a block
					// on the new line
					StringBuffer replace= new StringBuffer(getIndentOfLine(d, lastLine));
					c.length += replace.length();
					replace.append(c.text);
					c.offset= lineOffset;
					c.text= replace.toString();
				}
			}
			
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
		
	}

	private void smartIndentAfterNewLine(IDocument d, DocumentCommand c) {
		JavaHeuristicScanner scanner= new JavaHeuristicScanner(d);
		JavaIndenter indenter= new JavaIndenter(d, scanner);
		String indent= indenter.computeIndentation(c.offset);
		if (indent == null)
			indent= ""; //$NON-NLS-1$

		int docLength= d.getLength();
		if (c.offset == -1 || docLength == 0)
			return;

		try {
			int p= (c.offset == docLength ? c.offset - 1 : c.offset);
			int line= d.getLineOfOffset(p);

			StringBuffer buf= new StringBuffer(c.text + indent);


			IRegion reg= d.getLineInformation(line);
			int lineEnd= reg.getOffset() + reg.getLength();

			int contentStart= findEndOfWhiteSpace(d, c.offset, lineEnd);
			c.length=  Math.max(contentStart - c.offset, 0);
		
			int start= reg.getOffset();
			ITypedRegion region= TextUtilities.getPartition(d, fPartitioning, start);
			if (IJavaPartitions.JAVA_DOC.equals(region.getType()))
				start= d.getLineInformationOfOffset(region.getOffset()).getOffset();
				
			if (getBracketCount(d, start, c.offset, true) > 0 && closeBrace() && !isClosed(d, c.offset, c.length)) {
				c.caretOffset= c.offset + buf.length();
				c.shiftsCaret= false;
				
				// copy old content of line behind insertion point to new line
				// unless we think we are inserting an anonymous type definition
				if (c.offset == 0 || !(computeAnonymousPosition(d, c.offset - 1, fPartitioning, lineEnd) != -1)) {
					if (lineEnd - contentStart > 0) {
						c.length=  lineEnd - c.offset;
						buf.append(d.get(contentStart, lineEnd - contentStart).toCharArray());
					}
				}
			
				buf.append(getLineDelimiter(d));
				String reference= indenter.getReferenceIndentation(c.offset); 
				buf.append(reference == null ? "" : reference); //$NON-NLS-1$
				buf.append('}');
			}
			c.text= buf.toString();

		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
	}

	/**
	 * Computes an insert position for an opening brace if <code>offset</code> maps to a position in
	 * <code>document</code> with a expression in parenthesis that will take a block after the closing parenthesis.
	 * 
	 * @param document the document being modified
	 * @param offset the offset of the caret position, relative to the line start.
	 * @param partitioning the document partitioning
	 * @param max the max position
	 * @return an insert position relative to the line start if <code>line</code> contains a parenthesized expression that can be followed by a block, -1 otherwise
	 */
	private static int computeAnonymousPosition(IDocument document, int offset, String partitioning,  int max) {
		// find the opening parenthesis for every closing parenthesis on the current line after offset
		// return the position behind the closing parenthesis if it looks like a method declaration
		// or an expression for an if, while, for, catch statement
		
		JavaHeuristicScanner scanner= new JavaHeuristicScanner(document);
		int pos= offset;
		int length= max;
		int scanTo= scanner.scanForward(pos, length, '}');
		if (scanTo == -1)
			scanTo= length;
			
		int closingParen= findClosingParenToLeft(scanner, pos) - 1;

		while (true) {
			int startScan= closingParen + 1;
			closingParen= scanner.scanForward(startScan, scanTo, ')');
			if (closingParen == -1)
				break;

			int openingParen= scanner.findOpeningPeer(closingParen - 1, '(', ')');

			// no way an expression at the beginning of the document can mean anything
			if (openingParen < 1)
				break;

			// only select insert positions for parenthesis currently embracing the caret
			if (openingParen > pos)
				continue;

			if (looksLikeAnonymousClassDef(document, partitioning, scanner, openingParen - 1))
				return closingParen + 1;

		}

		return -1;
	}

	/**
	 * Finds a closing parenthesis to the left of <code>position</code> in document, where that parenthesis is only
	 * separated by whitespace from <code>position</code>. If no such parenthesis can be found, <code>position</code> is returned.
	 * 
	 * @param document the document being modified
	 * @param position the first character position in <code>document</code> to be considered
	 * @param partitioning the document partitioning
	 * @return the position of a closing parenthesis left to <code>position</code> separated only by whitespace, or <code>position</code> if no parenthesis can be found
	 */
	private static int findClosingParenToLeft(JavaHeuristicScanner scanner, int position) {
		if (position < 1)
			return position;

		if (scanner.previousToken(position - 1, JavaHeuristicScanner.UNBOUND) == Symbols.TokenRPAREN)
			return scanner.getPosition() + 1;
		return position;
	}

	/**
	 * Checks whether the content of <code>document</code> in the range (<code>offset</code>, <code>length</code>)
	 * contains the <code>new</code> keyword.
	 * 
	 * @param document the document being modified
	 * @param offset the first character position in <code>document</code> to be considered
	 * @param length the length of the character range to be considered
	 * @param partitioning the document partitioning
	 * @return <code>true</code> if the specified character range contains a <code>new</code> keyword, <code>false</code> otherwise.
	 */
	private static boolean isNewMatch(IDocument document, int offset, int length, String partitioning) {
		Assert.isTrue(length >= 0);
		Assert.isTrue(offset >= 0);
		Assert.isTrue(offset + length < document.getLength() + 1);

		try {
			String text= document.get(offset, length);
			int pos= text.indexOf("new"); //$NON-NLS-1$
			
			while (pos != -1 && !isDefaultPartition(document, pos + offset, partitioning))
				pos= text.indexOf("new", pos + 2); //$NON-NLS-1$

			if (pos < 0)
				return false;

			if (pos != 0 && Character.isJavaIdentifierPart(document.getChar(pos - 1)))
				return false;

			if (pos + 3 < length && Character.isJavaIdentifierPart(document.getChar(pos + 3)))
				return false;
			
			return true;

		} catch (BadLocationException e) {
		}
		return false;
	}

	/**
	 * Checks whether the content of <code>document</code> at <code>position</code> looks like an
	 * anonymous class definition. <code>position</code> must be to the left of the opening
	 * parenthesis of the definition's parameter list.
	 * 
	 * @param document the document being modified
	 * @param position the first character position in <code>document</code> to be considered
	 * @param partitioning the document partitioning
	 * @return <code>true</code> if the content of <code>document</code> looks like an anonymous class definition, <code>false</code> otherwise
	 */
	private static boolean looksLikeAnonymousClassDef(IDocument document, String partitioning, JavaHeuristicScanner scanner, int position) {
		int previousCommaOrParen= scanner.scanBackward(position - 1, JavaHeuristicScanner.UNBOUND, new char[] {',', '('});
		if (previousCommaOrParen == -1 || position < previousCommaOrParen + 5) // 2 for borders, 3 for "new"
			return false;

		if (isNewMatch(document, previousCommaOrParen + 1, position - previousCommaOrParen - 2, partitioning))
			return true;

		return false;
	}

	/**
	 * Checks whether <code>position</code> resides in a default (Java) partition of <code>document</code>.
	 * 
	 * @param document the document being modified
	 * @param position the position to be checked
	 * @param partitioning the document partitioning
	 * @return <code>true</code> if <code>position</code> is in the default partition of <code>document</code>, <code>false</code> otherwise
	 */
	private static boolean isDefaultPartition(IDocument document, int position, String partitioning) {
		Assert.isTrue(position >= 0);
		Assert.isTrue(position <= document.getLength());
		
		try {
			ITypedRegion region= TextUtilities.getPartition(document, partitioning, position);
			return region.getType().equals(IDocument.DEFAULT_CONTENT_TYPE);
			
		} catch (BadLocationException e) {
		}
		
		return false;
	}

	private boolean isClosed(IDocument document, int offset, int length) {
		
		CompilationUnitInfo info= getCompilationUnitForMethod(document, offset, fPartitioning);
		if (info == null)
			return false;
			
		CompilationUnit compilationUnit= null;
		try {
			compilationUnit= AST.parseCompilationUnit(info.buffer);
		} catch (ArrayIndexOutOfBoundsException x) {
			// work around for parser problem
			return false;
		}
		
		IProblem[] problems= compilationUnit.getProblems();
		for (int i= 0; i != problems.length; ++i) {
			if (problems[i].getID() == IProblem.UnmatchedBracket)
				return true;
		}
		
		final int relativeOffset= offset - info.delta;
		
		ASTNode node= NodeFinder.perform(compilationUnit, relativeOffset, length);
		if (node == null)
			return false;

		if (length == 0) {
			while (node != null && (relativeOffset == node.getStartPosition() || relativeOffset == node.getStartPosition() + node.getLength()))
				node= node.getParent();
		}
		
		switch (node.getNodeType()) {
		case ASTNode.BLOCK:
			return areBlocksConsistent(document, offset, fPartitioning);

		case ASTNode.IF_STATEMENT: 
			{
				IfStatement ifStatement= (IfStatement) node;
				Expression expression= ifStatement.getExpression();
				IRegion expressionRegion= createRegion(expression, info.delta);
				Statement thenStatement= ifStatement.getThenStatement();
				IRegion thenRegion= createRegion(thenStatement, info.delta);

				// between expression and then statement
				if (expressionRegion.getOffset() + expressionRegion.getLength() <= offset && offset + length <= thenRegion.getOffset())
					return thenStatement != null;
				

				Statement elseStatement= ifStatement.getElseStatement();
				IRegion elseRegion= createRegion(elseStatement, info.delta);
				
				IRegion elseToken= null;
				if (elseStatement != null) {
					int sourceOffset= thenRegion.getOffset() + thenRegion.getLength();
					int sourceLength= elseRegion.getOffset() - sourceOffset;
					elseToken= getToken(document, new Region(sourceOffset, sourceLength), ITerminalSymbols.TokenNameelse);
				}
				
				// between 'else' keyword and else statement				
				if (elseToken.getOffset() + elseToken.getLength() <= offset && offset + length < elseRegion.getOffset())
					return elseStatement != null;
			}
			break;

		case ASTNode.WHILE_STATEMENT:
		case ASTNode.FOR_STATEMENT:
			{
				Expression expression= node.getNodeType() == ASTNode.WHILE_STATEMENT ? ((WhileStatement) node).getExpression() : ((ForStatement) node).getExpression();
				IRegion expressionRegion= createRegion(expression, info.delta);
				Statement body= node.getNodeType() == ASTNode.WHILE_STATEMENT ? ((WhileStatement) node).getBody() : ((ForStatement) node).getBody();
				IRegion bodyRegion= createRegion(body, info.delta);
				
				// between expression and body statement
				if (expressionRegion.getOffset() + expressionRegion.getLength() <= offset && offset + length <= bodyRegion.getOffset())
					return body != null;
			}
			break;

		case ASTNode.DO_STATEMENT:
			{
				DoStatement doStatement= (DoStatement) node;
				IRegion doRegion= createRegion(doStatement, info.delta);
				Statement body= doStatement.getBody();
				IRegion bodyRegion= createRegion(body, info.delta);

				if (doRegion.getOffset() + doRegion.getLength() <= offset && offset + length <= bodyRegion.getOffset())
					return body != null;
			}
			break;
		}
		
		return true;
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

	private void smartPaste(IDocument document, DocumentCommand command) {

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
			int newIndentSize= insideBlockIndentSize < previousIndentSize ? insideBlockIndentSize : previousIndentSize;

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
			ITypedRegion typedRegion= TextUtilities.getPartition(document, fPartitioning, start);
			if (IJavaPartitions.JAVA_DOC.equals(typedRegion.getType())) {
				start= document.getLineInformationOfOffset(typedRegion.getOffset()).getOffset();
			} else if (IJavaPartitions.JAVA_SINGLE_LINE_COMMENT.equals(typedRegion.getType())) {
				buffer.append(COMMENT);
				start += 2;
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

		// assumption: string contains no whitespace
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

	private boolean isLineDelimiter(IDocument document, String text) {
		String[] delimiters= document.getLegalLineDelimiters();
		if (delimiters != null)
			return TextUtilities.equals(delimiters, text) > -1;
		return false;
	}

	private void smartIndentAfterBlockDelimiter(IDocument document, DocumentCommand command) {
		if (command.text.charAt(0) == '}')
			smartIndentAfterClosingBracket(document, command);
		else if (command.text.charAt(0) == '{')
			smartIndentAfterOpeningBracket(document, command);
	}

	/*
	 * @see org.eclipse.jface.text.IAutoIndentStrategy#customizeDocumentCommand(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.DocumentCommand)
	 */
	public void customizeDocumentCommand(IDocument d, DocumentCommand c) {
		
		clearCachedValues();
		if (!isSmartMode() || c.doit == false)
			return;
		
		if (c.length == 0 && c.text != null && isLineDelimiter(d, c.text))
			smartIndentAfterNewLine(d, c);
		else if (c.text.length() == 1)
			smartIndentAfterBlockDelimiter(d, c);
		else if (c.text.length() > 1 && getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SMART_PASTE))
			smartPaste(d, c);
		
	}
	
	private static IPreferenceStore getPreferenceStore() {
		return JavaPlugin.getDefault().getPreferenceStore();
	}
	
	private boolean useSpaces() {
		return fUseSpaces;
	}
	
	private boolean closeBrace() {
		return fCloseBrace;
	}

	private int getTabWidth() {
		return fTabWidth;
	}
	
	private boolean isSmartMode() {
		return fIsSmartMode;
	}
	
	private void clearCachedValues() {
		// Fix for bug 29909 contributed by Nikolay Metchev
		fTabWidth= CodeFormatterUtil.getTabWidth();
        
        IPreferenceStore preferenceStore= getPreferenceStore();
		fUseSpaces= preferenceStore.getBoolean(PreferenceConstants.EDITOR_SPACES_FOR_TABS);
		fCloseBrace= preferenceStore.getBoolean(PreferenceConstants.EDITOR_CLOSE_BRACES);
		fIsSmartMode= computeSmartMode();
	}
	
	private boolean computeSmartMode() {
		IWorkbenchPage page= JavaPlugin.getActivePage();
		if (page != null)  {
			IEditorPart part= page.getActiveEditor(); 
			if (part instanceof ITextEditorExtension3) {
				ITextEditorExtension3 extension= (ITextEditorExtension3) part;
				return extension.getInsertMode() == ITextEditorExtension3.SMART_INSERT;
			}
		}
		return false;
	}

	private static CompilationUnitInfo getCompilationUnitForMethod(IDocument document, int offset, String partitioning) {
		try {
			JavaHeuristicScanner scanner= new JavaHeuristicScanner(document);
			
			IRegion sourceRange= scanner.findSurroundingBlock(offset);
			if (sourceRange == null)
				return null;
			String source= document.get(sourceRange.getOffset(), sourceRange.getLength());
	
			StringBuffer contents= new StringBuffer();
			contents.append("class ____C{void ____m()"); //$NON-NLS-1$
			final int methodOffset= contents.length();
			contents.append(source);
			contents.append('}');
			
			char[] buffer= contents.toString().toCharArray();
	
			return new CompilationUnitInfo(buffer, sourceRange.getOffset() - methodOffset);
	
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
	
		return null;
	}
	
	private static boolean areBlocksConsistent(IDocument document, int offset, String partitioning) {
		if (offset < 1 || offset >= document.getLength())
			return false;
		
		int begin= offset;
		int end= offset - 1;
		
		JavaHeuristicScanner scanner= new JavaHeuristicScanner(document);
		
		while (true) {
			begin= scanner.findOpeningPeer(begin - 1, '{', '}');
			end= scanner.findClosingPeer(end + 1, '{', '}');
			if (begin == -1 && end == -1)
				return true;
			if (begin == -1 || end == -1)
				return false;
		}		
	}
	
	private static IRegion createRegion(ASTNode node, int delta) {
		return node == null ? null : new Region(node.getStartPosition() + delta, node.getLength());
	}
	
	private static IRegion getToken(IDocument document, IRegion scanRegion, int tokenId)  {

		try {
			
			final String source= document.get(scanRegion.getOffset(), scanRegion.getLength());
	
			IScanner scanner= ToolFactory.createScanner(false, false, false, false);
			scanner.setSource(source.toCharArray());

			int id= scanner.getNextToken();
			while (id != ITerminalSymbols.TokenNameEOF && id != tokenId)
				id= scanner.getNextToken();

			if (id == ITerminalSymbols.TokenNameEOF)
				return null;

			int tokenOffset= scanner.getCurrentTokenStartPosition();
			int tokenLength= scanner.getCurrentTokenEndPosition() + 1 - tokenOffset; // inclusive end
			return new Region(tokenOffset + scanRegion.getOffset(), tokenLength);

		} catch (InvalidInputException x) {
			return null;
		} catch (BadLocationException x) {
			return null;
		}
	}
}
