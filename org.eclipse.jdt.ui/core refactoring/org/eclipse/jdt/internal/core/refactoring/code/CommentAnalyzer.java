/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.code;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;

import org.eclipse.jdt.internal.compiler.parser.InvalidInputException;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalSymbols;

public class CommentAnalyzer {
	
	public RefactoringStatus check(Selection selection, char[] source, int start, int end) {
		RefactoringStatus result= new RefactoringStatus();
		
		Scanner scanner= new Scanner(true, false);
		scanner.setSourceBuffer(source);
		scanner.resetTo(start, end);
		
		int token= 0;
		try {
			loop: while (token != TerminalSymbols.TokenNameEOF) {
				token= scanner.getNextToken();
				switch(token) {
					case Scanner.TokenNameCOMMENT_LINE:
					case Scanner.TokenNameCOMMENT_BLOCK:
					case Scanner.TokenNameCOMMENT_JAVADOC:
						if (enclosesPosition(scanner, selection.start)) {
							result.addFatalError("Selection starts inside a comment.");
							break loop;
						}
						if (enclosesPosition(scanner, selection.end)) {
							result.addFatalError("Selection ends inside a comment.");
							break loop;
						}
						break;
				}
			} 
		} catch (InvalidInputException e) {
			result.addFatalError("Internal error during precondition checking.");
		}
		return result;
	}
	
	private boolean enclosesPosition(Scanner scanner, int position) {
		return scanner.startPosition < position && position < scanner.currentPosition - 1;
	}
}
