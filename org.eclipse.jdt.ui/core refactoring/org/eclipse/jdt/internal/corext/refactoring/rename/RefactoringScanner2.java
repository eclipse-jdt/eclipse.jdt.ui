/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.corext.Assert;

//TODO: rename to RefactoringScanner
public class RefactoringScanner2 {

	private String fPattern;
	
	private Set fMatches; //Set<Integer>, start positions
	
	public RefactoringScanner2() {
		//empty
	}
	
	public void scan(ICompilationUnit cu)	throws JavaModelException {
		try{
			scan(cu.getBuffer().getCharacters());		
		} catch (InvalidInputException e){
			//ignore
		}	
	}
	
	public void scan(String text) {
		try{
			scan(text.toCharArray());
		} catch (InvalidInputException e){
			//ignore
		}	
	}

	private void scan(char[] content) throws InvalidInputException {
		fMatches= new HashSet();
		
		IScanner scanner= ToolFactory.createScanner(true, true, false, true);
		scanner.setSource(content);
		int token = scanner.getNextToken();

		while (token != ITerminalSymbols.TokenNameEOF) {
			switch (token) {
				case ITerminalSymbols.TokenNameStringLiteral :
				case ITerminalSymbols.TokenNameCOMMENT_JAVADOC :
				case ITerminalSymbols.TokenNameCOMMENT_LINE :
				case ITerminalSymbols.TokenNameCOMMENT_BLOCK :
					parseCurrentToken(fMatches, scanner);
			}
			token = scanner.getNextToken();
		}
	}

	private static boolean isWholeWord(String value, int from, int to){
		if (from > 0) {
			char ch= value.charAt(from-1);
			if (Character.isLetterOrDigit(ch) || ch == '_') {
				return false;
			}
		}
		if (to < value.length()) {
			char ch= value.charAt(to);
			if (Character.isLetterOrDigit(ch) || ch == '_' ) {
				return false;
			}
		}
		return true;
	}
	
	private void parseCurrentToken(Set result, final IScanner scanner) {
		String value = new String(scanner.getRawTokenSource());
		int start= scanner.getCurrentTokenStartPosition();
		int index= value.indexOf(fPattern);
		while (index != -1) {
			if (isWholeWord(value, index, index + fPattern.length()))			
				result.add(new Integer(start + index));
			index= value.indexOf(fPattern, index + 1);
		}
	}

	public String getPattern() {
		return fPattern;
	}

	public void setPattern(String pattern) {
		Assert.isNotNull(pattern);
		fPattern = pattern;
	}
	
	/**
	 * @return Set of Integer (start positions of matches)
	 */
	public Set getMatches() {
		return fMatches;
	}
}

