/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.nls;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalSymbols;

public class NLSScanner {

	//no instances	
	private NLSScanner(){
	}

	/**
	 * Returns a list of NLSLines found in the compilation unit
	 */
	public static NLSLine[] scan(ICompilationUnit cu) throws JavaModelException, InvalidInputException {
		return scan(cu.getBuffer().getCharacters());
	}

	/**
	 * Returns a list of NLSLines found in the string
	 */	
	public static NLSLine[] scan(String s) throws JavaModelException, InvalidInputException {
		return scan(s.toCharArray()); 
	}
	
	private static NLSLine[] scan(char[] content) throws JavaModelException, InvalidInputException {
		List lines= new ArrayList();
		Scanner scanner= new Scanner(true, true);
		scanner.recordLineSeparator= true;
		scanner.setSourceBuffer(content);
		int token= scanner.getNextToken();
		int currentLineNr= -1;
		int previousLineNr= -1;
		NLSLine currentLine= null;
		
		while (token != TerminalSymbols.TokenNameEOF) {
			switch (token) {
				case TerminalSymbols.TokenNameStringLiteral:
					currentLineNr= scanner.linePtr;
					if (currentLineNr != previousLineNr) {
						currentLine= new NLSLine(currentLineNr);
						lines.add(currentLine);
						previousLineNr= currentLineNr;
					}
					String value= new String(scanner.getCurrentTokenSource());
					currentLine.add(new NLSElement(value, scanner.startPosition, scanner.currentPosition-scanner.startPosition));
					break;
				case Scanner.TokenNameCOMMENT_LINE:
					// When we get the line comment this pointer is already moved to the next line
					if (currentLineNr != scanner.linePtr - 1)
						break;
						
					parseTags(currentLine, scanner);
					break;
			}
			token= scanner.getNextToken();
		}
		return (NLSLine[]) lines.toArray(new NLSLine[lines.size()]);
	}
	
	private static void parseTags(NLSLine line, Scanner scanner) throws InvalidInputException {
		String s= new String(scanner.getCurrentTokenSource());
		int pos= s.indexOf(NLSElement.TAG_PREFIX);
		while (pos != -1) {
			int start= pos + NLSElement.TAG_PREFIX_LENGTH; 
			int end= s.indexOf(NLSElement.TAG_POSTFIX, start);
			String index= s.substring(start, end);
			int i= 0;
			try {
				i= Integer.parseInt(index) - 1; 	// Tags are one based not zero based.
			} catch (NumberFormatException e) {
				new InvalidInputException(NLSMessages.getString("nlsscanner.invalid_tag") + s.substring(pos, end + 1)); //$NON-NLS-1$
			}
			if (line.exists(i)) {
				NLSElement element= line.get(i);
				element.setTagPosition(scanner.startPosition + pos, end - pos + 1);
			} else {
				new InvalidInputException(NLSMessages.getString("nlsscanner.no_string_for_tag") + s.substring(pos, end + 1));				 //$NON-NLS-1$
			}
			pos= s.indexOf(NLSElement.TAG_PREFIX, start);
		}
	}	
}

