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

	private final String fName;
	private final String fQualifier;
	
	private IScanner fScanner;
	private ISourceRange fNoFlyZone; // don't scan in ImportContainer (sometimes edited by ImportStructure)
	private Set fMatches; //Set<Integer>, start positions

	
	public RefactoringScanner(String name, String qualifier) {
		Assert.isNotNull(name);
		Assert.isNotNull(qualifier);
		fName= name;
		fQualifier= qualifier;
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
		fScanner= null;
	}

	/** only for testing */
	public void scan(String text) {
		char[] chars= text.toCharArray();
		fMatches= new HashSet();
		fScanner= ToolFactory.createScanner(true, true, false, true);
		fScanner.setSource(chars);
		doScan();
		fScanner= null;
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
			char ch= value.charAt(from - 1);
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
		// only works for references without whitespace
		String value = new String(fScanner.getRawTokenSource());
		int start= fScanner.getCurrentTokenStartPosition();
		int index= value.indexOf(fName);
		while (index != -1) {
			if (isWholeWord(value, index, index + fName.length())
					&& ! hasWrongQualifier(value, index))
				addMatch(start + index);
			index= value.indexOf(fName, index + 1);
		}
	}

	private boolean hasWrongQualifier(String value, int nameStart) {
		// only works for references without whitespace
		int qualifierEnd= nameStart - 1;
		int qualifierStart= qualifierEnd - fQualifier.length();
		if (qualifierStart < 0)
			return false;
		
		char charBeforeName= value.charAt(qualifierEnd);
		if (! isQualifierSeparator(charBeforeName))
			return false;
		
		String srcQualifier= value.substring(qualifierStart, qualifierEnd);
		if (! srcQualifier.equals(fQualifier))
			return true;
		
		if (qualifierStart > 0) {
			// check case "p.A" -> "p.B" with reference "another.p.A":
			char charBeforeQualifier= value.charAt(qualifierStart - 1);
			if (isQualifierSeparator(charBeforeQualifier)) //$NON-NLS-1$
				return true;
		}
		return false;
	}

	private boolean isQualifierSeparator(char c) {
		return ".#".indexOf(c) != -1; //$NON-NLS-1$
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

