package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

public class BreakpointLocationVerifier {

	/**
	 * Returns the line number closest to the given line number that represents a
	 * valid location for a breakpoint in the given document, or -1 if a valid location
	 * cannot be found.
	 */
	public int getValidBreakpointLocation(IDocument doc, int lineNumber) {

		IScanner scanner = ToolFactory.createScanner(false, false, false);
		boolean found = false;
		int start = 0, length = 0, token = 0, lastToken = 0;

		while (!found) {
			try {
				start = doc.getLineOffset(lineNumber);
				length = doc.getLineLength(lineNumber);
				char[] txt = doc.get(start, length).toCharArray();
				scanner.setSource(txt);
				token = scanner.getNextToken();
				if (token == ITerminalSymbols.TokenNameEQUAL) {
					break;
				}
				lastToken = 0;

				while (token != ITerminalSymbols.TokenNameEOF) {
					if (token == ITerminalSymbols.TokenNameERROR) {
						lineNumber++;
						break;
					}
					if (token == ITerminalSymbols.TokenNameIdentifier) {
						if (lastToken == ITerminalSymbols.TokenNameIdentifier
							|| isPrimitiveTypeToken(lastToken)
							|| lastToken == ITerminalSymbols.TokenNameRBRACKET) {
							//var declaration..is there initialization 
							//OR method parameters on a line all by themselves
							lastToken = token;
							token = scanner.getNextToken();
							if (token == ITerminalSymbols.TokenNameEQUAL) {
								found = true;
								break;
							}
						}
						if (lastToken == ITerminalSymbols.TokenNameMULTIPLY) {
							//internal comment line starting with '*'
							break;
						}
					} else if (isNonIdentifierValidToken(token)) {
						found = true;
						break;
					} else if (
						lastToken == ITerminalSymbols.TokenNameIdentifier
							&& token != ITerminalSymbols.TokenNameLBRACKET) {
						found = true;
						break;
					} else if (
						lastToken == ITerminalSymbols.TokenNameLBRACKET
							&& token == ITerminalSymbols.TokenNameRBRACKET) {
						//var declaration..is there initialization
						lastToken = token;
						token = scanner.getNextToken();
						if (token == ITerminalSymbols.TokenNameSEMICOLON) {
							//no init
							break;
						} else if (token == ITerminalSymbols.TokenNameEQUAL) {
							found = true;
							break;
						}

						continue;
					}

					lastToken = token;
					token = scanner.getNextToken();
				}
				if (!found) {
					lineNumber++;
				}
			} catch (BadLocationException ble) {
				return -1;
			} catch (InvalidInputException ie) {
				//start of a comment "/**" or "/*"
				lineNumber++;
			}
		}
		// add 1 to the line number - Document is 0 based, JDI is 1 based
		return lineNumber + 1;
	}

	protected boolean isPrimitiveTypeToken(int token) {
		switch (token) {
			case ITerminalSymbols.TokenNameboolean :
			case ITerminalSymbols.TokenNamebyte :
			case ITerminalSymbols.TokenNamechar :
			case ITerminalSymbols.TokenNamedouble :
			case ITerminalSymbols.TokenNamefloat :
			case ITerminalSymbols.TokenNameint :
			case ITerminalSymbols.TokenNamelong :
			case ITerminalSymbols.TokenNameshort :
				return true;
			default :
				return false;
		}
	}

	protected boolean isNonIdentifierValidToken(int token) {
		switch (token) {
			case ITerminalSymbols.TokenNamebreak :
			case ITerminalSymbols.TokenNamecontinue :
			case ITerminalSymbols.TokenNamereturn :
			case ITerminalSymbols.TokenNamethis :
			case ITerminalSymbols.TokenNamesuper :
				return true;
			default :
				return false;
		}
	}
}