package org.eclipse.jdt.internal.debug.ui;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */

import org.eclipse.jdt.internal.compiler.parser.InvalidInputException;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalSymbols;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

public class BreakpointLocationVerifier {

	/**
	 * Returns the line number closest to the given line number that represents a
	 * valid location for a breakpoint in the given document, or -1 is a valid location
	 * cannot be found.
	 */
	public int getValidBreakpointLocation(IDocument doc, int lineNumber) {

		Scanner scanner= new Scanner();
		boolean found= false;
		int start= 0, length= 0, token= 0;

		while (!found) {
			try {
				start= doc.getLineOffset(lineNumber);
				length= doc.getLineLength(lineNumber);
				char[] txt= doc.get(start, length).toCharArray();
				scanner.setSourceBuffer(txt);
				token= scanner.getNextToken();

				while (token != TerminalSymbols.TokenNameEOF) {
					if (token == TerminalSymbols.TokenNamebreak ||
						token == TerminalSymbols.TokenNamecontinue ||
						token == TerminalSymbols.TokenNameIdentifier ||
						token == TerminalSymbols.TokenNamereturn ||
						token == TerminalSymbols.TokenNamethis) {
						found= true;
						break;
					} else {
						token= scanner.getNextToken();
					}
				}
				if (!found) {
					lineNumber++;
				}
			} catch (BadLocationException ble) {
				return -1;
			} catch (InvalidInputException ie) {
				return -1;
			}
		}
		// add 1 to the line number - Document is 0 based, JDI is 1 based
		return lineNumber + 1;
	}

}
