package org.eclipse.jdt.internal.corext.refactoring.code;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;

/**
 * A special text edit that deletes the selected source range and everything to the next token (not inluding semicolons)
 * or end of the line.
 */
class LineEndDeleteTextEdit extends SimpleTextEdit {
	
	private String fFullSource;
	
	LineEndDeleteTextEdit(int offset, int length, String source){
		super(offset, length, ""); //$NON-NLS-1$
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
			IScanner scanner= ToolFactory.createScanner(true, true, false, true);
			scanner.setSource(fFullSource.toCharArray());
			int start= getTextRange().getOffset() + length;
			scanner.resetTo(start, Integer.MAX_VALUE);
			int token= scanner.getNextToken();
			while (token != ITerminalSymbols.TokenNameEOF) {
				switch (token) {
					case ITerminalSymbols.TokenNameWHITESPACE:
						break;
					case ITerminalSymbols.TokenNameSEMICOLON:
						break;	
					case ITerminalSymbols.TokenNameCOMMENT_LINE :
						break;
					default:
						return scanner.getCurrentTokenEndPosition() + 1 - getTextRange().getOffset() - scanner.getCurrentTokenSource().length;
				}
				token= scanner.getNextToken();
			}
			return length;
		} catch (InvalidInputException e){
			return length;
		}
	}
}

