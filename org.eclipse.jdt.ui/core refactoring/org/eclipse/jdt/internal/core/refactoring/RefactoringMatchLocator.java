/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
package org.eclipse.jdt.internal.core.refactoring;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.compiler.parser.InvalidInputException;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalSymbols;
import org.eclipse.jdt.internal.compiler.util.CharOperation;
import org.eclipse.jdt.internal.core.search.matching.MatchLocator;
import org.eclipse.jdt.internal.core.search.matching.SearchPattern;
import org.eclipse.jdt.internal.core.util.HackFinder;

class RefactoringMatchLocator extends MatchLocator{
	
	public RefactoringMatchLocator(SearchPattern pattern, int detailLevel, IJavaSearchResultCollector collector, IJavaSearchScope scope) {
		super(pattern, detailLevel, collector, scope);
	}
	
	public void report(int sourceStart, int sourceEnd, IJavaElement element, int accuracy) throws CoreException {
		report(sourceStart, sourceEnd, element, accuracy, false);
	}
	
	private void report(int sourceStart, int sourceEnd, IJavaElement element, int accuracy, boolean qualified) throws CoreException {
		if (this.scope.encloses(element)) {
			((SearchResultCollector)this.collector).accept(getCurrentResource(), sourceStart, sourceEnd + 1, element, accuracy, qualified);
		}
	}	
	
	/**
	 * Reports the given qualified reference to the search requestor.
	 */
	public void reportQualifiedReference(int sourceStart, int sourceEnd, char[][] qualifiedName, IJavaElement element, int accuracy) throws CoreException {
		
		HackFinder.fixMeSoon("code copied from org.eclipse.jdt.internal.core.search.matching.MatchLocator;");
		
			// compute source positions of the qualified reference 
		Scanner scanner= getScanner();
		scanner.resetTo(sourceStart, sourceEnd);

		int refSourceStart= -1, refSourceEnd= -1;
		int tokenNumber= qualifiedName.length;
		int token= -1;
		int previousValid= -1;
		int i= 0;
		do {
			int currentPosition= scanner.currentPosition;
			// read token
			try {
				token= scanner.getNextToken();
			} catch (InvalidInputException e) {
			}
			if (token != TerminalSymbols.TokenNameEOF) {
				char[] currentTokenSource= scanner.getCurrentTokenSource();
				while (i < tokenNumber && !CharOperation.equals(currentTokenSource, qualifiedName[i++])) {}
				if (CharOperation.equals(currentTokenSource, qualifiedName[i - 1]) && (previousValid == -1 || previousValid == i - 2)) {
					previousValid= i - 1;
					if (refSourceStart == -1) {
						refSourceStart= currentPosition;
					}
					refSourceEnd= scanner.currentPosition - 1;
				} else {
					i= 0;
					refSourceStart= -1;
					previousValid= -1;
				}
				// read '.'
				try {
					token= scanner.getNextToken();
				} catch (InvalidInputException e) {
				}
			}
		}
		while (token != TerminalSymbols.TokenNameEOF && i < tokenNumber);

		// accept method declaration
		if (refSourceStart != -1) {
			this.report(refSourceStart, refSourceEnd, element, accuracy, true);
		} else {
			this.report(sourceStart, sourceEnd, element, accuracy, true);
		}
	}
		
}


