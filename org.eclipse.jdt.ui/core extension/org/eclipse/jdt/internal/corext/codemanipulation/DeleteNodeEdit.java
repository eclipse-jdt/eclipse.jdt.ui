/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.internal.compiler.ast.AstNode;
import org.eclipse.jdt.internal.compiler.parser.InvalidInputException;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalSymbols;
import org.eclipse.jdt.internal.corext.refactoring.util.ASTUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;

/**
 * Deletes the text range specified by an AST nodes from a source file.
 */
public final class DeleteNodeEdit extends SimpleTextEdit {

	private boolean fDeleteLine;
	private int fLineDelimiterLength;

	public DeleteNodeEdit(AstNode node, boolean deleteLine) {
		super(TextRange.createFromStartAndInclusiveEnd(ASTUtil.getSourceStart(node), ASTUtil.getSourceEnd(node)), "");
		fDeleteLine= deleteLine;
	}

	private DeleteNodeEdit(TextRange range, boolean deleteLine) {
		super(range,"");
		fDeleteLine= deleteLine;
	}

	public boolean deletesLineEnd() {
		return fLineDelimiterLength != 0;
	}
	
	public int getLineDelimiterLength() {
		return fLineDelimiterLength;
	}
	
	/* non Java-doc
	 * @see TextEdit#connect(TextBufferEditor)
	 */
	public void connect(TextBufferEditor editor) throws CoreException {
		TextBuffer buffer= editor.getTextBuffer();
		TextRange range= getTextRange();
		TextRegion region;
		int startOffset= buffer.getLineInformationOfOffset(range.getOffset()).getOffset();
		region= buffer.getLineInformationOfOffset(range.getInclusiveEnd());
		int endOffset= region.getOffset() + region.getLength() - 1;		// inclusive.
		Scanner scanner= new Scanner(true, false);							// comments, but no white spaces
		scanner.setSourceBuffer(buffer.getContent(startOffset, endOffset - startOffset + 1).toCharArray());
		try {
			int start= fDeleteLine ? getStart(buffer, scanner, startOffset) : range.getOffset();
			int end= getEnd(buffer, scanner, startOffset);
			setTextRange(new TextRange(start, end - start));
		} catch (InvalidInputException e) {
			// Only delete node range
		}
	}

	/* non Java-doc
	 * @see TextEdit#copy()
	 */
	public TextEdit copy() throws CoreException {
		return new DeleteNodeEdit(getTextRange().copy(), this.fDeleteLine);
	}
	
	private int getStart(TextBuffer buffer, Scanner scanner, int startOffset) throws InvalidInputException {
		int relativeNodeStart= getTextRange().getOffset() - startOffset;
		scanner.resetTo(startOffset, relativeNodeStart - 1);
		int token;
		loop: while((token= scanner.getNextToken()) != TerminalSymbols.TokenNameEOF) {
			switch (token) {
				case Scanner.TokenNameCOMMENT_LINE:
				case Scanner.TokenNameCOMMENT_BLOCK:
				case Scanner.TokenNameCOMMENT_JAVADOC:
					continue loop;
				default:
					return getTextRange().getOffset();
			}
		}
		return startOffset;
	}
	
	private int getEnd(TextBuffer buffer, Scanner scanner, int startOffset) throws InvalidInputException {
		int begin= getTextRange().getExclusiveEnd() - startOffset;
		scanner.resetTo(begin, scanner.source.length - 1);
		int result= getTextRange().getExclusiveEnd();
		int token;
		loop: while((token= scanner.getNextToken()) != TerminalSymbols.TokenNameEOF) {
			switch (token) {
				case Scanner.TokenNameCOMMENT_LINE:
				case Scanner.TokenNameCOMMENT_BLOCK:
				case Scanner.TokenNameCOMMENT_JAVADOC:
					continue loop;
				case TerminalSymbols.TokenNameSEMICOLON:
					result= startOffset + scanner.currentPosition;
					if (!fDeleteLine)
						return result;
					break;
				default:
					return result;
			}
		}
		result= startOffset + scanner.source.length;
		fLineDelimiterLength= buffer.getLineDelimiter(buffer.getLineOfOffset(result - 1)).length();
		return result + fLineDelimiterLength;
	}
}
