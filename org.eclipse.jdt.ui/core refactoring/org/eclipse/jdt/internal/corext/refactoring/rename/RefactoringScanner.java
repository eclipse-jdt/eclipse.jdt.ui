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
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;

import org.eclipse.jdt.internal.corext.Assert;

public class RefactoringScanner {

	private String fPattern;
	
	private IScanner fScanner;
	private ISourceRange fNoFlyZone; // don't scan in ImportContainer (sometimes edited by ImportStructure)
	private Set fMatches; //Set<Integer>, start positions
	
	public RefactoringScanner(String pattern) {
		Assert.isNotNull(pattern);
		fPattern= pattern;
	}
	
	public void scan(ICompilationUnit cu)	throws JavaModelException {
		char[] chars= cu.getBuffer().getCharacters();
		fMatches= new HashSet();
		fScanner= ToolFactory.createScanner(true, true, false, true);
		fScanner.setSource(chars);

		IImportContainer importContainer= cu.getImportContainer();
		if (importContainer.exists())
			fNoFlyZone= importContainer.getSourceRange();
		else
			fNoFlyZone= null;
		
		doScan();
	}

	/** only for testing */
	public void scan(String text) {
		char[] chars= text.toCharArray();
		fMatches= new HashSet();
		fScanner= ToolFactory.createScanner(true, true, false, true);
		fScanner.setSource(chars);
		doScan();
	}

	private void doScan() {
		try{
			int token = fScanner.getNextToken();
			while (token != ITerminalSymbols.TokenNameEOF) {
				switch (token) {
					case ITerminalSymbols.TokenNameStringLiteral :
					case ITerminalSymbols.TokenNameCOMMENT_JAVADOC :
					case ITerminalSymbols.TokenNameCOMMENT_LINE :
					case ITerminalSymbols.TokenNameCOMMENT_BLOCK :
						parseCurrentToken();
				}
				token = fScanner.getNextToken();
			}
		} catch (InvalidInputException e){
			//ignore
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
	
	private void parseCurrentToken() {
		String value = new String(fScanner.getRawTokenSource());
		int start= fScanner.getCurrentTokenStartPosition();
		int index= value.indexOf(fPattern);
		while (index != -1) {
			if (isWholeWord(value, index, index + fPattern.length()))			
				addMatch(start + index);
			index= value.indexOf(fPattern, index + 1);
		}
	}

	private void addMatch(int matchStart) {
		if (fNoFlyZone != null 
				&& fNoFlyZone.getOffset() <= matchStart
				&& matchStart < fNoFlyZone.getOffset() + fNoFlyZone.getLength())
			return;
		fMatches.add(new Integer(matchStart));		
	}

	/**
	 * @return Set of Integer (start positions of matches)
	 */
	public Set getMatches() {
		return fMatches;
	}
}

