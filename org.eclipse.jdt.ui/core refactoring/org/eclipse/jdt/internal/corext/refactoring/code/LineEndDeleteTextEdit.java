package org.eclipse.jdt.internal.corext.refactoring.code;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.parser.InvalidInputException;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalSymbols;
import org.eclipse.jdt.internal.corext.codemanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.codemanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.TextPosition;
import org.eclipse.jdt.internal.corext.refactoring.Assert;

/**
 * A special text edit that deletes the selected source range and everything to the next token (not inluding semicolons)
 * or end of the line.
 */
class LineEndDeleteTextEdit extends SimpleTextEdit {
	
	private String fFullSource;
	
	LineEndDeleteTextEdit(int offset, int length, String source){
		super(offset, length, "");
		Assert.isTrue(offset >= 0);
		Assert.isTrue(length >= 0);
		Assert.isTrue(offset + length <= source.length());
		fFullSource= source;
	}
	
	/*
	 * @see TextEdit#copy()
	 */
	public TextEdit copy() {
		return new LineEndDeleteTextEdit(getTextPosition().getOffset(), getTextPosition().getLength(), fFullSource);
	}
	
	/*
	 * @see TextEdit#connect(TextBuffer)
	 */
	public void connect(TextBuffer textBuffer) throws JavaModelException{
		setTextPosition(new TextPosition(getTextPosition().getOffset(), computeLength()));
	}
	
	private int computeLength() throws JavaModelException{
		int length= getTextPosition().getLength();
		try{	
			Scanner scanner= new Scanner(true, true); //comments, whitespaces
			scanner.recordLineSeparator = true;
			scanner.setSourceBuffer(fFullSource.toCharArray());
			int start= getTextPosition().getOffset() + length;
			scanner.currentPosition= start;
			int token = scanner.getNextToken();
			while (token != TerminalSymbols.TokenNameEOF) {
				switch (token) {
					case Scanner.TokenNameWHITESPACE:
						break;
					case TerminalSymbols.TokenNameSEMICOLON:
						break;	
					case Scanner.TokenNameCOMMENT_LINE :
						break;
					default:
						return scanner.currentPosition - getTextPosition().getOffset() - scanner.getCurrentTokenSource().length;
				}
				token = scanner.getNextToken();
			}
			return length;
		} catch (InvalidInputException e){
			return length;
		}
	}
}

