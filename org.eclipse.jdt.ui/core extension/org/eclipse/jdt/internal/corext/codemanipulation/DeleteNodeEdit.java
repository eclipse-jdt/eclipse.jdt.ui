/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalSymbols;

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
	private int fDelimiterToken;

	public DeleteNodeEdit(ASTNode node, boolean deleteLine) {
		super(TextRange.createFromStartAndLength(node.getStartPosition(), node.getLength()), "");
		fDeleteLine= deleteLine;
		fDelimiterToken= getDelimiterToken(node);
	}

	private DeleteNodeEdit(TextRange range, boolean deleteLine) {
		super(range,"");
		fDeleteLine= deleteLine;
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
			int end= fDeleteLine ? getLineEnd(buffer, scanner, startOffset) : getEndIncludingDelimiter(buffer, scanner, startOffset);
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
	
	private int getLineEnd(TextBuffer buffer, Scanner scanner, int startOffset) throws InvalidInputException {
		int result= getTextRange().getExclusiveEnd();
		scanner.resetTo(result - startOffset, scanner.source.length - 1);
		int token;
		while((token= scanner.getNextToken()) != TerminalSymbols.TokenNameEOF) {
			if (token == Scanner.TokenNameCOMMENT_LINE || token == Scanner.TokenNameCOMMENT_BLOCK 
					|| token == Scanner.TokenNameCOMMENT_JAVADOC || token == fDelimiterToken) {
				continue;
			} 
			return result;
		}
		// we can remove the entire line
		return startOffset + scanner.source.length+ buffer.getLineDelimiter(buffer.getLineOfOffset(result - 1)).length();
	}
	
	private int getEndIncludingDelimiter(TextBuffer buffer, Scanner scanner, int startOffset) throws InvalidInputException {
		int result= getTextRange().getExclusiveEnd();
		if (fDelimiterToken == -1)
			return result;
		scanner.resetTo(result - startOffset, scanner.source.length);
		int token;
		boolean delimiterFound= false;
		loop: while((token= scanner.getNextToken()) != TerminalSymbols.TokenNameEOF) {
			if (token == Scanner.TokenNameCOMMENT_LINE || token == Scanner.TokenNameCOMMENT_BLOCK 
					|| token == Scanner.TokenNameCOMMENT_JAVADOC) {
				if (delimiterFound) {
					return startOffset + scanner.startPosition;
				}
				continue loop;
			} else if (token == fDelimiterToken) {
				delimiterFound= true;
				result= startOffset + scanner.currentPosition;
			} else {
				if (delimiterFound)
					result= startOffset + scanner.startPosition;
				return result;
			}
		}
		return result;
	}
	
	private static int getDelimiterToken(ASTNode node) {
		if (node instanceof VariableDeclarationFragment)
			return Scanner.TokenNameCOMMA;
		if (node instanceof SingleVariableDeclaration)
			return Scanner.TokenNameCOMMA;
		ASTNode parent= node.getParent();
		if (node instanceof Expression && parent instanceof ForStatement) {
			List updaters= ((ForStatement)parent).updaters();
			if (updaters.contains(node))
				return Scanner.TokenNameCOMMA;
		}
		return -1;
	}
}
