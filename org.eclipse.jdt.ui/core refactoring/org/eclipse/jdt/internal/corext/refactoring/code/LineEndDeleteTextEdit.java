package org.eclipse.jdt.internal.corext.refactoring.code;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.parser.InvalidInputException;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalSymbols;
import org.eclipse.jdt.internal.corext.codemanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.codemanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.TextRange;
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
		return new LineEndDeleteTextEdit(getTextRange().getOffset(), getTextRange().getLength(), fFullSource);
	}
	
	/*
	 * @see TextEdit#connect(TextBuffer)
	 */
	public void connect(TextBufferEditor editor) throws JavaModelException{
		setTextRange(new TextRange(getTextRange().getOffset(), computeLength()));
	}
	
	private int computeLength() throws JavaModelException{
		int length= getTextRange().getLength();
		try{	
			Scanner scanner= new Scanner(true, true); //comments, whitespaces
			scanner.recordLineSeparator = true;
			scanner.setSourceBuffer(fFullSource.toCharArray());
			int start= getTextRange().getOffset() + length;
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
						return scanner.currentPosition - getTextRange().getOffset() - scanner.getCurrentTokenSource().length;
				}
				token = scanner.getNextToken();
			}
			return length;
		} catch (InvalidInputException e){
			return length;
		}
	}
}

