/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.util;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.CompilationUnitBuffer;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

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
		IScanner scanner= ToolFactory.createScanner(true, false, false, false);
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
	
	private boolean checkStart(IScanner scanner, int position) {
		return scanner.getCurrentTokenStartPosition() < position && position <= scanner.getCurrentTokenEndPosition();
	}

	private boolean checkEnd(IScanner scanner, int position) {
		return scanner.getCurrentTokenStartPosition() <= position && position < scanner.getCurrentTokenEndPosition();
	}
	
	/**
	 * Removes comments and whitespace
	 * @param reference the type reference
	 * @return the reference only consisting of dots and java identifier characters
	 */
	public static String normalizeReference(String reference) {
		IScanner scanner= ToolFactory.createScanner(false, false, false, false);
		scanner.setSource(reference.toCharArray());
		StringBuffer sb= new StringBuffer();
		try {
			int tokenType= scanner.getNextToken();
			while (tokenType != ITerminalSymbols.TokenNameEOF) {
				sb.append(scanner.getRawTokenSource());
				tokenType= scanner.getNextToken();
			}
		} catch (InvalidInputException e) {
			Assert.isTrue(false, reference);
		}
		reference= sb.toString();
		return reference;
	}
}
