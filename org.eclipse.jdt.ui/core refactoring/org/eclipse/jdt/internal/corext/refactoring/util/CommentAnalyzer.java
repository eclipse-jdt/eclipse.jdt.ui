/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.util;

import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.compiler.parser.Scanner;

import org.eclipse.jdt.internal.corext.dom.CompilationUnitBuffer;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

public class CommentAnalyzer {
	
	private CommentAnalyzer() {
	}
	
	public static RefactoringStatus perform(Selection selection, CompilationUnitBuffer source, int start, int length) {
		RefactoringStatus result= new RefactoringStatus();
		if (length <= 0)
			return result;
		new CommentAnalyzer().check(result, selection, source, start, start + length - 1);
		return result;
	}
	
	private void check(RefactoringStatus result, Selection selection, CompilationUnitBuffer source, int start, int end) {
		Scanner scanner= new Scanner(true, false);
		scanner.setSource(source.getCharacters());
		scanner.resetTo(start, end);
		
		int token= 0;
		try {
			loop: while (token != ITerminalSymbols.TokenNameEOF) {
				token= scanner.getNextToken();
				switch(token) {
					case ITerminalSymbols.TokenNameCOMMENT_LINE:
					case ITerminalSymbols.TokenNameCOMMENT_BLOCK:
					case ITerminalSymbols.TokenNameCOMMENT_JAVADOC:
						if (checkStart(scanner, selection.getOffset())) {
							result.addFatalError(RefactoringCoreMessages.getString("CommentAnalyzer.starts_inside_comment")); //$NON-NLS-1$
							break loop;
						}
						if (checkEnd(scanner, selection.getInclusiveEnd())) {
							result.addFatalError(RefactoringCoreMessages.getString("CommentAnalyzer.ends_inside_comment")); //$NON-NLS-1$
							break loop;
						}
						break;
				}
			} 
		} catch (InvalidInputException e) {
			result.addFatalError(RefactoringCoreMessages.getString("CommentAnalyzer.internal_error")); //$NON-NLS-1$
		}
	}
	
	private boolean checkStart(Scanner scanner, int position) {
		return scanner.startPosition < position && position < scanner.currentPosition;
	}

	private boolean checkEnd(Scanner scanner, int position) {
		return scanner.startPosition <= position && position < scanner.currentPosition - 1;
	}
}
