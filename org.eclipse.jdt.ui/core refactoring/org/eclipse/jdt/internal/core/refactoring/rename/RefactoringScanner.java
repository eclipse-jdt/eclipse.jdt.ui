/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.rename;

import java.util.HashSet;
import java.util.Set;

import java.util.StringTokenizer;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.compiler.parser.InvalidInputException;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalSymbols;
import org.eclipse.jdt.internal.compiler.util.CharOperation;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.DebugUtils;

public class RefactoringScanner {

	private boolean fAnalyzeJavaDoc;
	private boolean fAnalyzeComments;
	private boolean fAnalyzeStrings;
	
	private String fPattern;
	
	public RefactoringScanner(){
		this(true, true, true);
	}
	
	public RefactoringScanner(boolean analyzeJavaDoc, boolean analyzeComments, boolean analyzeStrings){
		fAnalyzeComments= analyzeComments;
		fAnalyzeJavaDoc= analyzeJavaDoc;
		fAnalyzeStrings= analyzeStrings;
	}
	
	public void scan(ICompilationUnit cu, Set javaDocResults, Set commentResults, Set stringResults)	throws JavaModelException {
		try{
			scan(cu.getBuffer().getCharacters(), javaDocResults, commentResults, stringResults);
		} catch (InvalidInputException e){
			//ignore
		}	
	}
	
	public void scan(String text, Set javaDocResults, Set commentResults, Set stringResults)	throws JavaModelException {
		try{
			scan(text.toCharArray(), javaDocResults, commentResults, stringResults);
		} catch (InvalidInputException e){
			//ignore
		}	
	}
	
	private void scan(char[] content, Set javaDocResults, Set commentResults, Set stringResults)	throws JavaModelException, InvalidInputException {
		Scanner scanner = new Scanner(true, true);
		scanner.recordLineSeparator = true;
		scanner.setSourceBuffer(content);
		int token = scanner.getNextToken();

		while (token != TerminalSymbols.TokenNameEOF) {
			switch (token) {
				case TerminalSymbols.TokenNameStringLiteral :
					if (!fAnalyzeStrings)
						break;
					parseCurrentToken(stringResults, scanner);	
					break;
				case Scanner.TokenNameCOMMENT_JAVADOC :
					if (!fAnalyzeJavaDoc)
						break;
					parseCurrentToken(javaDocResults, scanner);	
					break;
				case Scanner.TokenNameCOMMENT_LINE :
					if (!fAnalyzeComments)
						break;
					parseCurrentToken(commentResults, scanner);	
					break;
				case Scanner.TokenNameCOMMENT_BLOCK :
					if (!fAnalyzeComments)
						break;
					parseCurrentToken(commentResults, scanner);	
					break;
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
	
	private void parseCurrentToken(Set result, final Scanner scanner) throws  InvalidInputException {
		String value = new String(scanner.getCurrentTokenSource());
		int start = scanner.startPosition;
		int index = value.indexOf(fPattern);
		while (index != -1) {
			int offset= start + index;
			if (isWholeWord(value, index, index + fPattern.length()))			
				result.add(new Integer(start + index));
			index = value.indexOf(fPattern, index + 1);
		}
	}

	public boolean getAnalyzeJavaDoc() {
		return fAnalyzeJavaDoc;
	}

	public void setAnalyzeJavaDoc(boolean analyzeJavaDoc) {
		fAnalyzeJavaDoc = analyzeJavaDoc;
	}

	public boolean getAnalyzeComments() {
		return fAnalyzeComments;
	}

	public void setAnalyzeComments(boolean analyzeComments) {
		fAnalyzeComments= analyzeComments;
	}

	public boolean getAnalyzeStrings() {
		return fAnalyzeStrings;
	}

	public void setAnalyzeStrings(boolean analyzeStrings) {
		fAnalyzeStrings = analyzeStrings;
	}

	public String getPattern() {
		return fPattern;
	}

	public void setPattern(String pattern) {
		Assert.isNotNull(pattern);
		fPattern = pattern;
	}
}

